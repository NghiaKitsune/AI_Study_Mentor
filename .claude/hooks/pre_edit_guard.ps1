# pre_edit_guard.ps1 — PreToolUse hook (Edit/Write)
# Warns when Claude is about to edit a critical file.
# Receives JSON via stdin with tool_input.file_path

param()

try {
    $Input = [Console]::In.ReadToEnd()
    $Json  = $Input | ConvertFrom-Json
    $FilePath = $Json.tool_input.file_path
} catch {
    exit 0  # If we can't parse, don't block
}

if (-not $FilePath) { exit 0 }

$CriticalFiles = @(
    "AndroidManifest.xml",
    "build.gradle",
    "themes.xml",
    "colors.xml",
    "dimens.xml",
    "AppDatabase.java",
    "Session.java",
    "StudyMentorApp.java"
)

$FileName = [System.IO.Path]::GetFileName($FilePath)

foreach ($critical in $CriticalFiles) {
    if ($FileName -eq $critical) {
        $ProjectDir = "D:\College BTEC\Application Development\ASM-20260430T042155Z-3-001\Update Version\android-starter"
        $Timestamp  = Get-Date -Format "yyyy-MM-dd HH:mm:ss"

        # Log to change_log.md
        $LogLine = "[$Timestamp] EDITING CRITICAL: $FilePath"
        Add-Content "$ProjectDir\.claude\change_log.md" $LogLine -Encoding UTF8

        Write-Host ""
        Write-Host "⚠  CRITICAL FILE: $FileName"
        Write-Host "   Path: $FilePath"
        Write-Host "   This file affects core app behavior. Proceed carefully."
        Write-Host "   Changes have been logged to .claude\change_log.md"
        Write-Host ""

        exit 0  # Warn but don't block
    }
}

exit 0
