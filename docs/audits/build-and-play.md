# Audit: build, tests, Play launch readiness

Read-only audit performed 2026-09-02 against v1.6.0 (`aaf28e5`).

## 1. Build

compileSdk 37 / minSdk 31 / targetSdk 36; versionCode 8 / 1.6.0; AGP 9.1.1, Kotlin 2.4.10, CMP 1.12.0, JVM 21; config cache and build cache on.

- Signing reads a gitignored `keystore.properties`; secrets confirmed untracked. **README publishes the keystore password and keytool command.** No env-var fallback, so CI cannot sign. Under Play App Signing this key becomes the upload key only.
- **R8 and resource shrinking are off; no `proguard-rules.pro` exists.** Release ships the whole graph un-shrunk.
- **No AAB workflow**; README documents `assembleRelease` only. `bundleRelease` works by default but is untested.
- `profileinstaller` present; no app-specific baseline profile module.
- No dependency locking; material3 alpha force-pinned.

## 2. Manifest and policy

Permissions: INTERNET, ACCESS_LOCAL_NETWORK, POST_NOTIFICATIONS (checked before every notify), SCHEDULE_EXACT_ALARM (denied by default on 14+), RECEIVE_BOOT_COMPLETED. No RECORD_AUDIO (system recognizer), no storage, no foreground service, no QUERY_ALL_PACKAGES — good.

- `USE_EXACT_ALARM` not declared; needs the Play declaration form (calendar/reminders app qualifies).
- Exported components are correct; `ShareActivity` writes `EXTRA_TEXT` to the DB with no length cap and forwards it to the AI refiner silently.
- **`allowBackup` defaults true with no backup/data-extraction rules**: DB (WAL, uncheckpointed) and `settings.preferences_pb` (plaintext API key) go to Google Drive.
- `usesCleartextTraffic="true"` app-wide, no network security config.
- `<profileable android:shell="true"/>` ships in release (low risk).
- Framework light-only theme, no splash configuration.
- **No launcher icon at all** (no mipmap, no adaptive icon, no `android:icon`): installs with the stock robot. Hard blocker.
- No `localeConfig`; English only.
- App-lock/FLAG_SECURE resolve asynchronously after first composition.

## 3. Data Safety

No analytics, crash reporting, ads, accounts or backend. With a cloud AI provider on (default OFF, user key): capture text and, in Ask, the entire item database including note previews are **shared** with Anthropic/OpenAI/a custom host; API key in headers; chats resent as history. Form: Personal info + App activity/other user-generated content, shared, optional, app functionality, encrypted in transit only if cleartext is constrained; no deletion mechanism. Privacy policy required (URL in listing and in-app). Content rating, target audience 13+/18+, no ads.

## 4. Tests

11 classes, 166 tests, all JVM. Excellent: capture parser (69), recurrence, agenda builder. Good: AI parsing, backup codec, reminder math. Moderate: repository (9, real Room on desktop). Zero: migrations, AI network layer, all ViewModels, all UI, Android notifications, widgets/tiles/share, settings persistence, auto-backup, occurrence completion. No Compose UI or screenshot tests. No CI.

## 5. Dependency risks

material3 `1.12.0-alpha03` force-pinned with the expressive opt-in global (highest risk); navigation3 (young); richeditor 1.2.0 (single maintainer, the reason for the pin, reflection); calendar-compose 2.10.1 (fine); `material-icons-extended` 1.7.3 (deprecated upstream, off-cycle, large); sqlite-bundled (native per ABI). AGP 9 with `android.builtInKotlin=false` / `android.newDsl=false` escape hatches.

## 6. Release process

Exists: version catalog, caches, gitignored signing, README runbook, exported schemas, DECISIONS.md journal, clean commit history, release tags v1.x and GitHub Releases with APK + Windows zip. Missing: CI, Android Lint, detekt/ktlint, crash reporting, user-facing CHANGELOG, automated versionCode, AAB in the runbook, test-track plan (Play's 12-tester/14-day closed test for new personal accounts), store assets, dependency automation.

## Ranked master list

Must fix before any Play upload:
1. Destructive migration fallback (`AppDatabase.kt:79`).
2. Launcher icon.
3. Backup/data-extraction rules; key out of cloud backup.
4. Plaintext API key in DataStore and JSON export.
5. AAB.
6. Privacy policy + Data Safety declaring third-party sharing.
7. Keystore password out of README; rotate.

Should fix before launch: R8 with rules; `USE_EXACT_ALARM`; scoped network security config; migration tests; CI + Lint; app-lock race; prominent AI disclosure (share sheet especially); real theme + splash; `ShareActivity` clamp.

Nice: crash reporting, CHANGELOG, baseline profile module, screenshot tests, drop profileable from release, replace icons-extended, detekt/ktlint, Renovate, schedule the closed-testing window now.
