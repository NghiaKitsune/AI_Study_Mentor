param(
    [switch]$SkipInstrumentation
)

$ErrorActionPreference = "Continue"
$results = "report-testing/results"
New-Item -ItemType Directory -Force -Path $results | Out-Null

function Run-GradleTask([string]$Name, [string]$LogFile) {
    Write-Host "`n=== Running $Name ==="
    & .\gradlew.bat $Name --console=plain 2>&1 | Tee-Object -FilePath $LogFile
    $code = $LASTEXITCODE
    Write-Host "=== $Name exit code: $code ==="
    return $code
}

$statuses = @()
$statuses += [PSCustomObject]@{ Task = "testDebugUnitTest"; ExitCode = (Run-GradleTask "testDebugUnitTest" "$results/unit-test-console.txt") }
if (-not $SkipInstrumentation) {
    $statuses += [PSCustomObject]@{ Task = "connectedDebugAndroidTest"; ExitCode = (Run-GradleTask "connectedDebugAndroidTest" "$results/instrumentation-console.txt") }
}
$statuses += [PSCustomObject]@{ Task = "lintDebug"; ExitCode = (Run-GradleTask "lintDebug" "$results/lint-console.txt") }
$statuses += [PSCustomObject]@{ Task = "assembleRelease"; ExitCode = (Run-GradleTask "assembleRelease" "$results/release-build-console.txt") }
$statuses | Export-Csv -NoTypeInformation -Encoding UTF8 "$results/gradle-task-status.csv"

try {
    & "$PSScriptRoot\collect-test-summary.ps1"
} catch {
    Write-Warning $_.Exception.Message
}

Write-Host "`nEvidence logs are under $results"
Write-Host "Do not report a task as PASS unless its ExitCode is 0 and the corresponding report exists."
