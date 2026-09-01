# tina

Kotlin Multiplatform + Compose Multiplatform scaffold. Two targets: **Android** (primary) and **JVM desktop**. No features yet — just a blank Material 3 screen with the plumbing wired up.

## Stack

- Kotlin 2.4.10, Compose Multiplatform 1.12.0, Gradle 9.5.0, AGP 9.1.1
- Single module `composeApp` with `commonMain` / `androidMain` / `desktopMain` source sets
- Material 3 + material3-adaptive; dynamic color (Material You) on Android (minSdk 31), static scheme on desktop; edge-to-edge + predictive back enabled
- Navigation 3 (`org.jetbrains.androidx.navigation3:navigation3-ui`) with one placeholder route
- Room (KMP) + bundled SQLite driver — placeholder `Note` entity/DAO exposed via `Flow`, DB lives at `getDatabasePath("tina.db")` on Android and `~/.tina/tina.db` on desktop
- Koin DI — `initKoin(platformModule)` from `commonMain`, platform modules in `androidMain`/`desktopMain`
- Wired but unused: kizitonwose Calendar, Compose Rich Editor, WorkManager (Android only)
- `Notifier` expect/actual stub for local notifications (no-op on both platforms for now)

## Prerequisites

- JDK 17+ on `JAVA_HOME` (this machine: Temurin 21 at `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot`)
- Android SDK — path is read from `local.properties` (`sdk.dir=...`), currently `C:\Users\doni\AppData\Local\Android\Sdk`

## Run

**Desktop:**

```
.\gradlew :composeApp:run
```

**Android** (device/emulator connected via adb):

```
.\gradlew :composeApp:installDebug
```

then open the "tina" app — or build the APK without installing:

```
.\gradlew :composeApp:assembleDebug
```

APK: `composeApp\build\outputs\apk\debug\composeApp-debug.apk`

## Release APK (sideloadable)

```
.\gradlew :composeApp:assembleRelease
```

APK: `composeApp\build\outputs\apk\release\composeApp-release.apk`

Signing uses the self-signed keystore referenced by `keystore.properties` (both gitignored). On a fresh clone, regenerate them:

```
keytool -genkeypair -v -keystore release.keystore -alias tina -keyalg RSA -keysize 2048 -validity 10000 -storepass tina-local-release -keypass tina-local-release -dname "CN=tina"
```

```
keystore.properties:
storeFile=release.keystore
storePassword=tina-local-release
keyAlias=tina
keyPassword=tina-local-release
```

Note: sideload updates must be signed with the same keystore, so back `release.keystore` up if you distribute anything.
