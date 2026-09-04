# Peggy

A personal capture, calendar, tasks, and notes app for one user. No accounts, no cloud, no sync — everything lives in Room on the device. Kotlin Multiplatform + Compose Multiplatform; Android first, JVM desktop second.

**The core principle**: capturing or completing anything takes under 2 seconds and zero decisions. Nothing is required except a line of text. No confirmation dialogs anywhere — undo everywhere instead.

## What it does

- **Capture**: the app opens to a text field with the keyboard up. Type naturally — `lunch with sam tomorrow at noon #work !!` — and a pure-Kotlin parser turns date words, times, `!`/`!!` priority, `#tags`, durations (`for 2h`), and recurrence (`every friday`) into removable chips. Enter saves. Voice capture via the system recognizer. Capture also works from a home-screen widget, a quick-settings tile, an app-icon long-press, and the Android share sheet (shared text is saved instantly).
- **Today**: everything due or scheduled today plus overdue, in time order (Overdue / Morning / Afternoon / Evening / Anytime), with an inbox badge for one-tap triage (Today · Tomorrow · This week · Someday · Event · Note). Swipe right completes, swipe left deletes, tap edits the title inline, long-press drags to reorder.
- **Calendar**: month view with per-day dots, week strip, day agenda. Long-press a day to capture for that date. Full event editor: all-day, start/end, repeat (incl. custom RRULE), reminder, color, notes.
- **Notes**: staggered grid, pinned first, search, colors, and a rich text editor (bold/italic/underline/headings/lists) that autosaves.
- **Reminders**: exact alarms that survive reboots, with Done / Snooze 10 min / Snooze 1 hour actions. Done completes without opening the app.
- **AI parsing (optional, bring-your-own-key)**: Settings → AI parsing. Point it at a local **Ollama** (base URL like `http://<your-pc>:11434/v1` + a model name), a **Claude** API key, an **OpenAI** key, or any **custom** OpenAI-compatible endpoint. The built-in parser still handles the instant capture; the AI re-parses in the background and quietly upgrades the item (with undo) — so "coffee w/ jess thursday around 130ish" becomes a real Thursday 1:00 PM event. Cloud providers see your capture text; Ollama keeps everything local.
- **Everything else**: global search, a Today home-screen widget with tappable checkboxes, JSON export/import via the system file picker, dynamic color (Material You), edge-to-edge, predictive back.

Non-obvious product decisions live in [DECISIONS.md](DECISIONS.md).

## Prerequisites

- JDK 17+ on `JAVA_HOME` (this machine: Temurin 21 at `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot`)
- Android SDK — path read from `local.properties` (`sdk.dir=...`), currently `C:\Users\doni\AppData\Local\Android\Sdk`

## Run

**Desktop:**

```
.\gradlew :composeApp:run
```

Desktop shortcuts: `Ctrl+N` focus capture · `N` new item on the current tab · `Ctrl+F` search · arrows move the calendar / select rows on Today · `Enter` completes the selected row · `Delete` deletes it.

**Android** (device/emulator via adb):

```
.\gradlew :composeApp:installDebug
```

## Release builds

**Android APK (sideloadable):**

```
.\gradlew :composeApp:assembleRelease
```

APK: `composeApp\build\outputs\apk\release\composeApp-release.apk` — install with `adb install -r <apk>`.

Signing reads `keystore.properties` (gitignored) or the `TINA_KEYSTORE_FILE`, `TINA_KEYSTORE_PASSWORD`, `TINA_KEY_ALIAS`, `TINA_KEY_PASSWORD` environment variables (CI). On a fresh clone generate a keystore with `keytool -genkeypair` and write the four properties; never commit them. Under Play App Signing this is the upload key only.

Pro: `tina.proOverride=true` in `local.properties` (gitignored) makes your own builds Pro without a purchase; CI builds never set it. Product ids and prices live in `docs/MONETIZATION.md`.

Sideload updates must be signed with the same keystore — back `release.keystore` up if you distribute anything.

**Desktop distributable (Windows):**

```
.\gradlew :composeApp:createReleaseDistributable
```

Output: `composeApp\build\compose\binaries\main-release\app\Peggy\` — a self-contained folder with `Peggy.exe` (bundled JRE, no install needed; zip it to share). An MSI installer is also configured (`.\gradlew :composeApp:packageReleaseMsi`) but requires the [WiX Toolset 3.x](https://wixtoolset.org) on PATH.

Desktop data lives in `~\.tina\` (`tina.db`, `settings.preferences_pb`).

## Development

### Developer options

Settings → Developer (always visible in the `dev` build; in a Play build tap Version seven
times) re-runs the first-run cards, asks for a store review on the spot, or resets the
once-only review ask so the real trigger fires again on the twentieth capture. Play only
shows its review sheet for apps it installed, so on a sideload the row reports
"unavailable" and the flow is exercised up to that point. Desktop: run with `-Dtina.dev=true`.

### Keeping Settings honest

Settings copy drifts when features change. `/settings-audit` (a project skill in
`.claude/skills/`) rebuilds the truth from the code each run — rows, subpages, What's new,
onboarding cards — cross-checks it, fixes the drift, and proves it with a build and
screenshots. It names no rows or features itself, so it does not go stale.

### Trying a change on your phone without Play

The Play build is signed by Google, so an upload-key APK cannot install over it. Build the `dev` variant instead: same release code, package `com.peggy.app.dev`, shown as "Peggy dev" next to the real app with its own data.

```bash
ANDROID_SERIAL=<device> ./gradlew :composeApp:installDev
```

Static launcher shortcuts in the dev build open the Play app (they name the package); everything else is independent.

- Tests (parser, recurrence, reminders, backup, repository — runs on JVM): `.\gradlew :composeApp:desktopTest`
- One `items` table holds every entity type; type changes are lossless. See [DECISIONS.md](DECISIONS.md) for the data-model and parser rules.
