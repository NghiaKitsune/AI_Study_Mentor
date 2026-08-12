param(
    [int]$Runs = 10,
    [string]$Package = "com.studymentor.app",
    [string]$Activity = ".ui.SplashActivity",
    [string]$Output = "report-testing/results/cold-start.csv"
)

$ErrorActionPreference = "Stop"
if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
    throw "adb was not found. Open Android Studio/SDK platform-tools or add adb to PATH."
}

$dir = Split-Path -Parent $Output
if ($dir) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }

$rows = @()
for ($i = 1; $i -le $Runs; $i++) {
    adb shell am force-stop $Package | Out-Null
    Start-Sleep -Milliseconds 500
    $raw = adb shell am start -W -n "$Package/$Activity" 2>&1
    $totalLine = $raw | Where-Object { $_ -match '^TotalTime:' } | Select-Object -First 1
    if (-not $totalLine) {
        throw "Could not read TotalTime on run $i. Output:`n$($raw -join [Environment]::NewLine)"
    }
    $total = [int](($totalLine -split ':', 2)[1].Trim())
    $rows += [PSCustomObject]@{
        Run = $i
        TotalTime_ms = $total
        Android_API = (adb shell getprop ro.build.version.sdk).Trim()
        Device_Model = (adb shell getprop ro.product.model).Trim()
        Timestamp = (Get-Date -Format 'yyyy-MM-dd HH:mm:ss')
    }
}

$rows | Export-Csv -NoTypeInformation -Encoding UTF8 $Output
$values = @($rows.TotalTime_ms | Sort-Object)
$avg = [Math]::Round((($values | Measure-Object -Average).Average), 2)
$median = if ($values.Count % 2 -eq 1) {
    $values[[int][Math]::Floor($values.Count / 2)]
} else {
    [Math]::Round(($values[$values.Count/2 - 1] + $values[$values.Count/2]) / 2.0, 2)
}
$p95Index = [Math]::Max(0, [Math]::Ceiling(0.95 * $values.Count) - 1)
$p95 = $values[$p95Index]
$summary = [System.IO.Path]::ChangeExtension($Output, '.summary.txt')
@(
    "Cold start runs: $Runs",
    "Average_ms: $avg",
    "Median_ms: $median",
    "P95_ms: $p95",
    "Min_ms: $($values[0])",
    "Max_ms: $($values[-1])"
) | Set-Content -Encoding UTF8 $summary

Write-Host "Saved cold-start data to $Output"
Write-Host "Saved summary to $summary"
