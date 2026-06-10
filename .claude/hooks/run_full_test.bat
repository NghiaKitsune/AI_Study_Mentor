@echo off
REM run_full_test.bat — Manual full test pipeline
REM Usage: .claude\hooks\run_full_test.bat
REM Runs: build → install → launch → logcat → screenshot

setlocal enabledelayedexpansion

set "PROJECT_DIR=D:\College BTEC\Application Development\ASM-20260430T042155Z-3-001\Update Version\android-starter"
set "SDK_DIR=%LOCALAPPDATA%\Android\Sdk"
set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
set "ADB=%SDK_DIR%\platform-tools\adb.exe"
set "APK=%PROJECT_DIR%\app\build\outputs\apk\debug\app-debug.apk"
set "PREFS_FILE=%PROJECT_DIR%\.claude\mock_prefs.xml"

cd /d "%PROJECT_DIR%"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo.
echo ============================================
echo  FULL TEST PIPELINE — AI Study Mentor
echo ============================================
echo.

REM ===== STEP 1: BUILD =====
echo [1/5] Building...
call gradlew.bat assembleDebug 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [FAIL] Build failed. Fix errors before testing.
    exit /b 1
)
echo [OK] Build passed.
echo.

REM ===== STEP 2: CHECK EMULATOR =====
echo [2/5] Checking emulator...
"%ADB%" devices 2>nul | findstr "emulator.*device" >nul
if %ERRORLEVEL% NEQ 0 (
    echo [WARN] No emulator running. Launching Pixel6_API33...
    start "" "%SDK_DIR%\emulator\emulator.exe" -avd Pixel6_API33 -no-snapshot-load -no-audio -gpu swiftshader_indirect
    echo Waiting 60s for boot...
    timeout /t 60 /nobreak >nul
    "%ADB%" -s emulator-5554 shell getprop sys.boot_completed 2>nul | findstr "1" >nul
    if %ERRORLEVEL% NEQ 0 (
        echo [FAIL] Emulator did not boot in time.
        exit /b 1
    )
)
echo [OK] Emulator ready.
echo.

REM ===== STEP 3: WRITE MOCK SESSION PREFS =====
echo [3/5] Setting up mock logged-in session...
set "XML_CONTENT=<?xml version="1.0" encoding="utf-8" standalone="yes" ?><map><string name="auth_token">mock_token_123</string><string name="user_email">test@example.com</string><string name="user_name">Test User</string><boolean name="onboarded" value="true" /><boolean name="onboarding_seen" value="true" /><string name="user_level">high-school</string><string name="subjects">math,science</string><int name="streak_days" value="7" /></map>"
powershell -Command "[System.IO.File]::WriteAllText('%PREFS_FILE%', '<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"yes\" ?><map><string name=\"auth_token\">mock_token_123</string><string name=\"user_email\">test@example.com</string><string name=\"user_name\">Test User</string><boolean name=\"onboarded\" value=\"true\" /><boolean name=\"onboarding_seen\" value=\"true\" /><string name=\"user_level\">high-school</string><string name=\"subjects\">math,science</string><int name=\"streak_days\" value=\"7\" /></map>', [System.Text.Encoding]::UTF8)"
"%ADB%" -s emulator-5554 push "%PREFS_FILE%" /sdcard/prefs_tmp.xml >nul 2>&1
"%ADB%" -s emulator-5554 shell "su 0 sh -c 'mkdir -p /data/data/com.studymentor.app/shared_prefs && cp /sdcard/prefs_tmp.xml /data/data/com.studymentor.app/shared_prefs/com.studymentor.app_preferences.xml && chown 10174:10174 /data/data/com.studymentor.app/shared_prefs/com.studymentor.app_preferences.xml'" >nul 2>&1
echo [OK] Mock session prefs set (logged in, onboarded, streak=7).
echo.

REM ===== STEP 4: INSTALL + LAUNCH =====
echo [4/5] Installing and launching...
"%ADB%" -s emulator-5554 logcat -c >nul 2>&1
"%ADB%" -s emulator-5554 install -r "%APK%" 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [FAIL] Install failed.
    exit /b 1
)
"%ADB%" -s emulator-5554 shell monkey -p com.studymentor.app -c android.intent.category.LAUNCHER 1 >nul 2>&1
echo Waiting 6s for app to settle...
timeout /t 6 /nobreak >nul

REM ===== STEP 5: LOGCAT + SCREENSHOT =====
echo [5/5] Checking for crashes and taking screenshot...

"%ADB%" -s emulator-5554 logcat -d 2>nul | findstr /i "FATAL EXCEPTION" > "%PROJECT_DIR%\.claude\status\crash_check.txt"
for %%f in ("%PROJECT_DIR%\.claude\status\crash_check.txt") do set CRASH_SIZE=%%~zf

powershell -Command "$ts = Get-Date -Format 'yyyy-MM-dd_HH-mm-ss'; $sdk = $env:LOCALAPPDATA + '\Android\Sdk'; & \"$sdk\platform-tools\adb.exe\" -s emulator-5554 shell screencap /sdcard/test_screen.png 2>$null; & \"$sdk\platform-tools\adb.exe\" -s emulator-5554 pull /sdcard/test_screen.png \"%PROJECT_DIR%\.claude\screenshots\$ts.png\" 2>$null; Write-Host \"Screenshot: .claude\screenshots\$ts.png\""

echo.
echo ============================================
if !CRASH_SIZE! GTR 0 (
    echo  RESULT: FAIL — Crashes detected^^!
    echo  Check: .claude\status\crash_check.txt
    echo ============================================
    type "%PROJECT_DIR%\.claude\status\crash_check.txt"
    exit /b 1
) else (
    echo  RESULT: PASS — No crashes detected
    echo ============================================
    exit /b 0
)
