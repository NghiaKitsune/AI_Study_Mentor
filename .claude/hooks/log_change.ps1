# log_change.ps1 — PostToolUse hook (Edit/Write)
# Appends changed file info to change_log.md
# Receives JSON via stdin with tool_name + tool_input.file_path

param()

$ProjectDir = "D:\College BTEC\Application Development\ASM-20260430T042155Z-3-001\Update Version\android-starter"
$ChangeLog  = "$ProjectDir\.claude\change_log.md"

try {
    $RawInput = [Console]::In.ReadToEnd()
    $Json     = $RawInput | ConvertFrom-Json
    $ToolName = $Json.tool_name
    $FilePath = $Json.tool_input.file_path
} catch {
    exit 0
}

if (-not $FilePath) { exit 0 }

# Normalize path separators
$FilePath = $FilePath -replace '\\', '/'

# Determine action label
$Action = switch ($ToolName) {
    "Edit"  { "EDITED" }
    "Write" { "CREATED" }
    default { "MODIFIED" }
}

# Check if file was actually created (Write to new file) vs overwritten
if ($ToolName -eq "Write") {
    $FullPath = Join-Path $ProjectDir $FilePath
    $Action = if (Test-Path $FullPath) { "OVERWRITTEN" } else { "CREATED" }
}

$Timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
$LogLine   = "[$Timestamp] ${Action}: $FilePath"

Add-Content $ChangeLog $LogLine -Encoding UTF8

exit 0
