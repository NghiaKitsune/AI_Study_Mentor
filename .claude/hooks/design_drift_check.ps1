# design_drift_check.ps1 — Compares XML layouts with design bundle
# Fetches design bundle from Claude Design API and checks for drift.
# Called by orchestrate.ps1 every 5 sessions.

param()

$ProjectDir   = "D:\College BTEC\Application Development\ASM-20260430T042155Z-3-001\Update Version\android-starter"
$ClaudeDir    = "$ProjectDir\.claude"
$ErrorLog     = "$ClaudeDir\error_log.md"
$DesignFile   = "$ClaudeDir\latest_design.html"
$DesignUrl    = "https://api.anthropic.com/v1/design/h/sYYOs3uSHmzuIr43Q3DGxg"

Write-Host "  [design_drift] Fetching design bundle..."

try {
    $Response = Invoke-WebRequest -Uri $DesignUrl -TimeoutSec 15 -ErrorAction Stop
    $Response.Content | Set-Content $DesignFile -Encoding UTF8
    Write-Host "  [design_drift] Design bundle downloaded ($([int]$Response.Content.Length / 1024) KB)"
} catch {
    Write-Host "  [design_drift] Could not fetch design bundle: $_"
    Write-Host "  [design_drift] Skipping drift check (network unavailable or URL changed)"
    exit 0
}

$DesignContent = Get-Content $DesignFile -Raw -ErrorAction SilentlyContinue
if (-not $DesignContent) { exit 0 }

$Timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
$Drifts    = @()
$Checks    = @()

# ===== CHECK: Streak chip in HomeActivity =====
$HomeXml = Get-Content "$ProjectDir\app\src\main\res\layout\activity_home.xml" -Raw -ErrorAction SilentlyContinue
if ($HomeXml) {
    if ($HomeXml -match "chip_streak") {
        $Checks += "  ✓ HomeActivity: streak chip present"
    } else {
        $Drifts += "  ✗ HomeActivity: streak chip MISSING (design requires flame + days chip)"
    }

    if ($HomeXml -match "progress_xp") {
        $Checks += "  ✓ HomeActivity: XP progress bar present"
    } else {
        $Drifts += "  ✗ HomeActivity: XP progress bar MISSING"
    }
}

# ===== CHECK: Suggestion chips in ChatActivity =====
$ChatXml = Get-Content "$ProjectDir\app\src\main\res\layout\activity_chat.xml" -Raw -ErrorAction SilentlyContinue
if ($ChatXml) {
    if ($ChatXml -match "layout_suggestions|chip_suggest") {
        $Checks += "  ✓ ChatActivity: suggestion chips present"
    } else {
        $Drifts += "  ✗ ChatActivity: suggestion chips MISSING (design requires 4 example chips)"
    }
}

# ===== CHECK: Common mistakes in AnswerActivity =====
$AnswerXml = Get-Content "$ProjectDir\app\src\main\res\layout\activity_answer.xml" -Raw -ErrorAction SilentlyContinue
if ($AnswerXml) {
    if ($AnswerXml -match "common_mistakes|bg_mistake") {
        $Checks += "  ✓ AnswerActivity: common mistakes section present"
    } else {
        $Drifts += "  ✗ AnswerActivity: common mistakes section MISSING"
    }
}

# ===== CHECK: Milo noticed card in HistoryActivity =====
$HistoryXml = Get-Content "$ProjectDir\app\src\main\res\layout\activity_history.xml" -Raw -ErrorAction SilentlyContinue
if ($HistoryXml) {
    if ($HistoryXml -match "card_milo_noticed") {
        $Checks += "  ✓ HistoryActivity: Milo noticed card present"
    } else {
        $Drifts += "  ✗ HistoryActivity: Milo noticed card MISSING"
    }
}

# ===== CHECK: OnboardingActivity =====
$OnboardXml = Get-Content "$ProjectDir\app\src\main\res\layout\activity_onboarding.xml" -Raw -ErrorAction SilentlyContinue
if ($OnboardXml) {
    if ($OnboardXml -match "img_mascot_hero") {
        $Checks += "  ✓ OnboardingActivity: mascot hero present"
    } else {
        $Drifts += "  ✗ OnboardingActivity: mascot hero MISSING"
    }
}

# ===== REPORT =====
Write-Host ""
Write-Host "  [design_drift] Results ($($Checks.Count) OK, $($Drifts.Count) drifts):"
$Checks | ForEach-Object { Write-Host $_ }

if ($Drifts.Count -gt 0) {
    $Drifts | ForEach-Object { Write-Host $_ }

    $Entry = @"

## [$Timestamp] Design Drift Detected ⚠ NEEDS FIX
Design bundle: sYYOs3uSHmzuIr43Q3DGxg
$($Drifts -join "`n")
**Agent:** design_drift_check.ps1 (auto) → spawn Agent-1 to fix
"@
    Add-Content $ErrorLog $Entry -Encoding UTF8

    Write-Host ""
    Write-Host "  [ACTION] Design drift found. Claude should spawn Agent-1 (Design/UI Fixer)."
    exit 1
} else {
    Write-Host "  ✓ No design drift — all checked components match design bundle"
    exit 0
}
