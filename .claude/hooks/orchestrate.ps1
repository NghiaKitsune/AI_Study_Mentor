# orchestrate.ps1 — Main Stop Hook
# Runs after every Claude turn. Builds, checks logcat, notifies, prints action.
# Called by settings.json Stop hook.

param()

$ProjectDir = "D:\College BTEC\Application Development\ASM-20260430T042155Z-3-001\Update Version\android-starter"
$SdkDir     = "$env:LOCALAPPDATA\Android\Sdk"
$JavaHome   = "C:\Program Files\Android\Android Studio\jbr"
$Adb        = "$SdkDir\platform-tools\adb.exe"
$ClaudeDir  = "$ProjectDir\.claude"
$StatusDir  = "$ClaudeDir\status"
$LogDir     = "$ClaudeDir\logcat_history"

Set-Location $ProjectDir

$Now         = Get-Date
$Timestamp   = $Now.ToString("yyyy-MM-dd HH:mm:ss")
$FileStamp   = $Now.ToString("yyyy-MM-dd_HH-mm-ss")

Set-Content "$StatusDir\last_run.txt" $Timestamp

Write-Host ""
Write-Host "==============================================="
Write-Host " [orchestrate] $Timestamp"
Write-Host "==============================================="

# ===== STEP 1: BUILD =====
Write-Host "[1/4] Building..."
$env:JAVA_HOME = $JavaHome
$env:PATH      = "$JavaHome\bin;$env:PATH"

$BuildStart  = Get-Date
$BuildOutput = & .\gradlew.bat assembleDebug 2>&1 | Out-String
$BuildExit   = $LASTEXITCODE
$BuildTimeSec = [int]((Get-Date) - $BuildStart).TotalSeconds

$ApkPath    = "app\build\outputs\apk\debug\app-debug.apk"
$ApkSizeKb  = 0
if (Test-Path $ApkPath) {
    $ApkSizeKb = [int]((Get-Item $ApkPath).Length / 1024)
}

if ($BuildExit -eq 0) {
    @{ status = "OK"; timestamp = $Timestamp; build_time_s = $BuildTimeSec; apk_size_kb = $ApkSizeKb } |
        ConvertTo-Json | Set-Content "$StatusDir\build_status.json" -Encoding UTF8
    $BuildResult = "OK"
    Write-Host "  Build: PASSED ($BuildTimeSec s, $ApkSizeKb KB)"

    # Append to metrics.md
    $MetricLine = "| $Timestamp | ${BuildTimeSec}s | $ApkSizeKb KB | PASSED | auto |"
    Add-Content "$ClaudeDir\metrics.md" $MetricLine
} else {
    $ErrorLines = ($BuildOutput -split "`n" | Where-Object { $_ -match "(?i)error:" } | Select-Object -First 10) -join " || "
    @{ status = "FAILED"; timestamp = $Timestamp; errors = $ErrorLines } |
        ConvertTo-Json | Set-Content "$StatusDir\build_status.json" -Encoding UTF8
    $BuildResult = "FAILED"
    Write-Host "  Build: FAILED"
    Write-Host "  Errors: $ErrorLines"

    # Append to metrics.md
    $MetricLine = "| $Timestamp | ${BuildTimeSec}s | N/A | FAILED | auto |"
    Add-Content "$ClaudeDir\metrics.md" $MetricLine
}

# ===== STEP 2: LOGCAT CHECK =====
Write-Host "[2/4] Checking logcat..."
$LogcatResult = "NO_EMULATOR"

$DeviceOutput = & $Adb devices 2>$null | Out-String
if ($DeviceOutput -match "emulator-\d+\s+device") {
    $LogcatLines = & $Adb logcat -d 2>$null
    $CrashLines  = $LogcatLines | Where-Object { $_ -match "FATAL EXCEPTION|beginning of crash" }

    if ($CrashLines) {
        $Trace        = ($CrashLines | Select-Object -First 8) -join "`n"
        $LogcatResult = "CRASH"
        @{ status = "CRASH"; timestamp = $Timestamp; trace = $Trace } |
            ConvertTo-Json | Set-Content "$StatusDir\logcat_status.json" -Encoding UTF8
        $LogcatLines | Set-Content "$LogDir\$FileStamp.txt" -Encoding UTF8
        Write-Host "  Logcat: CRASH DETECTED — saved to logcat_history\$FileStamp.txt"
    } else {
        $LogcatResult = "CLEAN"
        @{ status = "CLEAN"; timestamp = $Timestamp } |
            ConvertTo-Json | Set-Content "$StatusDir\logcat_status.json" -Encoding UTF8
        Write-Host "  Logcat: CLEAN (no crashes)"
    }
} else {
    @{ status = "NO_EMULATOR"; timestamp = $Timestamp } |
        ConvertTo-Json | Set-Content "$StatusDir\logcat_status.json" -Encoding UTF8
    Write-Host "  Logcat: skipped (no emulator running)"
}

# ===== STEP 3: DRIFT COUNTER (run design check every 5 sessions) =====
$DriftCounterFile = "$StatusDir\drift_counter.txt"
$DriftCount = 0
if (Test-Path $DriftCounterFile) {
    $DriftCount = [int](Get-Content $DriftCounterFile -Raw -ErrorAction SilentlyContinue)
}
$DriftCount++
Set-Content $DriftCounterFile $DriftCount
if ($DriftCount -ge 5) {
    Set-Content $DriftCounterFile 0
    Write-Host "[3/4] Design drift check (every 5 sessions)..."
    & "$ClaudeDir\hooks\design_drift_check.ps1"
} else {
    Write-Host "[3/4] Design drift check: skipped ($DriftCount/5 sessions)"
}

# ===== STEP 4: NOTIFY =====
Write-Host "[4/4] Sending notification..."
$NotifyStatus = if ($BuildResult -eq "OK") { "ok" } else { "fail" }
$NotifyBody   = "Build: $BuildResult | Logcat: $LogcatResult"
& powershell -NonInteractive -File "$ClaudeDir\hooks\notify_toast.ps1" `
    -Title "Study Mentor" -Body $NotifyBody -Status $NotifyStatus

# ===== ACTION SUMMARY =====
Write-Host ""
Write-Host "==============================================="
Write-Host " RESULT: BUILD=$BuildResult | LOGCAT=$LogcatResult"
Write-Host "==============================================="

if ($BuildResult -eq "FAILED") {
    Write-Host ""
    Write-Host "[ACTION REQUIRED] Build failed. Claude should:"
    Write-Host "  1. Spawn Agent-3 (Evaluate+Rebuild): read .claude\status\build_status.json, fix errors, rebuild"
    Write-Host "  2. Spawn Agent-2 (Test+Log): update error_log.md and CLAUDE.md Session Log"
    exit 1
}

if ($LogcatResult -eq "CRASH") {
    $TraceContent = (Get-Content "$StatusDir\logcat_status.json" -Raw | ConvertFrom-Json).trace
    $IsUIError = $TraceContent -match "InflateException|Resources\$NotFoundException|ColorStateList"
    Write-Host ""
    Write-Host "[ACTION REQUIRED] Crash detected. Claude should:"
    if ($IsUIError) {
        Write-Host "  1. Spawn Agent-1 (Design/UI Fixer): UI-related crash — fix drawable/layout"
    } else {
        Write-Host "  1. Spawn Agent-3 (Evaluate+Rebuild): runtime crash — fix Java code"
    }
    Write-Host "  2. Spawn Agent-2 (Test+Log): update error_log.md and CLAUDE.md Session Log"
    exit 1
}

Write-Host ""
Write-Host "[ACTION] Clean run. Claude should:"
Write-Host "  Spawn Agent-2 (Test+Log): append clean-run entry to CLAUDE.md Session Log"
exit 0
