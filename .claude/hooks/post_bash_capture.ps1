# post_bash_capture.ps1 — PostToolUse hook (Bash)
# After adb install: takes emulator screenshot
# After gradlew: logs build time to metrics
# Receives JSON via stdin with tool_input.command

param()

$ProjectDir   = "D:\College BTEC\Application Development\ASM-20260430T042155Z-3-001\Update Version\android-starter"
$SdkDir       = "$env:LOCALAPPDATA\Android\Sdk"
$Adb          = "$SdkDir\platform-tools\adb.exe"
$ScreenshotDir = "$ProjectDir\.claude\screenshots"
$MetricsFile  = "$ProjectDir\.claude\metrics.md"

try {
    $RawInput = [Console]::In.ReadToEnd()
    $Json     = $RawInput | ConvertFrom-Json
    $Command  = $Json.tool_input.command
} catch {
    exit 0
}

if (-not $Command) { exit 0 }

# ===== SCREENSHOT after adb install =====
if ($Command -match "adb.*install") {
    Start-Sleep -Seconds 4  # Wait for app to be launchable

    # Check emulator is running
    $Devices = & $Adb devices 2>$null | Out-String
    if ($Devices -match "emulator-(\d+)\s+device") {
        $DeviceId = "emulator-$($Matches[1])"
        $Timestamp = Get-Date -Format "yyyy-MM-dd_HH-mm-ss"
        $RemotePath = "/sdcard/claude_screen_$Timestamp.png"
        $LocalPath  = "$ScreenshotDir\$Timestamp.png"

        & $Adb -s $DeviceId shell screencap $RemotePath 2>$null
        & $Adb -s $DeviceId pull $RemotePath $LocalPath 2>$null
        & $Adb -s $DeviceId shell rm $RemotePath 2>$null

        if (Test-Path $LocalPath) {
            Write-Host "📸 Screenshot saved: .claude\screenshots\$Timestamp.png"
            Write-Host "   Agent-1 can read this image to detect UI layout issues."
        }
    }
}

exit 0
