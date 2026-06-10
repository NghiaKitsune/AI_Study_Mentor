# pre_xml_validator.ps1 — PreToolUse hook (Edit/Write on XML files)
# Validates @drawable/, @color/, @dimen/, @string/ references in new content.
# Receives JSON via stdin with tool_input.file_path + tool_input.new_string (Edit)
# or tool_input.content (Write)

param()

$ProjectDir = "D:\College BTEC\Application Development\ASM-20260430T042155Z-3-001\Update Version\android-starter"
$ResDir     = "$ProjectDir\app\src\main\res"
$StatusDir  = "$ProjectDir\.claude\status"

try {
    $RawInput = [Console]::In.ReadToEnd()
    $Json     = $RawInput | ConvertFrom-Json
    $FilePath = $Json.tool_input.file_path
} catch {
    exit 0
}

if (-not $FilePath) { exit 0 }

# Only validate XML layout and drawable files
$IsLayout   = $FilePath -match "res[/\\]layout[/\\].*\.xml$"
$IsDrawable = $FilePath -match "res[/\\]drawable[/\\].*\.xml$"
if (-not ($IsLayout -or $IsDrawable)) { exit 0 }

# Get the new content to validate
$NewContent = $Json.tool_input.new_string
if (-not $NewContent) { $NewContent = $Json.tool_input.content }
if (-not $NewContent) { exit 0 }

$Missing = @()

# Check @drawable/xxx references
$DrawableRefs = [regex]::Matches($NewContent, '@drawable/(\w+)') | ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique
foreach ($ref in $DrawableRefs) {
    $exists = (Test-Path "$ResDir\drawable\$ref.xml") -or
              (Test-Path "$ResDir\drawable-v24\$ref.xml") -or
              (Get-ChildItem "$ResDir" -Filter "$ref.*" -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1)
    if (-not $exists) {
        $Missing += "  - @drawable/$ref → NOT FOUND in res/drawable/"
    }
}

# Check @color/xxx references (look in res/values/colors.xml)
$ColorRefs = [regex]::Matches($NewContent, '@color/(\w+)') | ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique
$ColorsXml = "$ResDir\values\colors.xml"
if (Test-Path $ColorsXml) {
    $ColorsContent = Get-Content $ColorsXml -Raw
    foreach ($ref in $ColorRefs) {
        if ($ColorsContent -notmatch "name=`"$ref`"") {
            $Missing += "  - @color/$ref → NOT FOUND in res/values/colors.xml"
        }
    }
}

# Check @dimen/xxx references
$DimenRefs = [regex]::Matches($NewContent, '@dimen/(\w+)') | ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique
$DimensXml = "$ResDir\values\dimens.xml"
if (Test-Path $DimensXml) {
    $DimensContent = Get-Content $DimensXml -Raw
    foreach ($ref in $DimenRefs) {
        if ($DimensContent -notmatch "name=`"$ref`"") {
            $Missing += "  - @dimen/$ref → NOT FOUND in res/values/dimens.xml"
        }
    }
}

# Check @string/xxx references
$StringRefs = [regex]::Matches($NewContent, '@string/(\w+)') | ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique
$StringsXml = "$ResDir\values\strings.xml"
if (Test-Path $StringsXml) {
    $StringsContent = Get-Content $StringsXml -Raw
    foreach ($ref in $StringRefs) {
        if ($StringsContent -notmatch "name=`"$ref`"") {
            $Missing += "  - @string/$ref → NOT FOUND in res/values/strings.xml"
        }
    }
}

# Save result to status
$FileName = [System.IO.Path]::GetFileName($FilePath)
if ($Missing.Count -gt 0) {
    @{ file = $FilePath; missing = $Missing; timestamp = (Get-Date -Format "yyyy-MM-dd HH:mm:ss") } |
        ConvertTo-Json | Set-Content "$StatusDir\resource_check.json" -Encoding UTF8

    Write-Host ""
    Write-Host "⚠  MISSING RESOURCES in $FileName ($($Missing.Count) issues):"
    $Missing | ForEach-Object { Write-Host $_ }
    Write-Host "   These must exist before build will succeed."
    Write-Host ""
} else {
    @{ file = $FilePath; missing = @(); timestamp = (Get-Date -Format "yyyy-MM-dd HH:mm:ss") } |
        ConvertTo-Json | Set-Content "$StatusDir\resource_check.json" -Encoding UTF8
}

exit 0  # Always allow the edit — warn only
