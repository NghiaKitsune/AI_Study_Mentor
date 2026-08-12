param(
    [string]$BuildRoot = "app/build",
    [string]$Output = "report-testing/results/test-summary.csv"
)

$ErrorActionPreference = "Stop"
$dir = Split-Path -Parent $Output
if ($dir) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }

$xmlFiles = Get-ChildItem -Path $BuildRoot -Recurse -Filter "TEST-*.xml" -File -ErrorAction SilentlyContinue
if (-not $xmlFiles) {
    throw "No JUnit XML files were found under $BuildRoot. Run unit/instrumentation tests first."
}

$rows = foreach ($file in $xmlFiles) {
    try {
        [xml]$xml = Get-Content -Raw $file.FullName
        $suite = $xml.testsuite
        if ($suite) {
            [PSCustomObject]@{
                Suite = [string]$suite.name
                Tests = [int]$suite.tests
                Failures = [int]$suite.failures
                Errors = if ($suite.errors) { [int]$suite.errors } else { 0 }
                Skipped = if ($suite.skipped) { [int]$suite.skipped } else { 0 }
                Time_seconds = [string]$suite.time
                Source_XML = $file.FullName.Substring((Get-Location).Path.Length).TrimStart('\')
            }
        }
    } catch {
        Write-Warning "Could not parse $($file.FullName): $($_.Exception.Message)"
    }
}

$rows | Export-Csv -NoTypeInformation -Encoding UTF8 $Output
$totalTests = ($rows | Measure-Object Tests -Sum).Sum
$totalFailures = ($rows | Measure-Object Failures -Sum).Sum
$totalErrors = ($rows | Measure-Object Errors -Sum).Sum
$totalSkipped = ($rows | Measure-Object Skipped -Sum).Sum
$passed = $totalTests - $totalFailures - $totalErrors - $totalSkipped
$summary = [System.IO.Path]::ChangeExtension($Output, '.summary.txt')
@(
    "Tests: $totalTests",
    "Passed: $passed",
    "Failed: $totalFailures",
    "Errors: $totalErrors",
    "Skipped: $totalSkipped",
    "Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
) | Set-Content -Encoding UTF8 $summary

Write-Host "Saved test summary to $Output"
Write-Host "Saved totals to $summary"
