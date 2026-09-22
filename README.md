# Nudge

Personal Android app: alarm-style reminders for meals and water (rings and vibrates until you dismiss), plus an ingredient-based meal builder with calories and macros.

## Build (WSL2 / Linux, no Android Studio)

Requirements: JDK 17, Android SDK with `platforms;android-35` and `build-tools;35.0.0` (see `docs/superpowers/plans/2026-09-21-nudge-android-app.md`, Task 1).

On this machine the JDK is a sudo-free Temurin tarball install at `~/.jdks/temurin-17` (there is no system JDK on `PATH`), so `JAVA_HOME` must be passed explicitly on every Gradle invocation:

```bash
cp local.properties.example local.properties   # then set sdk.dir and, optionally, USDA_API_KEY
JAVA_HOME=~/.jdks/temurin-17 ./gradlew assembleDebug
JAVA_HOME=~/.jdks/temurin-17 ./gradlew testDebugUnitTest
```

`local.properties` (not committed) must point at the SDK, e.g. `sdk.dir=/home/vinia/Android/Sdk`.

APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Install on a phone from WSL2

Enable Developer options → Wireless debugging, then:

```bash
adb pair <ip>:<pairing-port>
adb connect <ip>:<port>
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## USDA FoodData Central

Ingredient search works offline with a bundled table. To also search USDA, get a free key at https://fdc.nal.usda.gov/api-key-signup and put `USDA_API_KEY=...` in `local.properties` (never committed).

## Permissions it asks for and why

- Notifications — reminders are alarm notifications.
- Full-screen intent (Android 14+) — to show the ringing screen over the lock screen.
- Exact alarms (`USE_EXACT_ALARM`) — granted automatically to alarm apps; reminders fire on time even in Doze.

## Architecture

Kotlin + Jetpack Compose (Material 3, custom Manrope theme). SharedPreferences + `org.json` for storage. `AlarmManager.setAlarmClock` one-shot alarms, self-rescheduled by `AlarmReceiver`; `AlarmRingingService` (foreground) owns sound/vibration and posts the full-screen notification that opens `AlarmActivity`.
