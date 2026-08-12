param(
    [string]$Output = "report-testing/results/environment.txt"
)

$ErrorActionPreference = "SilentlyContinue"
$dir = Split-Path -Parent $Output
if ($dir) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }

$cpu = (Get-CimInstance Win32_Processor | Select-Object -First 1 -ExpandProperty Name)
$ramBytes = (Get-CimInstance Win32_PhysicalMemory | Measure-Object -Property Capacity -Sum).Sum
$ramGb = if ($ramBytes) { [Math]::Round($ramBytes / 1GB, 2) } else { "UNKNOWN" }
$os = Get-CimInstance Win32_OperatingSystem

$lines = @()
$lines += "Development environment evidence"
$lines += "Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
$lines += "CPU: $cpu"
$lines += "RAM_GB: $ramGb"
$lines += "OS: $($os.Caption)"
$lines += "OS_VERSION: $($os.Version)"
$lines += "OS_BUILD: $($os.BuildNumber)"
$lines += "Android_Studio_Version: [FILL FROM Help > About]"

if (Get-Command adb -ErrorAction SilentlyContinue) {
    $lines += "ADB_DEVICE_MODEL: $(adb shell getprop ro.product.model 2>$null)"
    $lines += "ANDROID_RELEASE: $(adb shell getprop ro.build.version.release 2>$null)"
    $lines += "ANDROID_API: $(adb shell getprop ro.build.version.sdk 2>$null)"
    $lines += "SCREEN_SIZE: $(adb shell wm size 2>$null | Select-Object -First 1)"
    $lines += "SCREEN_DENSITY: $(adb shell wm density 2>$null | Select-Object -First 1)"
} else {
    $lines += "ADB: NOT_FOUND"
}

$javaVersion = (& java -version 2>&1 | Select-Object -First 1)
$lines += "JAVA: $javaVersion"
$lines | Set-Content -Encoding UTF8 $Output
Write-Host "Saved environment evidence to $Output"
