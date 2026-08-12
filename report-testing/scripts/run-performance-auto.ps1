param(
    [string]$Adb = "D:\Android\SDK\platform-tools\adb.exe"
)

$ErrorActionPreference = "Stop"

function Get-Median([double[]]$Values) {
    $sorted = @($Values | Sort-Object)
    $n = $sorted.Count
    if ($n -eq 0) { return 0 }
    if ($n % 2 -eq 1) { return [double]$sorted[[int][math]::Floor($n / 2)] }
    return ([double]$sorted[$n / 2 - 1] + [double]$sorted[$n / 2]) / 2.0
}

function Get-P95([double[]]$Values) {
    $sorted = @($Values | Sort-Object)
    $n = $sorted.Count
    if ($n -eq 0) { return 0 }
    $index = [math]::Ceiling(0.95 * $n) - 1
    if ($index -lt 0) { $index = 0 }
    return [double]$sorted[$index]
}

$root = (Get-Location).Path
$source = Join-Path $root "report-testing\benchmark\PerformanceBenchmarkTest.java"
$targetDir = Join-Path $root "app\src\androidTest\java\com\studymentor\app\performance"
$target = Join-Path $targetDir "PerformanceBenchmarkTest.java"
$resultsDir = Join-Path $root "report-testing\results"
$raw = Join-Path $resultsDir "performance-results-auto.csv"
$summary = Join-Path $resultsDir "performance-summary-auto.csv"
$display = Join-Path $resultsDir "performance-summary-auto.txt"

if (-not (Test-Path $source)) {
    throw "Missing benchmark source: $source"
}
if (-not (Test-Path ".\gradlew.bat")) {
    throw "Run this script from the AI_Study_Mentor project root."
}
if (-not (Test-Path $Adb)) {
    throw "adb.exe was not found at: $Adb"
}

New-Item -ItemType Directory -Force -Path $resultsDir | Out-Null

Write-Host "=== 1/6 Checking connected Android device ==="
$devices = & $Adb devices
$devices | Write-Host
if (-not ($devices -match "\sdevice\s*$")) {
    throw "No authorized Android device detected."
}

if (Test-Path $target) {
    Remove-Item $target -Force
}
New-Item -ItemType Directory -Force -Path $targetDir | Out-Null
Copy-Item $source $target

try {
    Write-Host ""
    Write-Host "=== 2/6 Building debug app + benchmark test APK ==="
    & ".\gradlew.bat" assembleDebug assembleDebugAndroidTest --rerun-tasks
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle build failed."
    }

    $appApk = Get-ChildItem ".\app\build\outputs\apk\debug" -Filter "*.apk" -File |
        Where-Object { $_.Name -notmatch "androidTest" } |
        Select-Object -First 1
    $testApk = Get-ChildItem ".\app\build\outputs\apk\androidTest\debug" -Filter "*.apk" -File |
        Select-Object -First 1

    if (-not $appApk) { throw "Debug app APK not found." }
    if (-not $testApk) { throw "AndroidTest APK not found." }

    Write-Host ""
    Write-Host "=== 3/6 Installing APKs without automatic post-test uninstall ==="
    & $Adb install -r -t $appApk.FullName
    if ($LASTEXITCODE -ne 0) { throw "Could not install debug app APK." }

    & $Adb install -r -t $testApk.FullName
    if ($LASTEXITCODE -ne 0) { throw "Could not install androidTest APK." }

    $instrumentationLines = & $Adb shell pm list instrumentation
    $matching = $instrumentationLines |
        Where-Object { $_ -match "com\.studymentor\.app\.test" } |
        Select-Object -First 1

    if (-not $matching) {
        Write-Host ($instrumentationLines -join "`n")
        throw "Could not find the instrumentation runner for com.studymentor.app.test."
    }

    if ($matching -notmatch "^instrumentation:(\S+)") {
        throw "Could not parse instrumentation component: $matching"
    }
    $component = $Matches[1]

    Write-Host "Instrumentation: $component"

    # Remove stale result from previous runs, if present.
    & $Adb shell run-as com.studymentor.app rm -f "files/report-testing/performance-results-auto.csv" | Out-Null

    Write-Host ""
    Write-Host "=== 4/6 Running benchmark: 10 Gemini + 10 OCR ==="
    $instrumentOutput = & $Adb shell am instrument -w -r `
        -e class "com.studymentor.app.performance.PerformanceBenchmarkTest" `
        $component 2>&1

    $instrumentOutput | Write-Host

    if ($LASTEXITCODE -ne 0 -or
        ($instrumentOutput -match "FAILURES!!!|INSTRUMENTATION_FAILED|shortMsg=Process crashed")) {
        throw "Instrumentation benchmark failed."
    }

    Write-Host ""
    Write-Host "=== 5/6 Exporting raw CSV immediately ==="

    # Confirm file exists in target app internal storage.
    $exists = & $Adb shell run-as com.studymentor.app sh -c `
        "'test -f files/report-testing/performance-results-auto.csv && echo EXISTS'"
    if (-not ($exists -match "EXISTS")) {
        throw "Benchmark finished, but internal CSV was not found."
    }

    # Use cmd redirection so the raw bytes go directly to the PC.
    $quotedAdb = '"' + $Adb + '"'
    $quotedRaw = '"' + $raw + '"'
    $cmd = "$quotedAdb exec-out run-as com.studymentor.app cat files/report-testing/performance-results-auto.csv > $quotedRaw"
    cmd.exe /d /c $cmd
    if ($LASTEXITCODE -ne 0 -or -not (Test-Path $raw) -or (Get-Item $raw).Length -eq 0) {
        throw "Could not export benchmark CSV to the PC."
    }

    $rows = @(Import-Csv $raw)
    $expectedGemini = @($rows | Where-Object { $_.Metric -eq "Gemini response" })
    $expectedOcr = @($rows | Where-Object { $_.Metric -eq "OCR" })

    if ($expectedGemini.Count -ne 10 -or $expectedOcr.Count -ne 10) {
        throw "Expected 10 Gemini and 10 OCR rows, got Gemini=$($expectedGemini.Count), OCR=$($expectedOcr.Count)."
    }

    Write-Host ""
    Write-Host "=== 6/6 FINAL PERFORMANCE SUMMARY ==="

    $summaryRows = foreach ($metric in @("Gemini response", "OCR")) {
        $group = @($rows | Where-Object { $_.Metric -eq $metric })
        [double[]]$values = @($group | ForEach-Object { [double]$_.Duration_ms })
        $successCount = @($group | Where-Object { $_.'Success (1/0)' -eq "1" }).Count
        $samples = $group.Count
        $failed = $samples - $successCount

        [pscustomobject]@{
            Metric = $metric
            Samples = $samples
            Average_ms = [math]::Round((($values | Measure-Object -Average).Average), 2)
            Median_ms = [math]::Round((Get-Median $values), 2)
            P95_ms = [math]::Round((Get-P95 $values), 2)
            Min_ms = [math]::Round((($values | Measure-Object -Minimum).Minimum), 2)
            Max_ms = [math]::Round((($values | Measure-Object -Maximum).Maximum), 2)
            SuccessRate_pct = [math]::Round(($successCount * 100.0 / $samples), 2)
            ErrorRate_pct = [math]::Round(($failed * 100.0 / $samples), 2)
        }
    }

    $summaryRows | Export-Csv $summary -NoTypeInformation -Encoding UTF8

    $geminiMeta = $expectedGemini | Select-Object -First 1
    Write-Host ("Device: {0} | Android API: {1} | Gemini network: {2} | Test date: {3}" -f `
        $geminiMeta.'Device or AVD',
        $geminiMeta.'Android API',
        $geminiMeta.Network,
        $geminiMeta.'Test date')
    Write-Host "OCR mode: On-device | Runs per metric: 10"
    Write-Host ""

    $table = $summaryRows |
        Format-Table Metric,Samples,Average_ms,Median_ms,P95_ms,Min_ms,Max_ms,SuccessRate_pct,ErrorRate_pct -AutoSize |
        Out-String -Width 220

    $header = "Device: $($geminiMeta.'Device or AVD') | Android API: $($geminiMeta.'Android API') | Gemini network: $($geminiMeta.Network) | Test date: $($geminiMeta.'Test date')`r`nOCR mode: On-device | Runs per metric: 10`r`n"
    ($header + $table) | Set-Content -Encoding UTF8 $display
    Write-Host $table

    Write-Host "Raw CSV:     $raw"
    Write-Host "Summary CSV: $summary"
    Write-Host "Display TXT: $display"
    Write-Host ""
    Write-Host "SCREENSHOT NOW: capture from '=== 6/6 FINAL PERFORMANCE SUMMARY ===' through the two result rows."
}
finally {
    if (Test-Path $target) {
        Remove-Item $target -Force
        Write-Host ""
        Write-Host "Temporary benchmark source removed from app/src/androidTest."
    }
}
