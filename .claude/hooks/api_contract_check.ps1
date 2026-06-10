# api_contract_check.ps1 — Validates ChatResponse.java against api-contract schema
# Runs from orchestrate.ps1 when MockAiService.java or ChatResponse.java was recently changed.
# Can also be called standalone.

param()

$ProjectDir   = "D:\College BTEC\Application Development\ASM-20260430T042155Z-3-001\Update Version\android-starter"
$SchemaFile   = "$ProjectDir\api-contract\chat-response.schema.json"
$JavaFile     = "$ProjectDir\app\src\main\java\com\studymentor\app\api\ChatResponse.java"
$ErrorLog     = "$ProjectDir\.claude\error_log.md"
$ChangeLog    = "$ProjectDir\.claude\change_log.md"

# Only run if MockAiService or ChatResponse was recently changed
$RecentChanges = Get-Content $ChangeLog -Raw -ErrorAction SilentlyContinue
$ShouldRun = $RecentChanges -match "MockAiService\.java|ChatResponse\.java"
if (-not $ShouldRun) {
    Write-Host "  [api_contract] Skipped (no changes to API files)"
    exit 0
}

Write-Host "  [api_contract] Checking ChatResponse.java vs schema..."

if (-not (Test-Path $SchemaFile)) {
    Write-Host "  [api_contract] Schema not found at $SchemaFile — skipping"
    exit 0
}
if (-not (Test-Path $JavaFile)) {
    Write-Host "  [api_contract] ChatResponse.java not found — skipping"
    exit 0
}

# Parse schema fields
$Schema = Get-Content $SchemaFile -Raw | ConvertFrom-Json
$SchemaFields = $Schema.properties.PSObject.Properties.Name

# Parse Java fields (look for @SerializedName or field declarations)
$JavaContent  = Get-Content $JavaFile -Raw
$JavaFields   = [regex]::Matches($JavaContent, '@SerializedName\("(\w+)"\)') |
                    ForEach-Object { $_.Groups[1].Value }
if ($JavaFields.Count -eq 0) {
    # Fallback: look for public field names
    $JavaFields = [regex]::Matches($JavaContent, 'public\s+\w+\s+(\w+)\s*[;=]') |
                      ForEach-Object { $_.Groups[1].Value }
}

$Mismatches = @()

# Fields in schema but missing in Java
foreach ($sf in $SchemaFields) {
    if ($JavaFields -notcontains $sf) {
        $Mismatches += "  - Schema field '$sf' missing in ChatResponse.java"
    }
}

# Fields in Java but not in schema
foreach ($jf in $JavaFields) {
    if ($jf -notin @("serialVersionUID") -and $SchemaFields -notcontains $jf) {
        $Mismatches += "  - Java field '$jf' not declared in schema"
    }
}

$Timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"

if ($Mismatches.Count -gt 0) {
    Write-Host ""
    Write-Host "  ⚠  API CONTRACT MISMATCH ($($Mismatches.Count) issues):"
    $Mismatches | ForEach-Object { Write-Host $_ }

    $Entry = @"

## [$Timestamp] API Contract Mismatch ⚠ NEEDS FIX
$($Mismatches -join "`n")
**Agent:** api_contract_check.ps1 (auto)
"@
    Add-Content $ErrorLog $Entry -Encoding UTF8
    exit 1
} else {
    Write-Host "  ✓ ChatResponse.java matches schema ($($SchemaFields.Count) fields verified)"
    exit 0
}
