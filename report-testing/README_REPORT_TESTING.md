# AI Study Mentor — Report Testing Evidence Pack

This folder and the added test classes exist only to collect evidence for the Unit 22 report. No existing production Java/XML/Gradle file was edited by this test addition.

## Why these tests were added

The current report already has automated tests for password hashing, Gemini JSON parsing, quiz validation/evaluation/timer, progress rules and Room migration/idempotency. The report/guideline still needs stronger evidence for:

- functional requirements and requirement-level test results;
- security and user-data isolation;
- history/search/bookmark/delete behaviour;
- notification/reminder persistence;
- pending/failed/retry AI question state;
- final test totals from the current revision;
- measured performance (cold start, Gemini response, OCR duration/error rate);
- compatibility and UAT evidence.

The new automated tests target gaps that can be tested without changing production behaviour. Performance, compatibility and UAT are collected separately because they depend on a real emulator/device, network, Gemini service, OCR input and/or real participants.

## Added automated test files

### JVM unit tests

- `app/src/test/java/com/studymentor/app/api/ChatRequestContractTest.java`
  - blank request ID generation;
  - null message normalization;
  - legacy conversation ID behaviour.

### Android instrumentation tests

- `app/src/androidTest/java/com/studymentor/app/data/AuthenticationSecurityTest.java`
  - exact duplicate email DB constraint;
  - session start/logout state;
  - cascade removal of user-owned data.
- `app/src/androidTest/java/com/studymentor/app/data/HistoryNotificationDatabaseTest.java`
  - history keyword/subject/bookmark filters;
  - question-child cascade delete;
  - notification ownership/read state;
  - notification filtering and reminder schedule persistence.
- `app/src/androidTest/java/com/studymentor/app/data/QuestionStateMachineTest.java`
  - retry after FAILED;
  - stale PROCESSING recovery;
  - exact-cache user/context isolation;
  - foreign-user mutation protection.
- `app/src/androidTest/java/com/studymentor/app/security/ManifestSecurityTest.java`
  - backup disabled;
  - only launcher activity exported;
  - FileProvider private with temporary URI grants.

## 1. Run the existing + new unit tests

From the project root in Android Studio Terminal / PowerShell:

```powershell
.\gradlew.bat testDebugUnitTest --console=plain
```

Keep:

- `app/build/reports/tests/testDebugUnitTest/index.html`
- `app/build/test-results/testDebugUnitTest/`
- a screenshot showing the final total/pass/fail result.

Do not write a pass total in the report before this command completes successfully on the final revision.

## 2. Run instrumentation tests on an emulator/device

Start an Android 13+ emulator or connect a compatible device, then:

```powershell
.\gradlew.bat connectedDebugAndroidTest --console=plain
```

Keep:

- `app/build/reports/androidTests/connected/debug/`
- `app/build/outputs/androidTest-results/connected/debug/`
- a screenshot of the final instrumentation result.

## 3. Run lint and release build

```powershell
.\gradlew.bat lintDebug --console=plain
.\gradlew.bat assembleRelease --console=plain
```

Keep the generated lint report and build console evidence. Record whether the release APK is signed or unsigned; do not describe an unsigned APK as release-ready.

## 4. One-command evidence run

With an emulator already running:

```powershell
powershell -ExecutionPolicy Bypass -File .\report-testing\scripts\run-final-report-tests.ps1
```

If you only want JVM test/lint/build first:

```powershell
powershell -ExecutionPolicy Bypass -File .\report-testing\scripts\run-final-report-tests.ps1 -SkipInstrumentation
```

Logs are written to `report-testing/results/`. Generated results are evidence files, not production source.

## 5. Extract exact test totals for Table 17

After tests have generated XML files:

```powershell
powershell -ExecutionPolicy Bypass -File .\report-testing\scripts\collect-test-summary.ps1
```

Use the generated summary to fill `Cases / Passed / Failed / Skipped`. If a test is skipped, report it as skipped rather than pass.

## 6. Capture environment evidence

With the target emulator/device running:

```powershell
powershell -ExecutionPolicy Bypass -File .\report-testing\scripts\collect-environment.ps1
```

Then manually add the Android Studio version from **Help > About**. Do not add usernames, personal file paths, API keys or passwords to report screenshots.

## 7. Measure cold start

Install/run the final debug build, keep the emulator/device connected, then:

```powershell
powershell -ExecutionPolicy Bypass -File .\report-testing\scripts\collect-cold-start.ps1 -Runs 10
```

The script force-stops the package before each run and records Android `TotalTime`. It creates CSV + average/median/P95/min/max summary.

## 8. Measure Gemini and OCR performance

Use `report-testing/templates/performance-results.csv`.

For Gemini response time:

- use 10 unique normal study questions to avoid exact-cache hits;
- start timing when **Send** is pressed;
- stop when the structured answer is fully visible;
- record failures/rate-limit/error codes instead of deleting failed samples;
- keep the same device/network for the measurement set.

For OCR:

- prepare a fixed set of 10 clear images;
- start timing when the image is confirmed/selected;
- stop when editable recognized text appears;
- record whether recognition succeeded and any meaningful correction needed;
- do not claim diagram/formula accuracy unless the chosen dataset actually tests it.

After entering durations:

```powershell
powershell -ExecutionPolicy Bypass -File .\report-testing\scripts\summarize-performance.ps1
```

The summary calculates sample count, average, median, P95, min, max and error rate when success values are filled.

## 9. Compatibility

Use the `COMP-*` rows in `test-case-matrix.csv`. Recommended minimum evidence for this app's current `minSdk 33`:

- API 33 phone AVD;
- API 34 phone AVD;
- API 34 tablet AVD.

Only claim the configurations actually tested. A 16 KB page-size warning is evidence of a compatibility limitation, not a pass result.

## 10. UAT / interview

Use `uat-results.csv`. Perform UAT only after the final build is stable. Use participant codes (P01, P02...) in report evidence and avoid collecting unnecessary personal data.

Minimum task flow:

1. register/login/personalise;
2. ask AI by text;
3. camera/OCR;
4. history/bookmark/search;
5. quiz/result/dashboard;
6. one offline/error case.

Record Pass/Fail, time-on-task, ease 1–5, satisfaction 1–5, errors and comments. UAT results cannot be replaced by automated JUnit results.

## 11. What the automated tests do NOT prove

Even if every JUnit/instrumentation test passes, do not infer:

- Gemini answers are always correct;
- external API availability is guaranteed;
- OCR accurately understands diagrams/equations;
- every Android device is compatible;
- the app is production-secure;
- user usability/satisfaction is high.

Those claims require the separate performance, compatibility, security limitation review and UAT evidence described above.
