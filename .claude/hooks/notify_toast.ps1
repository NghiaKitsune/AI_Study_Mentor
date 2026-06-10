# notify_toast.ps1 — Windows balloon tip notification
# Shows a system tray notification with build result.
# Called from orchestrate.ps1

param(
    [string]$Title  = "Study Mentor Build",
    [string]$Body   = "Build complete",
    [string]$Status = "ok"   # "ok" | "fail" | "warn"
)

try {
    Add-Type -AssemblyName System.Windows.Forms
    Add-Type -AssemblyName System.Drawing

    $Icon = switch ($Status.ToLower()) {
        "ok"   { [System.Windows.Forms.ToolTipIcon]::Info }
        "fail" { [System.Windows.Forms.ToolTipIcon]::Error }
        "warn" { [System.Windows.Forms.ToolTipIcon]::Warning }
        default { [System.Windows.Forms.ToolTipIcon]::Info }
    }

    $SysIcon = switch ($Status.ToLower()) {
        "ok"   { [System.Drawing.SystemIcons]::Information }
        "fail" { [System.Drawing.SystemIcons]::Error }
        "warn" { [System.Drawing.SystemIcons]::Warning }
        default { [System.Drawing.SystemIcons]::Information }
    }

    $Notifier = New-Object System.Windows.Forms.NotifyIcon
    $Notifier.Icon    = $SysIcon
    $Notifier.Visible = $true
    $Notifier.ShowBalloonTip(4000, $Title, $Body, $Icon)

    Start-Sleep -Milliseconds 500
    $Notifier.Dispose()
} catch {
    # Fallback: just print to console (notification not critical)
    Write-Host "[$Status.ToUpper()] $Title — $Body"
}

exit 0
