param(
    [string]$Input = "report-testing/templates/performance-results.csv",
    [string]$Output = "report-testing/results/performance-summary.csv"
)

$ErrorActionPreference = "Stop"
$rows = Import-Csv $Input
$usable = $rows | Where-Object { $_.Duration_ms -match '^\d+(\.\d+)?$' }
if (-not $usable) { throw "No numeric Duration_ms values were found in $Input." }

$summaryRows = foreach ($group in ($usable | Group-Object Metric)) {
    $durations = @($group.Group | ForEach-Object { [double]$_.Duration_ms } | Sort-Object)
    $avg = [Math]::Round((($durations | Measure-Object -Average).Average), 2)
    $median = if ($durations.Count % 2 -eq 1) {
        $durations[[int][Math]::Floor($durations.Count / 2)]
    } else {
        [Math]::Round(($durations[$durations.Count/2 - 1] + $durations[$durations.Count/2]) / 2.0, 2)
    }
    $p95Index = [Math]::Max(0, [Math]::Ceiling(0.95 * $durations.Count) - 1)
    $successRows = $group.Group | Where-Object { $_.'Success (1/0)' -match '^[01]$' }
    $failures = @($successRows | Where-Object { $_.'Success (1/0)' -eq '0' }).Count
    $errorRate = if (@($successRows).Count -gt 0) { [Math]::Round(100.0 * $failures / @($successRows).Count, 2) } else { $null }
    [PSCustomObject]@{
        Metric = $group.Name
        Samples = $durations.Count
        Average_ms = $avg
        Median_ms = $median
        P95_ms = $durations[$p95Index]
        Min_ms = $durations[0]
        Max_ms = $durations[-1]
        ErrorRate_percent = $errorRate
    }
}

$dir = Split-Path -Parent $Output
if ($dir) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
$summaryRows | Export-Csv -NoTypeInformation -Encoding UTF8 $Output
Write-Host "Saved performance summary to $Output"
