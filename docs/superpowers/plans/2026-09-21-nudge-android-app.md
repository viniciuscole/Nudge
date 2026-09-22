# Nudge — Meal & Water Reminders Android App Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build "Nudge", a personal native Android app that rings like an alarm clock (looping sound + vibration until dismissed) for daily meal times and a hydration window, plus an ingredient-based meal builder with calorie/macro totals, matching the Claude Design mockup.

**Architecture:** Single-module Kotlin app, Jetpack Compose UI with a custom Material 3 theme built from the design tokens (Manrope, coral/teal palette, light + dark). Reminders are persisted as JSON in SharedPreferences and scheduled one-shot with `AlarmManager.setAlarmClock`; every fire self-reschedules the next occurrence from a `BroadcastReceiver`, which starts a foreground `AlarmRingingService` that owns the sound/vibration and posts a full-screen-intent notification that launches `AlarmActivity` over the lock screen. The meal builder searches a bundled food table plus USDA FoodData Central (key from `local.properties` → `BuildConfig`) and saves meals as JSON.

**Tech Stack:** Kotlin 2.1, Gradle Kotlin DSL + version catalog, AGP 8.10, compileSdk/targetSdk 35, minSdk 26, Jetpack Compose (BOM 2025.06) + Material 3, Navigation Compose, Lifecycle/ViewModel, Coroutines, `org.json` (platform), `HttpURLConnection`. No Room, no Gson, no Hilt, no OkHttp. JUnit 4 for pure-logic unit tests only.

**Spec:** Claude Design project `04436794-da0c-4367-ab04-827f68e1ff07`, file `Meal & Water Reminders.dc.html` (screens 1a home/add/ring, 1b lock-screen alarm, 1c dark home, 1d empty state, 2a/2b meal builder). Design tokens and copy are transcribed into this plan; the user's written spec (Portuguese) is summarized in **Global Constraints**.

## Global Constraints

- `minSdk = 26`, `targetSdk = 35`, `compileSdk = 35`. JDK 17.
- `applicationId` / `namespace` = `dev.viniciuscole.nudge`. App name: **Nudge**.
- Persistence: `SharedPreferences` + `org.json` only. No Room, no Gson, no kotlinx-serialization.
- Scheduling: `AlarmManager.setAlarmClock` (exact, Doze-exempt), one-shot, self-rescheduled on every fire. Never `setRepeating`.
- Ringing: looping `MediaPlayer` on `USAGE_ALARM` + repeating `Vibrator` waveform, owned by a **foreground service**, not by the Activity. Stops only on Done / Snooze 5 min (auto-snooze after 10 min as a safety net).
- Manifest permissions: `POST_NOTIFICATIONS`, `USE_EXACT_ALARM`, `USE_FULL_SCREEN_INTENT`, `RECEIVE_BOOT_COMPLETED`, `VIBRATE`, `WAKE_LOCK`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `INTERNET`.
- USDA key: `USDA_API_KEY=` in `local.properties` → `BuildConfig.USDA_API_KEY`. Never hardcoded. Builder must work with an empty key (bundled food table).
- Copy: English default (`values/strings.xml`, matches design) + Brazilian Portuguese (`values-pt-rBR/strings.xml`). No hardcoded user-visible strings in Kotlin.
- Font: Manrope (variable TTF bundled in `res/font/manrope.ttf`). Weights used: 500, 600, 700, 800.
- Design tokens (light): bg `#FBF7F4`, card `#FFFFFF`, ink `#241E1B`, body `#6B5C55`, muted `#8A7A73`, faint `#A2938B`, line `#E4DAD3`, chip `#F1E8E1`, field `#F7F0EA`, coral `#E9704B`, coralDeep `#C1543A`, coralContainer `#FDE9E2`, coralOnContainer `#8E3A26`, coralMuted `#B4705C`, teal `#2E8F92`, tealDeep `#24736F`, tealContainer `#DDF0EF`, tealOnContainer `#1C5D5E`, tealMuted `#4E8A89`, carbs `#F0B27A`, carbsContainer `#FBEEDD`, carbsOnContainer `#A9702F`, toggleOff `#E4DAD3`, toggleKnobOff `#B3A49C`.
- Design tokens (dark): bg `#151210`, card `#211C19`, ink `#F4EDE8`, body `#C9BDB6`, muted `#9A8B84`, faint `#7E716A`, line `#2E2723`, chip/field `#2A2320`, coralDeep `#F0A88F`, coralContainer `#33170F`, coralOnContainer `#FBE4DA`, coralMuted `#C08D7C`, teal/toggleOn `#3ABDB8`, tealDeep `#7FC6C4`, tealContainer `#12302F`, tealOnContainer `#D9F2F0`, tealMuted `#8FB8B7`, toggleKnobOn `#062E2D`, toggleOff `#2E2723`, toggleKnobOff `#6E625C`.
- Ring screens (theme-independent): meal bg `#2A140E`, meal icon `#FFD2C2`, water bg `#1C5D5E`, white text.
- Commits: plain `git commit` — never pass `--author`, `-c user.name` or `-c user.email`. Conventional-commit prefixes (`feat:`, `chore:`, `test:`).
- Build gate for every task: `./gradlew assembleDebug` succeeds (and `./gradlew testDebugUnitTest` where the task adds tests).

---

## File Structure

```
nudge/
├── settings.gradle.kts, build.gradle.kts, gradle.properties, gradlew, gradle/wrapper/*, gradle/libs.versions.toml
├── local.properties            (gitignored: sdk.dir, USDA_API_KEY)
├── local.properties.example    (committed template)
├── .gitignore, README.md
└── app/
    ├── build.gradle.kts
    └── src/
        ├── main/AndroidManifest.xml
        ├── main/res/
        │   ├── font/manrope.ttf
        │   ├── drawable/ic_fork.xml, ic_drop.xml, ic_plus.xml, ic_back.xml, ic_search.xml, ic_close.xml, ic_launcher_foreground.xml
        │   ├── mipmap-anydpi-v26/ic_launcher.xml
        │   ├── values/strings.xml, themes.xml, ic_launcher_background.xml
        │   ├── values-night/themes.xml
        │   └── values-pt-rBR/strings.xml
        ├── main/java/dev/viniciuscole/nudge/
        │   ├── NudgeApp.kt                 Application: owns repositories/scheduler (service locator), creates channels
        │   ├── MainActivity.kt             Compose host + NavHost
        │   ├── data/model/Reminder.kt      Reminder + ReminderType
        │   ├── data/model/Meal.kt          Ingredient, SavedMeal, MealTotals, Nutrition (math)
        │   ├── data/model/DayStats.kt
        │   ├── data/json/ReminderJson.kt   org.json codec
        │   ├── data/json/MealJson.kt
        │   ├── data/ReminderRepository.kt  SharedPreferences + StateFlow
        │   ├── data/MealRepository.kt
        │   ├── data/DayStatsRepository.kt
        │   ├── data/food/FoodItem.kt, LocalFoods.kt, UsdaClient.kt, FoodSearch.kt
        │   ├── alarm/NextFire.kt           pure next-occurrence math
        │   ├── alarm/AlarmScheduler.kt     AlarmManager wrapper
        │   ├── alarm/AlarmReceiver.kt      fire → reschedule → start service
        │   ├── alarm/BootReceiver.kt
        │   ├── alarm/Notifications.kt      channel + ringing notification
        │   ├── alarm/Ringer.kt             MediaPlayer + Vibrator
        │   ├── alarm/RingingState.kt       in-process StateFlow<Long?>
        │   ├── alarm/AlarmRingingService.kt
        │   ├── alarm/AlarmActivity.kt      lock-screen host for RingingScreen
        │   ├── permissions/Permissions.kt
        │   ├── ui/theme/Color.kt, Type.kt, Theme.kt
        │   ├── ui/components/Primitives.kt NudgeSwitch, PillButton, IconTile, SectionLabel, NudgeChip, CircleIconButton, CircleTextButton, SegmentedPill, Card24
        │   ├── ui/format/ReminderFormat.kt schedule/interval/time strings
        │   ├── ui/NudgeNavHost.kt
        │   ├── ui/home/HomeScreen.kt, HomeViewModel.kt
        │   ├── ui/add/AddReminderScreen.kt, AddReminderViewModel.kt
        │   ├── ui/ring/RingingScreen.kt
        │   └── ui/builder/MealBuilderScreen.kt, MealBuilderViewModel.kt
        └── test/java/dev/viniciuscole/nudge/
            ├── data/json/ReminderJsonTest.kt
            ├── alarm/NextFireTest.kt
            ├── data/model/NutritionTest.kt
            └── data/food/UsdaClientTest.kt
```

Tasks 1–9 ship a complete, usable reminders app. Tasks 10–12 add the meal builder. Task 13 finishes localization, icon, README. Tasks 5, 6 and 7 must be executed back-to-back: the first `assembleDebug` after Task 4 happens at the end of Task 7 (they reference each other's classes).

---

### Task 1: Toolchain + project scaffold (builds "Hello")

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`, `gradlew`, `gradle/wrapper/gradle-wrapper.jar`, `.gitignore`, `local.properties`
- Create: `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/src/main/res/values/strings.xml`, `app/src/main/res/values/themes.xml`, `app/src/main/res/values-night/themes.xml`, `app/src/main/java/dev/viniciuscole/nudge/MainActivity.kt`, `app/src/main/java/dev/viniciuscole/nudge/NudgeApp.kt`

**Interfaces:**
- Produces: `NudgeApp.from(context): NudgeApp` (service locator; later tasks add `val` members to it), `BuildConfig.USDA_API_KEY: String`, version-catalog aliases `libs.*` used by all later tasks.

- [ ] **Step 1: Install JDK 17 and Android command-line tools (WSL2, no Android Studio)**

```bash
sudo apt-get update && sudo apt-get install -y openjdk-17-jdk-headless unzip wget
mkdir -p ~/Android/Sdk/cmdline-tools && cd ~/Android/Sdk/cmdline-tools
wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O ct.zip
unzip -q ct.zip && mv cmdline-tools latest && rm ct.zip
cat >> ~/.bashrc <<'EOF'
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools
EOF
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools
yes | sdkmanager --licenses >/dev/null
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"
```

If the zip URL 404s, take the current "Command line tools only" Linux link from developer.android.com/studio and keep the rest identical.

- [ ] **Step 2: Verify toolchain**

Run: `java -version && sdkmanager --list_installed`
Expected: `openjdk version "17..."` and a table listing `build-tools;35.0.0`, `platform-tools`, `platforms;android-35`.

- [ ] **Step 3: Gradle wrapper (no local Gradle install needed)**

```bash
cd /home/vinia/vinicius/nudge
mkdir -p gradle/wrapper
wget -q https://raw.githubusercontent.com/gradle/gradle/v8.13.0/gradle/wrapper/gradle-wrapper.jar -O gradle/wrapper/gradle-wrapper.jar
wget -q https://raw.githubusercontent.com/gradle/gradle/v8.13.0/gradlew -O gradlew
chmod +x gradlew
cat > gradle/wrapper/gradle-wrapper.properties <<'EOF'
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.13-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
EOF
```

- [ ] **Step 4: Root build files**

`settings.gradle.kts`:
```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "Nudge"
include(":app")
```

`build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
```

`gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx3g -Dfile.encoding=UTF-8
org.gradle.caching=true
org.gradle.configuration-cache=true
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

`gradle/libs.versions.toml`:
```toml
[versions]
agp = "8.10.1"
kotlin = "2.1.21"
coreKtx = "1.16.0"
lifecycle = "2.9.1"
activityCompose = "1.10.1"
composeBom = "2025.06.00"
navigationCompose = "2.9.0"
coroutines = "1.10.2"
junit = "4.13.2"
orgJson = "20250107"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
org-json = { group = "org.json", name = "json", version.ref = "orgJson" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

If Gradle reports a version that does not exist, bump that one entry to the latest stable shown at maven.google.com — do not change the rest.

- [ ] **Step 5: `.gitignore` and `local.properties`**

`.gitignore`:
```
*.iml
.gradle/
/local.properties
/.idea/
/build/
/app/build/
/captures/
.externalNativeBuild/
.cxx/
*.apk
*.aab
.kotlin/
```

`local.properties` (not committed):
```properties
sdk.dir=/home/vinia/Android/Sdk
USDA_API_KEY=
```

- [ ] **Step 6: App module**

`app/build.gradle.kts`:
```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "dev.viniciuscole.nudge"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.viniciuscole.nudge"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("String", "USDA_API_KEY", "\"${localProps.getProperty("USDA_API_KEY", "")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions.unitTests.isReturnDefaultValues = true
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.org.json)
}
```

`app/src/main/AndroidManifest.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:name=".NudgeApp"
        android:allowBackup="true"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.Nudge">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

`app/src/main/res/values/strings.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Nudge</string>
</resources>
```

`app/src/main/res/values/themes.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.Nudge" parent="android:Theme.Material.Light.NoActionBar">
        <item name="android:windowBackground">#FBF7F4</item>
        <item name="android:statusBarColor">@android:color/transparent</item>
        <item name="android:navigationBarColor">@android:color/transparent</item>
    </style>
</resources>
```

`app/src/main/res/values-night/themes.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.Nudge" parent="android:Theme.Material.NoActionBar">
        <item name="android:windowBackground">#151210</item>
        <item name="android:statusBarColor">@android:color/transparent</item>
        <item name="android:navigationBarColor">@android:color/transparent</item>
    </style>
</resources>
```

`app/src/main/java/dev/viniciuscole/nudge/NudgeApp.kt`:
```kotlin
package dev.viniciuscole.nudge

import android.app.Application
import android.content.Context

class NudgeApp : Application() {

    companion object {
        fun from(context: Context): NudgeApp = context.applicationContext as NudgeApp
    }
}
```

`app/src/main/java/dev/viniciuscole/nudge/MainActivity.kt`:
```kotlin
package dev.viniciuscole.nudge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { Text("Nudge") }
    }
}
```

- [ ] **Step 7: Build**

Run: `cd /home/vinia/vinicius/nudge && ./gradlew assembleDebug --no-daemon` (first run downloads Gradle + dependencies; allow 10 minutes)
Expected: `BUILD SUCCESSFUL` and `app/build/outputs/apk/debug/app-debug.apk` exists.

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "chore: scaffold Nudge Android app (Kotlin, Compose, AGP 8.10)"
```

---

### Task 2: Design system — colors, Manrope, theme, primitives, icons

**Files:**
- Create: `app/src/main/res/font/manrope.ttf`
- Create: `app/src/main/res/drawable/ic_fork.xml`, `ic_drop.xml`, `ic_plus.xml`, `ic_back.xml`, `ic_search.xml`, `ic_close.xml`
- Create: `app/src/main/java/dev/viniciuscole/nudge/ui/theme/Color.kt`, `Type.kt`, `Theme.kt`
- Create: `app/src/main/java/dev/viniciuscole/nudge/ui/components/Primitives.kt`
- Modify: `app/src/main/java/dev/viniciuscole/nudge/MainActivity.kt`

**Interfaces:**
- Produces: `NudgeTheme { }`, `NudgeTheme.colors: NudgeColors`, `RingColors`, `manrope(size, weight, lineHeight?, letterSpacing?): TextStyle`, `CapsLabel: TextStyle`, composables `NudgeSwitch(checked, onCheckedChange)`, `PillButton(text, onClick, color, contentColor, height, outlined, elevated)`, `IconTile(iconRes, bg, tint, size, radius, iconSize)`, `SectionLabel(text)`, `NudgeChip(text, selected, onClick, selectedColor)`, `CircleIconButton(iconRes, onClick, size, bg, tint, iconSize)`, `CircleTextButton(text, onClick, size, fontSize)`, `SegmentedPill(options, selectedIndex, onSelect, colors)`, `Card24(modifier, radius) { }`.

- [ ] **Step 1: Bundle Manrope (OFL licensed)**

```bash
mkdir -p app/src/main/res/font
wget -q "https://github.com/google/fonts/raw/main/ofl/manrope/Manrope%5Bwght%5D.ttf" -O app/src/main/res/font/manrope.ttf
ls -la app/src/main/res/font/manrope.ttf
```
Expected: a file of roughly 100–200 KB.

- [ ] **Step 2: Vector icons (stroke paths transcribed from the design SVGs, 24×24 viewBox)**

`app/src/main/res/drawable/ic_fork.xml`:
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:pathData="M7 3v7a2.5 2.5 0 0 0 5 0V3" android:strokeColor="#FFFFFF" android:strokeWidth="1.6" android:strokeLineCap="round" android:fillColor="#00000000"/>
    <path android:pathData="M9.5 10v11" android:strokeColor="#FFFFFF" android:strokeWidth="1.6" android:strokeLineCap="round" android:fillColor="#00000000"/>
    <path android:pathData="M16.5 3v18" android:strokeColor="#FFFFFF" android:strokeWidth="1.6" android:strokeLineCap="round" android:fillColor="#00000000"/>
    <path android:pathData="M16.5 3c1.8 0 2.6 1.4 2.6 3.4S18.3 10 16.5 10" android:strokeColor="#FFFFFF" android:strokeWidth="1.6" android:strokeLineCap="round" android:fillColor="#00000000"/>
</vector>
```

`ic_drop.xml`:
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:pathData="M12 3.2c0 0-6.6 7.1-6.6 11.2a6.6 6.6 0 0 0 13.2 0C18.6 10.3 12 3.2 12 3.2z" android:strokeColor="#FFFFFF" android:strokeWidth="1.6" android:strokeLineCap="round" android:strokeLineJoin="round" android:fillColor="#00000000"/>
    <path android:pathData="M9 15.4a3 3 0 0 0 2.2 2.8" android:strokeColor="#FFFFFF" android:strokeWidth="1.6" android:strokeLineCap="round" android:fillColor="#00000000"/>
</vector>
```

`ic_plus.xml`:
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:pathData="M12 5v14M5 12h14" android:strokeColor="#FFFFFF" android:strokeWidth="2.2" android:strokeLineCap="round" android:fillColor="#00000000"/>
</vector>
```

`ic_back.xml`:
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:pathData="M15 5l-7 7 7 7" android:strokeColor="#FFFFFF" android:strokeWidth="1.8" android:strokeLineCap="round" android:strokeLineJoin="round" android:fillColor="#00000000"/>
</vector>
```

`ic_search.xml`:
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:pathData="M11 4.5a6.5 6.5 0 1 0 0 13a6.5 6.5 0 1 0 0-13z" android:strokeColor="#FFFFFF" android:strokeWidth="1.8" android:fillColor="#00000000"/>
    <path android:pathData="M16 16l4 4" android:strokeColor="#FFFFFF" android:strokeWidth="1.8" android:strokeLineCap="round" android:fillColor="#00000000"/>
</vector>
```

`ic_close.xml`:
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:pathData="M6 6l12 12M18 6L6 18" android:strokeColor="#FFFFFF" android:strokeWidth="2.2" android:strokeLineCap="round" android:fillColor="#00000000"/>
</vector>
```

All icons are white and get tinted at the call site via `Icon(..., tint = ...)`.

- [ ] **Step 3: Colors**

`app/src/main/java/dev/viniciuscole/nudge/ui/theme/Color.kt`:
```kotlin
package dev.viniciuscole.nudge.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class NudgeColors(
    val bg: Color,
    val card: Color,
    val ink: Color,
    val body: Color,
    val muted: Color,
    val faint: Color,
    val line: Color,
    val chip: Color,
    val field: Color,
    val coral: Color,
    val coralDeep: Color,
    val coralContainer: Color,
    val coralOnContainer: Color,
    val coralMuted: Color,
    val teal: Color,
    val tealDeep: Color,
    val tealContainer: Color,
    val tealOnContainer: Color,
    val tealMuted: Color,
    val carbs: Color,
    val carbsContainer: Color,
    val carbsOnContainer: Color,
    val toggleOn: Color,
    val toggleKnobOn: Color,
    val toggleOff: Color,
    val toggleKnobOff: Color,
    val isDark: Boolean,
)

val LightNudgeColors = NudgeColors(
    bg = Color(0xFFFBF7F4),
    card = Color(0xFFFFFFFF),
    ink = Color(0xFF241E1B),
    body = Color(0xFF6B5C55),
    muted = Color(0xFF8A7A73),
    faint = Color(0xFFA2938B),
    line = Color(0xFFE4DAD3),
    chip = Color(0xFFF1E8E1),
    field = Color(0xFFF7F0EA),
    coral = Color(0xFFE9704B),
    coralDeep = Color(0xFFC1543A),
    coralContainer = Color(0xFFFDE9E2),
    coralOnContainer = Color(0xFF8E3A26),
    coralMuted = Color(0xFFB4705C),
    teal = Color(0xFF2E8F92),
    tealDeep = Color(0xFF24736F),
    tealContainer = Color(0xFFDDF0EF),
    tealOnContainer = Color(0xFF1C5D5E),
    tealMuted = Color(0xFF4E8A89),
    carbs = Color(0xFFF0B27A),
    carbsContainer = Color(0xFFFBEEDD),
    carbsOnContainer = Color(0xFFA9702F),
    toggleOn = Color(0xFF2E8F92),
    toggleKnobOn = Color(0xFFFFFFFF),
    toggleOff = Color(0xFFE4DAD3),
    toggleKnobOff = Color(0xFFB3A49C),
    isDark = false,
)

val DarkNudgeColors = NudgeColors(
    bg = Color(0xFF151210),
    card = Color(0xFF211C19),
    ink = Color(0xFFF4EDE8),
    body = Color(0xFFC9BDB6),
    muted = Color(0xFF9A8B84),
    faint = Color(0xFF7E716A),
    line = Color(0xFF2E2723),
    chip = Color(0xFF2A2320),
    field = Color(0xFF2A2320),
    coral = Color(0xFFE9704B),
    coralDeep = Color(0xFFF0A88F),
    coralContainer = Color(0xFF33170F),
    coralOnContainer = Color(0xFFFBE4DA),
    coralMuted = Color(0xFFC08D7C),
    teal = Color(0xFF3ABDB8),
    tealDeep = Color(0xFF7FC6C4),
    tealContainer = Color(0xFF12302F),
    tealOnContainer = Color(0xFFD9F2F0),
    tealMuted = Color(0xFF8FB8B7),
    carbs = Color(0xFFF0B27A),
    carbsContainer = Color(0xFF3A2A16),
    carbsOnContainer = Color(0xFFF0C48F),
    toggleOn = Color(0xFF3ABDB8),
    toggleKnobOn = Color(0xFF062E2D),
    toggleOff = Color(0xFF2E2723),
    toggleKnobOff = Color(0xFF6E625C),
    isDark = true,
)

object RingColors {
    val mealBg = Color(0xFF2A140E)
    val mealIcon = Color(0xFFFFD2C2)
    val mealBars = Color(0xFFFFAA8C)
    val waterBg = Color(0xFF1C5D5E)
    val coral = Color(0xFFE9704B)
}

val LocalNudgeColors = staticCompositionLocalOf { LightNudgeColors }
```

- [ ] **Step 4: Typography**

`app/src/main/java/dev/viniciuscole/nudge/ui/theme/Type.kt`:
```kotlin
package dev.viniciuscole.nudge.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import dev.viniciuscole.nudge.R

val Manrope = FontFamily(
    Font(R.font.manrope, FontWeight.Normal),
    Font(R.font.manrope, FontWeight.Medium),
    Font(R.font.manrope, FontWeight.SemiBold),
    Font(R.font.manrope, FontWeight.Bold),
    Font(R.font.manrope, FontWeight.ExtraBold),
)

fun manrope(
    size: TextUnit,
    weight: FontWeight,
    lineHeight: TextUnit = TextUnit.Unspecified,
    letterSpacing: TextUnit = TextUnit.Unspecified,
): TextStyle = TextStyle(
    fontFamily = Manrope,
    fontSize = size,
    fontWeight = weight,
    lineHeight = lineHeight,
    letterSpacing = letterSpacing,
)

private val base = Typography()

val NudgeTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = Manrope),
    headlineSmall = base.headlineSmall.copy(fontFamily = Manrope),
    titleLarge = base.titleLarge.copy(fontFamily = Manrope),
    titleMedium = base.titleMedium.copy(fontFamily = Manrope),
    bodyLarge = base.bodyLarge.copy(fontFamily = Manrope),
    bodyMedium = base.bodyMedium.copy(fontFamily = Manrope),
    labelLarge = base.labelLarge.copy(fontFamily = Manrope, fontWeight = FontWeight.Bold),
    labelMedium = base.labelMedium.copy(fontFamily = Manrope),
)

val CapsLabel = manrope(12.5.sp, FontWeight.SemiBold, letterSpacing = 0.6.sp)
```

`Font(resId, weight)` on a variable font sets the `wght` axis from `weight` automatically (Compose default `variationSettings`), so one TTF serves all weights.

- [ ] **Step 5: Theme**

`app/src/main/java/dev/viniciuscole/nudge/ui/theme/Theme.kt`:
```kotlin
package dev.viniciuscole.nudge.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

@Composable
fun NudgeTheme(
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val c = if (dark) DarkNudgeColors else LightNudgeColors
    val scheme = if (dark) {
        darkColorScheme(
            primary = c.coral, onPrimary = c.card, secondary = c.teal,
            background = c.bg, onBackground = c.ink, surface = c.card, onSurface = c.ink,
            surfaceVariant = c.chip, onSurfaceVariant = c.muted, outline = c.line,
            primaryContainer = c.coralContainer, onPrimaryContainer = c.coralOnContainer,
            secondaryContainer = c.tealContainer, onSecondaryContainer = c.tealOnContainer,
        )
    } else {
        lightColorScheme(
            primary = c.coral, onPrimary = c.card, secondary = c.teal,
            background = c.bg, onBackground = c.ink, surface = c.card, onSurface = c.ink,
            surfaceVariant = c.chip, onSurfaceVariant = c.muted, outline = c.line,
            primaryContainer = c.coralContainer, onPrimaryContainer = c.coralOnContainer,
            secondaryContainer = c.tealContainer, onSecondaryContainer = c.tealOnContainer,
        )
    }
    CompositionLocalProvider(LocalNudgeColors provides c) {
        MaterialTheme(colorScheme = scheme, typography = NudgeTypography, content = content)
    }
}

object NudgeTheme {
    val colors: NudgeColors
        @Composable @ReadOnlyComposable get() = LocalNudgeColors.current
}
```

- [ ] **Step 6: Primitives (custom switch, pill button, tiles, chips, segmented control)**

`app/src/main/java/dev/viniciuscole/nudge/ui/components/Primitives.kt`:
```kotlin
package dev.viniciuscole.nudge.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.viniciuscole.nudge.ui.theme.CapsLabel
import dev.viniciuscole.nudge.ui.theme.NudgeTheme
import dev.viniciuscole.nudge.ui.theme.manrope

@Composable
fun NudgeSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val c = NudgeTheme.colors
    val track by animateColorAsState(if (checked) c.toggleOn else c.toggleOff, label = "track")
    val knobSize by animateDpAsState(if (checked) 24.dp else 16.dp, label = "knob")
    val knobOffset by animateDpAsState(if (checked) 24.dp else 8.dp, label = "offset")
    Box(
        modifier
            .size(width = 52.dp, height = 32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(track)
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .offset(x = knobOffset)
                .size(knobSize)
                .clip(CircleShape)
                .background(if (checked) c.toggleKnobOn else c.toggleKnobOff),
        )
    }
}

@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = NudgeTheme.colors.coral,
    contentColor: Color = Color.White,
    height: Dp = 58.dp,
    outlined: Boolean = false,
    elevated: Boolean = true,
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .then(if (elevated && !outlined) Modifier.shadow(8.dp, shape, spotColor = color.copy(alpha = .45f), ambientColor = color.copy(alpha = .3f)) else Modifier)
            .clip(shape)
            .then(if (outlined) Modifier.border(2.dp, contentColor.copy(alpha = .6f), shape) else Modifier.background(color))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = manrope(17.sp, FontWeight.Bold), color = contentColor)
    }
}

@Composable
fun IconTile(iconRes: Int, bg: Color, tint: Color, size: Dp = 48.dp, radius: Dp = 16.dp, iconSize: Dp = 24.dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(radius)).background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(iconRes), contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = CapsLabel, color = NudgeTheme.colors.faint, modifier = modifier)
}

@Composable
fun NudgeChip(text: String, selected: Boolean, onClick: () -> Unit, selectedColor: Color = NudgeTheme.colors.teal) {
    val c = NudgeTheme.colors
    val shape = RoundedCornerShape(999.dp)
    Box(
        Modifier
            .clip(shape)
            .then(if (selected) Modifier.background(selectedColor) else Modifier.border(1.5.dp, c.line, shape))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
    ) {
        Text(
            text,
            style = manrope(13.5.sp, if (selected) FontWeight.Bold else FontWeight.SemiBold),
            color = if (selected) Color.White else c.body,
        )
    }
}

@Composable
fun CircleIconButton(
    iconRes: Int,
    onClick: () -> Unit,
    size: Dp = 44.dp,
    bg: Color = Color.Transparent,
    tint: Color = NudgeTheme.colors.ink,
    iconSize: Dp = 22.dp,
    contentDescription: String? = null,
) {
    Box(
        Modifier.size(size).clip(CircleShape).background(bg).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(iconRes), contentDescription, tint = tint, modifier = Modifier.size(iconSize))
    }
}

@Composable
fun CircleTextButton(text: String, onClick: () -> Unit, size: Dp = 28.dp, fontSize: Int = 15) {
    val c = NudgeTheme.colors
    Box(
        Modifier.size(size).clip(CircleShape).background(c.field).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = manrope(fontSize.sp, FontWeight.Bold), color = c.muted)
    }
}

@Composable
fun SegmentedPill(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    colors: List<Color>,
) {
    val c = NudgeTheme.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(999.dp)).background(c.chip).padding(4.dp),
    ) {
        options.forEachIndexed { i, label ->
            val selected = i == selectedIndex
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (selected) colors[i] else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = manrope(14.5.sp, if (selected) FontWeight.Bold else FontWeight.SemiBold),
                    color = if (selected) Color.White else c.muted,
                )
            }
        }
    }
}

@Composable
fun Card24(modifier: Modifier = Modifier, radius: Dp = 24.dp, content: @Composable BoxScope.() -> Unit) {
    val c = NudgeTheme.colors
    val shape = RoundedCornerShape(radius)
    Box(
        modifier
            .shadow(if (c.isDark) 0.dp else 6.dp, shape, ambientColor = c.ink.copy(alpha = .06f), spotColor = c.ink.copy(alpha = .08f))
            .clip(shape)
            .background(c.card),
        content = content,
    )
}
```

- [ ] **Step 7: Themed placeholder in MainActivity**

Replace the `setContent { Text("Nudge") }` line in `MainActivity.onCreate` with:
```kotlin
setContent {
    NudgeTheme {
        Box(Modifier.fillMaxSize().background(NudgeTheme.colors.bg), contentAlignment = Alignment.Center) {
            Text("Nudge", style = manrope(30.sp, FontWeight.ExtraBold), color = NudgeTheme.colors.ink)
        }
    }
}
```
Add imports: `androidx.compose.foundation.background`, `androidx.compose.foundation.layout.Box`, `androidx.compose.foundation.layout.fillMaxSize`, `androidx.compose.ui.Alignment`, `androidx.compose.ui.Modifier`, `androidx.compose.ui.text.font.FontWeight`, `androidx.compose.ui.unit.sp`, `dev.viniciuscole.nudge.ui.theme.NudgeTheme`, `dev.viniciuscole.nudge.ui.theme.manrope`.

- [ ] **Step 8: Build and commit**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`.

```bash
git add -A
git commit -m "feat: design system — Manrope, palette, theme, primitives, icons"
```

---

### Task 3: Reminder model, JSON codec, repository

**Files:**
- Create: `app/src/main/java/dev/viniciuscole/nudge/data/model/Reminder.kt`
- Create: `app/src/main/java/dev/viniciuscole/nudge/data/json/ReminderJson.kt`
- Create: `app/src/main/java/dev/viniciuscole/nudge/data/ReminderRepository.kt`
- Create: `app/src/test/java/dev/viniciuscole/nudge/data/json/ReminderJsonTest.kt`
- Modify: `app/src/main/java/dev/viniciuscole/nudge/NudgeApp.kt`

**Interfaces:**
- Produces:
  - `enum class ReminderType { MEAL, WATER }`
  - `data class Reminder(id: Long, type: ReminderType, label: String, enabled: Boolean = true, insistent: Boolean = true, hour: Int = 12, minute: Int = 30, startHour: Int = 8, endHour: Int = 22, intervalMin: Int = 90)` with `isMeal`, `isWater`
  - `ReminderJson.encode(List<Reminder>): String`, `ReminderJson.decode(String?): List<Reminder>`
  - `ReminderRepository.reminders: StateFlow<List<Reminder>>`, `get(id: Long): Reminder?`, `nextId(): Long`, `upsert(Reminder)`, `delete(id: Long)`, `setEnabled(id: Long, enabled: Boolean)`
  - `NudgeApp.reminders: ReminderRepository`

- [ ] **Step 1: Model**

`app/src/main/java/dev/viniciuscole/nudge/data/model/Reminder.kt`:
```kotlin
package dev.viniciuscole.nudge.data.model

enum class ReminderType { MEAL, WATER }

data class Reminder(
    val id: Long,
    val type: ReminderType,
    val label: String,
    val enabled: Boolean = true,
    val insistent: Boolean = true,
    val hour: Int = 12,
    val minute: Int = 30,
    val startHour: Int = 8,
    val endHour: Int = 22,
    val intervalMin: Int = 90,
) {
    val isMeal: Boolean get() = type == ReminderType.MEAL
    val isWater: Boolean get() = type == ReminderType.WATER
}
```

- [ ] **Step 2: Failing codec test**

`app/src/test/java/dev/viniciuscole/nudge/data/json/ReminderJsonTest.kt`:
```kotlin
package dev.viniciuscole.nudge.data.json

import dev.viniciuscole.nudge.data.model.Reminder
import dev.viniciuscole.nudge.data.model.ReminderType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderJsonTest {

    private val sample = listOf(
        Reminder(1, ReminderType.MEAL, "Breakfast", hour = 8, minute = 0),
        Reminder(2, ReminderType.WATER, "Drink water", enabled = false, insistent = false, startHour = 8, endHour = 22, intervalMin = 90),
    )

    @Test
    fun roundTripPreservesEveryField() {
        val decoded = ReminderJson.decode(ReminderJson.encode(sample))
        assertEquals(sample, decoded)
    }

    @Test
    fun nullOrBlankDecodesToEmpty() {
        assertTrue(ReminderJson.decode(null).isEmpty())
        assertTrue(ReminderJson.decode("  ").isEmpty())
    }

    @Test
    fun missingOptionalFieldsUseDefaults() {
        val decoded = ReminderJson.decode("""[{"id":7,"type":"MEAL","label":"Lunch"}]""")
        assertEquals(Reminder(7, ReminderType.MEAL, "Lunch"), decoded.single())
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "dev.viniciuscole.nudge.data.json.ReminderJsonTest"`
Expected: compilation FAILS with `Unresolved reference: ReminderJson`.

- [ ] **Step 4: Codec**

`app/src/main/java/dev/viniciuscole/nudge/data/json/ReminderJson.kt`:
```kotlin
package dev.viniciuscole.nudge.data.json

import dev.viniciuscole.nudge.data.model.Reminder
import dev.viniciuscole.nudge.data.model.ReminderType
import org.json.JSONArray
import org.json.JSONObject

object ReminderJson {

    fun encode(list: List<Reminder>): String {
        val arr = JSONArray()
        list.forEach { arr.put(toJson(it)) }
        return arr.toString()
    }

    fun decode(raw: String?): List<Reminder> {
        if (raw.isNullOrBlank()) return emptyList()
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
    }

    private fun toJson(r: Reminder): JSONObject = JSONObject()
        .put("id", r.id)
        .put("type", r.type.name)
        .put("label", r.label)
        .put("enabled", r.enabled)
        .put("insistent", r.insistent)
        .put("hour", r.hour)
        .put("minute", r.minute)
        .put("startHour", r.startHour)
        .put("endHour", r.endHour)
        .put("intervalMin", r.intervalMin)

    private fun fromJson(o: JSONObject): Reminder = Reminder(
        id = o.getLong("id"),
        type = ReminderType.valueOf(o.getString("type")),
        label = o.getString("label"),
        enabled = o.optBoolean("enabled", true),
        insistent = o.optBoolean("insistent", true),
        hour = o.optInt("hour", 12),
        minute = o.optInt("minute", 30),
        startHour = o.optInt("startHour", 8),
        endHour = o.optInt("endHour", 22),
        intervalMin = o.optInt("intervalMin", 90),
    )
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "dev.viniciuscole.nudge.data.json.ReminderJsonTest"`
Expected: `BUILD SUCCESSFUL`, 3 tests passed. (JVM tests use the real `org.json:json` artifact added as `testImplementation` in Task 1; the Android stub would return nulls.)

- [ ] **Step 6: Repository**

`app/src/main/java/dev/viniciuscole/nudge/data/ReminderRepository.kt`:
```kotlin
package dev.viniciuscole.nudge.data

import android.content.Context
import dev.viniciuscole.nudge.data.json.ReminderJson
import dev.viniciuscole.nudge.data.model.Reminder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ReminderRepository(context: Context) {

    private val prefs = context.getSharedPreferences("reminders", Context.MODE_PRIVATE)
    private val _reminders = MutableStateFlow(ReminderJson.decode(prefs.getString(KEY, null)))
    val reminders: StateFlow<List<Reminder>> = _reminders.asStateFlow()

    fun get(id: Long): Reminder? = _reminders.value.firstOrNull { it.id == id }

    fun nextId(): Long = (_reminders.value.maxOfOrNull { it.id } ?: 0L) + 1

    fun upsert(r: Reminder) {
        val cur = _reminders.value
        write(if (cur.any { it.id == r.id }) cur.map { if (it.id == r.id) r else it } else cur + r)
    }

    fun delete(id: Long) = write(_reminders.value.filter { it.id != id })

    fun setEnabled(id: Long, enabled: Boolean) =
        write(_reminders.value.map { if (it.id == id) it.copy(enabled = enabled) else it })

    private fun write(list: List<Reminder>) {
        _reminders.value = list
        // commit(), not apply(): the alarm receiver reads this store and must see a durable write
        prefs.edit().putString(KEY, ReminderJson.encode(list)).commit()
    }

    private companion object {
        const val KEY = "list"
    }
}
```

- [ ] **Step 7: Register in NudgeApp**

In `NudgeApp.kt` add inside the class, above `companion object`:
```kotlin
val reminders: ReminderRepository by lazy { ReminderRepository(this) }
```
and the import `dev.viniciuscole.nudge.data.ReminderRepository`.

- [ ] **Step 8: Build and commit**

Run: `./gradlew assembleDebug testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`.

```bash
git add -A
git commit -m "feat: reminder model, JSON codec and SharedPreferences repository"
```

---

### Task 4: Next-fire scheduling math (pure Kotlin)

**Files:**
- Create: `app/src/main/java/dev/viniciuscole/nudge/alarm/NextFire.kt`
- Create: `app/src/test/java/dev/viniciuscole/nudge/alarm/NextFireTest.kt`

**Interfaces:**
- Consumes: `Reminder`, `ReminderType` (Task 3).
- Produces: `NextFire.compute(r: Reminder, now: LocalDateTime): LocalDateTime`, `NextFire.slotsOn(r: Reminder, day: LocalDate): List<LocalDateTime>`, `NextFire.slotCount(r: Reminder): Int`, `NextFire.toEpochMillis(t: LocalDateTime, zone: ZoneId = ZoneId.systemDefault()): Long`.

- [ ] **Step 1: Failing tests**

`app/src/test/java/dev/viniciuscole/nudge/alarm/NextFireTest.kt`:
```kotlin
package dev.viniciuscole.nudge.alarm

import dev.viniciuscole.nudge.data.model.Reminder
import dev.viniciuscole.nudge.data.model.ReminderType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class NextFireTest {

    private val lunch = Reminder(1, ReminderType.MEAL, "Lunch", hour = 12, minute = 30)
    private val water = Reminder(2, ReminderType.WATER, "Water", startHour = 8, endHour = 22, intervalMin = 90)
    private val day = LocalDate.of(2026, 9, 21)

    @Test
    fun mealLaterTodayFiresToday() {
        val now = day.atTime(9, 0)
        assertEquals(day.atTime(12, 30), NextFire.compute(lunch, now))
    }

    @Test
    fun mealAlreadyPassedFiresTomorrow() {
        val now = day.atTime(12, 30)
        assertEquals(day.plusDays(1).atTime(12, 30), NextFire.compute(lunch, now))
    }

    @Test
    fun waterPicksNextSlotInsideWindow() {
        val now = day.atTime(10, 0)
        assertEquals(day.atTime(11, 0), NextFire.compute(water, now))
    }

    @Test
    fun waterAfterWindowFiresAtTomorrowStart() {
        val now = day.atTime(22, 30)
        assertEquals(day.plusDays(1).atTime(8, 0), NextFire.compute(water, now))
    }

    @Test
    fun waterBeforeWindowFiresAtTodayStart() {
        val now = day.atTime(6, 0)
        assertEquals(day.atTime(8, 0), NextFire.compute(water, now))
    }

    @Test
    fun slotCountCoversWholeWindowInclusive() {
        assertEquals(10, NextFire.slotCount(water))
        assertEquals(15, NextFire.slotCount(water.copy(intervalMin = 60)))
    }

    @Test
    fun invertedWindowDegradesToSingleSlot() {
        val broken = water.copy(startHour = 20, endHour = 8)
        assertEquals(listOf(day.atTime(20, 0)), NextFire.slotsOn(broken, day))
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew testDebugUnitTest --tests "dev.viniciuscole.nudge.alarm.NextFireTest"`
Expected: compilation FAILS with `Unresolved reference: NextFire`.

- [ ] **Step 3: Implementation**

`app/src/main/java/dev/viniciuscole/nudge/alarm/NextFire.kt`:
```kotlin
package dev.viniciuscole.nudge.alarm

import dev.viniciuscole.nudge.data.model.Reminder
import dev.viniciuscole.nudge.data.model.ReminderType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object NextFire {

    fun compute(r: Reminder, now: LocalDateTime): LocalDateTime = when (r.type) {
        ReminderType.MEAL -> {
            val today = now.toLocalDate().atTime(r.hour, r.minute)
            if (today.isAfter(now)) today else today.plusDays(1)
        }
        ReminderType.WATER -> {
            val today = now.toLocalDate()
            slotsOn(r, today).firstOrNull { it.isAfter(now) } ?: slotsOn(r, today.plusDays(1)).first()
        }
    }

    fun slotsOn(r: Reminder, day: LocalDate): List<LocalDateTime> {
        val start = day.atTime(r.startHour, 0)
        val end = day.atTime(r.endHour, 0)
        if (end.isBefore(start)) return listOf(start)
        val step = r.intervalMin.coerceAtLeast(5).toLong()
        return generateSequence(start) { it.plusMinutes(step) }
            .takeWhile { !it.isAfter(end) }
            .toList()
    }

    fun slotCount(r: Reminder): Int = slotsOn(r, LocalDate.of(2000, 1, 1)).size

    fun toEpochMillis(t: LocalDateTime, zone: ZoneId = ZoneId.systemDefault()): Long =
        t.atZone(zone).toInstant().toEpochMilli()
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "dev.viniciuscole.nudge.alarm.NextFireTest"`
Expected: `BUILD SUCCESSFUL`, 7 tests passed.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: next-fire computation for meal and water reminders"
```

---

### Task 5: AlarmScheduler, AlarmReceiver, BootReceiver, manifest permissions

**Files:**
- Create: `app/src/main/java/dev/viniciuscole/nudge/alarm/AlarmScheduler.kt`
- Create: `app/src/main/java/dev/viniciuscole/nudge/alarm/AlarmReceiver.kt`
- Create: `app/src/main/java/dev/viniciuscole/nudge/alarm/BootReceiver.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/java/dev/viniciuscole/nudge/NudgeApp.kt`

**Interfaces:**
- Consumes: `NextFire` (Task 4), `ReminderRepository` (Task 3).
- Produces: `AlarmScheduler.schedule(r: Reminder)`, `snooze(id: Long, minutes: Int = 5)`, `cancel(id: Long)`, `rescheduleAll(list: List<Reminder>)`; `AlarmReceiver.ACTION_FIRE`, `AlarmReceiver.EXTRA_ID`; `NudgeApp.scheduler: AlarmScheduler`. `AlarmReceiver` calls `AlarmRingingService.start(context, id)` which Task 6 creates — until then it is a compile error, so **Task 5 and Task 6 are built together** (Task 5 has no build step; Task 6 does).

- [ ] **Step 1: Scheduler**

`app/src/main/java/dev/viniciuscole/nudge/alarm/AlarmScheduler.kt`:
```kotlin
package dev.viniciuscole.nudge.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import dev.viniciuscole.nudge.MainActivity
import dev.viniciuscole.nudge.data.model.Reminder
import java.time.LocalDateTime

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(r: Reminder) {
        cancel(r.id)
        if (!r.enabled) return
        val at = NextFire.toEpochMillis(NextFire.compute(r, LocalDateTime.now()))
        set(at, firePendingIntent(r.id, snooze = false))
    }

    fun snooze(id: Long, minutes: Int = 5) {
        set(System.currentTimeMillis() + minutes * 60_000L, firePendingIntent(id, snooze = true))
    }

    fun cancel(id: Long) {
        alarmManager.cancel(firePendingIntent(id, snooze = false))
        alarmManager.cancel(firePendingIntent(id, snooze = true))
    }

    fun rescheduleAll(list: List<Reminder>) = list.forEach(::schedule)

    private fun set(at: Long, pi: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            return
        }
        alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(at, openAppPendingIntent()), pi)
    }

    private fun firePendingIntent(id: Long, snooze: Boolean): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION_FIRE)
            .putExtra(AlarmReceiver.EXTRA_ID, id)
        val requestCode = (id * 2 + if (snooze) 1 else 0).toInt()
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun openAppPendingIntent(): PendingIntent = PendingIntent.getActivity(
        context, 0, Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
```

Why `setAlarmClock`: it is the API for user-visible alarms — exact, exempt from Doze/App Standby, shows the alarm icon in the status bar. The snooze one-shot uses a separate request code (`id*2+1`) so it never cancels the regular schedule. IDs are small sequential longs (`ReminderRepository.nextId()`), so the `Int` cast is safe.

- [ ] **Step 2: Fire receiver (self-reschedules, then hands off to the service)**

`app/src/main/java/dev/viniciuscole/nudge/alarm/AlarmReceiver.kt`:
```kotlin
package dev.viniciuscole.nudge.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.viniciuscole.nudge.NudgeApp

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE) return
        val id = intent.getLongExtra(EXTRA_ID, -1L)
        if (id < 0) return
        val app = NudgeApp.from(context)
        val reminder = app.reminders.get(id) ?: return
        if (!reminder.enabled) return
        app.scheduler.schedule(reminder)
        AlarmRingingService.start(context, id)
    }

    companion object {
        const val ACTION_FIRE = "dev.viniciuscole.nudge.action.FIRE"
        const val EXTRA_ID = "reminder_id"
    }
}
```

- [ ] **Step 3: Boot / time-change receiver**

`app/src/main/java/dev/viniciuscole/nudge/alarm/BootReceiver.kt`:
```kotlin
package dev.viniciuscole.nudge.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.viniciuscole.nudge.NudgeApp

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            -> {
                val app = NudgeApp.from(context)
                app.scheduler.rescheduleAll(app.reminders.reminders.value)
            }
        }
    }
}
```

- [ ] **Step 4: Register scheduler in NudgeApp**

In `NudgeApp.kt` add below `val reminders ...`:
```kotlin
val scheduler: AlarmScheduler by lazy { AlarmScheduler(this) }
```
and the import `dev.viniciuscole.nudge.alarm.AlarmScheduler`.

- [ ] **Step 5: Manifest — permissions and receivers**

Replace the whole `app/src/main/AndroidManifest.xml` with:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.USE_EXACT_ALARM" />
    <uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    <uses-permission android:name="android.permission.VIBRATE" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:name=".NudgeApp"
        android:allowBackup="true"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.Nudge">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <receiver
            android:name=".alarm.AlarmReceiver"
            android:exported="false" />

        <receiver
            android:name=".alarm.BootReceiver"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
                <action android:name="android.intent.action.MY_PACKAGE_REPLACED" />
                <action android:name="android.intent.action.TIME_SET" />
                <action android:name="android.intent.action.TIMEZONE_CHANGED" />
            </intent-filter>
        </receiver>
    </application>
</manifest>
```

`USE_EXACT_ALARM` (not `SCHEDULE_EXACT_ALARM`) is granted automatically to alarm-clock-type apps and cannot be revoked by the user — right for a personal alarm app that is sideloaded. `BootReceiver` is `exported="true"` because the system delivers those protected broadcasts; only the system can send them.

- [ ] **Step 6: Commit (build happens at the end of Task 6)**

```bash
git add -A
git commit -m "feat: exact alarm scheduling, fire and boot receivers"
```

---

### Task 6: Notification channel, Ringer, foreground AlarmRingingService, DayStats

**Files:**
- Create: `app/src/main/java/dev/viniciuscole/nudge/alarm/Notifications.kt`
- Create: `app/src/main/java/dev/viniciuscole/nudge/alarm/Ringer.kt`
- Create: `app/src/main/java/dev/viniciuscole/nudge/alarm/RingingState.kt`
- Create: `app/src/main/java/dev/viniciuscole/nudge/alarm/AlarmRingingService.kt`
- Create: `app/src/main/java/dev/viniciuscole/nudge/data/model/DayStats.kt`
- Create: `app/src/main/java/dev/viniciuscole/nudge/data/DayStatsRepository.kt`
- Create: `app/src/main/res/drawable/ic_notification.xml`
- Modify: `app/src/main/res/values/strings.xml`, `app/src/main/AndroidManifest.xml`, `app/src/main/java/dev/viniciuscole/nudge/NudgeApp.kt`

**Interfaces:**
- Consumes: `AlarmScheduler` (Task 5), `ReminderRepository` (Task 3).
- Produces: `AlarmRingingService.start(context, id)`, `AlarmRingingService.intent(context, id, action)`, actions `ACTION_RING`, `ACTION_DONE`, `ACTION_SNOOZE`; `RingingState.current: StateFlow<Long?>`; `Notifications.createChannels(context)`, `Notifications.ringing(context, r): Notification`; `DayStats(date, waterDone, mealsDone)`, `DayStatsRepository.stats: StateFlow<DayStats>`, `today(): DayStats`, `recordDone(r: Reminder?)`; `NudgeApp.stats: DayStatsRepository`. `Notifications.ringing` references `AlarmActivity.intent(context, id)` from Task 7 — **build Tasks 5+6+7 together** (first `assembleDebug` at the end of Task 7).

- [ ] **Step 1: Strings used by the service**

Add inside `<resources>` in `app/src/main/res/values/strings.xml`:
```xml
    <string name="channel_alarms">Alarms</string>
    <string name="channel_alarms_desc">Meal and water reminders</string>
    <string name="ring_sub_meal">Ringing · vibrating</string>
    <string name="ring_sub_water">Glass %1$d of %2$d · ringing</string>
    <string name="action_done">Done</string>
    <string name="action_snooze">Snooze 5 min</string>
```

- [ ] **Step 2: Notification small icon (monochrome bell-less fork mark)**

`app/src/main/res/drawable/ic_notification.xml`:
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:pathData="M7 3v7a2.5 2.5 0 0 0 5 0V3" android:strokeColor="#FFFFFF" android:strokeWidth="2" android:strokeLineCap="round" android:fillColor="#00000000"/>
    <path android:pathData="M9.5 10v11" android:strokeColor="#FFFFFF" android:strokeWidth="2" android:strokeLineCap="round" android:fillColor="#00000000"/>
    <path android:pathData="M16.5 3v18" android:strokeColor="#FFFFFF" android:strokeWidth="2" android:strokeLineCap="round" android:fillColor="#00000000"/>
    <path android:pathData="M16.5 3c1.8 0 2.6 1.4 2.6 3.4S18.3 10 16.5 10" android:strokeColor="#FFFFFF" android:strokeWidth="2" android:strokeLineCap="round" android:fillColor="#00000000"/>
</vector>
```

- [ ] **Step 3: DayStats model + repository**

`app/src/main/java/dev/viniciuscole/nudge/data/model/DayStats.kt`:
```kotlin
package dev.viniciuscole.nudge.data.model

import java.time.LocalDate

data class DayStats(
    val date: LocalDate,
    val waterDone: Int = 0,
    val mealsDone: Int = 0,
)
```

`app/src/main/java/dev/viniciuscole/nudge/data/DayStatsRepository.kt`:
```kotlin
package dev.viniciuscole.nudge.data

import android.content.Context
import dev.viniciuscole.nudge.data.model.DayStats
import dev.viniciuscole.nudge.data.model.Reminder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

class DayStatsRepository(context: Context) {

    private val prefs = context.getSharedPreferences("stats", Context.MODE_PRIVATE)
    private val _stats = MutableStateFlow(load())
    val stats: StateFlow<DayStats> = _stats.asStateFlow()

    fun today(): DayStats {
        val s = _stats.value
        return if (s.date == LocalDate.now()) s else DayStats(LocalDate.now())
    }

    fun recordDone(r: Reminder?) {
        r ?: return
        val cur = today()
        write(if (r.isWater) cur.copy(waterDone = cur.waterDone + 1) else cur.copy(mealsDone = cur.mealsDone + 1))
    }

    private fun load(): DayStats {
        val date = prefs.getString("date", null)?.let(LocalDate::parse) ?: LocalDate.now()
        return DayStats(date, prefs.getInt("water", 0), prefs.getInt("meals", 0))
    }

    private fun write(s: DayStats) {
        _stats.value = s
        prefs.edit()
            .putString("date", s.date.toString())
            .putInt("water", s.waterDone)
            .putInt("meals", s.mealsDone)
            .apply()
    }
}
```

- [ ] **Step 4: In-process ringing state (lets AlarmActivity close itself when the service stops)**

`app/src/main/java/dev/viniciuscole/nudge/alarm/RingingState.kt`:
```kotlin
package dev.viniciuscole.nudge.alarm

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object RingingState {
    private val _current = MutableStateFlow<Long?>(null)
    val current: StateFlow<Long?> = _current.asStateFlow()

    fun set(id: Long?) {
        _current.value = id
    }
}
```

- [ ] **Step 5: Notifications**

`app/src/main/java/dev/viniciuscole/nudge/alarm/Notifications.kt`:
```kotlin
package dev.viniciuscole.nudge.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import dev.viniciuscole.nudge.NudgeApp
import dev.viniciuscole.nudge.R
import dev.viniciuscole.nudge.data.model.Reminder

object Notifications {

    const val CHANNEL_ALARMS = "alarms"
    const val RINGING_ID = 1001

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ALARMS,
            context.getString(R.string.channel_alarms),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.channel_alarms_desc)
            // the service plays the alarm tone and vibrates; the channel must stay silent
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannel(channel)
    }

    fun ringing(context: Context, r: Reminder): Notification {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val fullScreen = PendingIntent.getActivity(context, r.id.toInt(), AlarmActivity.intent(context, r.id), flags)
        val done = PendingIntent.getService(context, 1, AlarmRingingService.intent(context, r.id, AlarmRingingService.ACTION_DONE), flags)
        val snooze = PendingIntent.getService(context, 2, AlarmRingingService.intent(context, r.id, AlarmRingingService.ACTION_SNOOZE), flags)

        return NotificationCompat.Builder(context, CHANNEL_ALARMS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(r.label)
            .setContentText(subtitle(context, r))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(fullScreen)
            .setFullScreenIntent(fullScreen, true)
            .addAction(0, context.getString(R.string.action_done), done)
            .addAction(0, context.getString(R.string.action_snooze), snooze)
            .build()
    }

    fun subtitle(context: Context, r: Reminder): String {
        if (r.isMeal) return context.getString(R.string.ring_sub_meal)
        val app = NudgeApp.from(context)
        val glass = app.stats.today().waterDone + 1
        val goal = app.reminders.reminders.value.filter { it.enabled && it.isWater }.sumOf { NextFire.slotCount(it) }
        return context.getString(R.string.ring_sub_water, glass, maxOf(goal, glass))
    }
}
```

- [ ] **Step 6: Ringer (looping alarm tone + repeating vibration)**

`app/src/main/java/dev/viniciuscole/nudge/alarm/Ringer.kt`:
```kotlin
package dev.viniciuscole.nudge.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings

class Ringer(private val context: Context) {

    private var player: MediaPlayer? = null

    private val vibrator: Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }

    fun start() {
        if (player != null) return
        player = createPlayer(alarmUri()) ?: createPlayer(Settings.System.DEFAULT_ALARM_ALERT_URI)
        val effect = VibrationEffect.createWaveform(longArrayOf(0, 600, 400, 600, 1200), 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            vibrator.vibrate(effect)
        }
    }

    fun stop() {
        player?.let { p ->
            runCatching { if (p.isPlaying) p.stop() }
            p.release()
        }
        player = null
        vibrator.cancel()
    }

    private fun alarmUri(): Uri =
        RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: Settings.System.DEFAULT_ALARM_ALERT_URI

    private fun createPlayer(uri: Uri): MediaPlayer? = runCatching {
        MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            setDataSource(context, uri)
            isLooping = true
            prepare()
            start()
        }
    }.getOrNull()
}
```

- [ ] **Step 7: Foreground service that owns the ring**

`app/src/main/java/dev/viniciuscole/nudge/alarm/AlarmRingingService.kt`:
```kotlin
package dev.viniciuscole.nudge.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.content.ContextCompat
import dev.viniciuscole.nudge.NudgeApp

class AlarmRingingService : Service() {

    private lateinit var ringer: Ringer
    private val handler = Handler(Looper.getMainLooper())
    private val autoSnooze = Runnable { finish(snooze = true) }
    private var reminderId = -1L

    override fun onCreate() {
        super.onCreate()
        ringer = Ringer(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = NudgeApp.from(this)
        reminderId = intent?.getLongExtra(EXTRA_ID, reminderId) ?: reminderId
        when (intent?.action) {
            ACTION_RING -> {
                val reminder = app.reminders.get(reminderId)
                if (reminder == null) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                val notification = Notifications.ringing(this, reminder)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(Notifications.RINGING_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
                } else {
                    startForeground(Notifications.RINGING_ID, notification)
                }
                RingingState.set(reminderId)
                if (reminder.insistent) ringer.start()
                handler.removeCallbacks(autoSnooze)
                handler.postDelayed(autoSnooze, AUTO_SNOOZE_MS)
            }
            ACTION_DONE -> {
                app.stats.recordDone(app.reminders.get(reminderId))
                finish(snooze = false)
            }
            ACTION_SNOOZE -> finish(snooze = true)
        }
        return START_NOT_STICKY
    }

    private fun finish(snooze: Boolean) {
        handler.removeCallbacks(autoSnooze)
        ringer.stop()
        if (snooze && reminderId >= 0) NudgeApp.from(this).scheduler.snooze(reminderId)
        RingingState.set(null)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        handler.removeCallbacks(autoSnooze)
        ringer.stop()
        RingingState.set(null)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_RING = "dev.viniciuscole.nudge.action.RING"
        const val ACTION_DONE = "dev.viniciuscole.nudge.action.DONE"
        const val ACTION_SNOOZE = "dev.viniciuscole.nudge.action.SNOOZE"
        const val EXTRA_ID = "reminder_id"
        private const val AUTO_SNOOZE_MS = 10 * 60_000L

        fun intent(context: Context, id: Long, action: String): Intent =
            Intent(context, AlarmRingingService::class.java).setAction(action).putExtra(EXTRA_ID, id)

        fun start(context: Context, id: Long) {
            ContextCompat.startForegroundService(context, intent(context, id, ACTION_RING))
        }
    }
}
```

Why a service and not the Activity: when the phone is unlocked and in use, Android shows a heads-up notification instead of launching the full-screen intent — if the Activity owned the sound, the alarm would be silent exactly then. Starting a foreground service from an exact-alarm broadcast is an allowed background-start exemption.

- [ ] **Step 8: Register in NudgeApp and manifest**

`NudgeApp.kt` — add member and channel creation:
```kotlin
val stats: DayStatsRepository by lazy { DayStatsRepository(this) }

override fun onCreate() {
    super.onCreate()
    Notifications.createChannels(this)
}
```
Imports: `dev.viniciuscole.nudge.data.DayStatsRepository`, `dev.viniciuscole.nudge.alarm.Notifications`.

Manifest — add inside `<application>`, after the receivers:
```xml
        <service
            android:name=".alarm.AlarmRingingService"
            android:exported="false"
            android:foregroundServiceType="mediaPlayback" />
```

- [ ] **Step 9: Commit (build happens at the end of Task 7)**

```bash
git add -A
git commit -m "feat: ringing foreground service, notification channel, day stats"
```

---

### Task 7: AlarmActivity + RingingScreen (lock-screen alarm UI, design 1a-ring / 1b)

**Files:**
- Create: `app/src/main/java/dev/viniciuscole/nudge/ui/ring/RingingScreen.kt`
- Create: `app/src/main/java/dev/viniciuscole/nudge/alarm/AlarmActivity.kt`
- Modify: `app/src/main/res/values/strings.xml`, `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes: `RingingState`, `AlarmRingingService.intent/ACTION_DONE/ACTION_SNOOZE` (Task 6), `RingColors`, `manrope`, `PillButton` (Task 2).
- Produces: `AlarmActivity.intent(context, id): Intent`; `RingingScreen(reminder: Reminder, time: String, subtitle: String, onDone: () -> Unit, onSnooze: () -> Unit)`.

- [ ] **Step 1: Strings**

Add to `values/strings.xml`:
```xml
    <string name="reminder">Reminder</string>
```

- [ ] **Step 2: RingingScreen composable**

`app/src/main/java/dev/viniciuscole/nudge/ui/ring/RingingScreen.kt`:
```kotlin
package dev.viniciuscole.nudge.ui.ring

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.viniciuscole.nudge.R
import dev.viniciuscole.nudge.data.model.Reminder
import dev.viniciuscole.nudge.ui.components.PillButton
import dev.viniciuscole.nudge.ui.theme.RingColors
import dev.viniciuscole.nudge.ui.theme.manrope

@Composable
fun RingingScreen(
    reminder: Reminder,
    time: String,
    subtitle: String,
    onDone: () -> Unit,
    onSnooze: () -> Unit,
) {
    val meal = reminder.isMeal
    val bg = if (meal) RingColors.mealBg else RingColors.waterBg
    val accent = if (meal) RingColors.coral else Color.White
    val ringStroke = if (meal) RingColors.coral.copy(alpha = .55f) else Color.White.copy(alpha = .35f)
    val discFill = if (meal) RingColors.coral.copy(alpha = .22f) else Color.White.copy(alpha = .14f)
    val iconTint = if (meal) RingColors.mealIcon else Color.White
    val barColor = if (meal) RingColors.mealBars.copy(alpha = .7f) else Color.White.copy(alpha = .55f)

    Column(
        Modifier
            .fillMaxSize()
            .background(bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(56.dp))
        Text(
            stringResource(R.string.reminder).uppercase(),
            style = manrope(13.sp, FontWeight.SemiBold, letterSpacing = 1.8.sp),
            color = Color.White.copy(alpha = .7f),
        )
        Text(
            time,
            style = manrope(64.sp, FontWeight.ExtraBold, letterSpacing = (-1.9).sp),
            color = Color.White,
            modifier = Modifier.padding(top = 14.dp),
        )

        Spacer(Modifier.height(44.dp))
        PulsingDisc(ringStroke, discFill) {
            Icon(
                painterResource(if (meal) R.drawable.ic_fork else R.drawable.ic_drop),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(64.dp),
            )
        }

        Text(
            reminder.label,
            style = manrope(34.sp, FontWeight.ExtraBold, lineHeight = 41.sp),
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 40.dp),
        )
        Text(
            subtitle,
            style = manrope(15.sp, FontWeight.Medium),
            color = Color.White.copy(alpha = .75f),
            modifier = Modifier.padding(top = 8.dp),
        )

        Spacer(Modifier.height(26.dp))
        Equalizer(barColor)

        Spacer(Modifier.weight(1f))
        Column(Modifier.fillMaxWidth().padding(bottom = 34.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PillButton(
                text = stringResource(R.string.action_done),
                onClick = onDone,
                color = accent,
                contentColor = if (meal) Color.White else RingColors.waterBg,
                height = 62.dp,
                elevated = false,
            )
            PillButton(
                text = stringResource(R.string.action_snooze),
                onClick = onSnooze,
                contentColor = Color.White,
                height = 62.dp,
                outlined = true,
            )
        }
    }
}

@Composable
private fun PulsingDisc(ringStroke: Color, discFill: Color, content: @Composable () -> Unit) {
    val t = rememberInfiniteTransition(label = "disc")
    val pulse by t.animateFloat(
        1f, 1.07f,
        infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse",
    )
    val ring1 by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(2200, easing = LinearOutSlowInEasing)),
        label = "ring1",
    )
    val ring2 by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(2200, easing = LinearOutSlowInEasing), initialStartOffset = StartOffset(1100)),
        label = "ring2",
    )
    Box(Modifier.size(180.dp), contentAlignment = Alignment.Center) {
        listOf(ring1, ring2).forEach { p ->
            Box(
                Modifier
                    .fillMaxSize()
                    .scale(0.9f + 0.65f * p)
                    .alpha(0.55f * (1f - p))
                    .border(2.dp, ringStroke, CircleShape),
            )
        }
        Box(
            Modifier.size(132.dp).scale(pulse).clip(CircleShape).background(discFill),
            contentAlignment = Alignment.Center,
        ) { content() }
    }
}

@Composable
private fun Equalizer(color: Color) {
    val delays = listOf(0, 140, 280, 420, 560, 420, 280, 140)
    val t = rememberInfiniteTransition(label = "eq")
    Row(
        Modifier.height(36.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        delays.forEach { delay ->
            val scaleY by t.animateFloat(
                0.25f, 1f,
                infiniteRepeatable(tween(500, easing = FastOutSlowInEasing), RepeatMode.Reverse, StartOffset(delay)),
                label = "bar$delay",
            )
            Box(
                Modifier
                    .width(5.dp)
                    .height(36.dp)
                    .graphicsLayer {
                        this.scaleY = scaleY
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    }
                    .clip(RoundedCornerShape(3.dp))
                    .background(color),
            )
        }
    }
}
```

- [ ] **Step 3: AlarmActivity**

`app/src/main/java/dev/viniciuscole/nudge/alarm/AlarmActivity.kt`:
```kotlin
package dev.viniciuscole.nudge.alarm

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.viniciuscole.nudge.NudgeApp
import dev.viniciuscole.nudge.ui.ring.RingingScreen
import dev.viniciuscole.nudge.ui.theme.NudgeTheme
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()
        enableEdgeToEdge()

        val id = intent.getLongExtra(EXTRA_ID, -1L)
        val reminder = NudgeApp.from(this).reminders.get(id)
        if (reminder == null) {
            finish()
            return
        }
        val time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        val subtitle = Notifications.subtitle(this, reminder)

        setContent {
            NudgeTheme(dark = true) {
                val ringing by RingingState.current.collectAsStateWithLifecycle()
                LaunchedEffect(ringing) { if (ringing == null) finish() }
                RingingScreen(
                    reminder = reminder,
                    time = time,
                    subtitle = subtitle,
                    onDone = { startService(AlarmRingingService.intent(this, id, AlarmRingingService.ACTION_DONE)) },
                    onSnooze = { startService(AlarmRingingService.intent(this, id, AlarmRingingService.ACTION_SNOOZE)) },
                )
            }
        }
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    companion object {
        const val EXTRA_ID = "reminder_id"

        fun intent(context: Context, id: Long): Intent =
            Intent(context, AlarmActivity::class.java)
                .putExtra(EXTRA_ID, id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
}
```

Back-press is intentionally not intercepted: the notification stays ongoing and the service keeps ringing until Done/Snooze — leaving the screen never silences the alarm.

- [ ] **Step 4: Manifest entry**

Add inside `<application>`, after `MainActivity`:
```xml
        <activity
            android:name=".alarm.AlarmActivity"
            android:exported="false"
            android:excludeFromRecents="true"
            android:launchMode="singleInstance"
            android:showWhenLocked="true"
            android:turnScreenOn="true"
            android:taskAffinity="" />
```

- [ ] **Step 5: Build (first build since Task 5)**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`. Typical fix-ups if it fails: a missing import in `RingingScreen.kt` (all used symbols are listed in the import block above) or a typo in a manifest attribute.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat: lock-screen ringing activity with pulsing alarm UI"
```

---

### Task 8: Home screen (design 1a home, 1c dark, 1d empty) + navigation + permissions

**Files:**
- Create: `app/src/main/java/dev/viniciuscole/nudge/ui/format/ReminderFormat.kt`
- Create: `app/src/main/java/dev/viniciuscole/nudge/permissions/Permissions.kt`
- Create: `app/src/main/java/dev/viniciuscole/nudge/ui/home/HomeViewModel.kt`
- Create: `app/src/main/java/dev/viniciuscole/nudge/ui/home/HomeScreen.kt`
- Create: `app/src/main/java/dev/viniciuscole/nudge/ui/NudgeNavHost.kt`
- Modify: `app/src/main/java/dev/viniciuscole/nudge/MainActivity.kt`, `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `ReminderRepository`, `DayStatsRepository`, `AlarmScheduler`, `AlarmRingingService.start`, `AlarmActivity.intent`, `NextFire.slotCount`, primitives from Task 2.
- Produces: `ReminderFormat.time(h, m)`, `ReminderFormat.interval(min)`, `ReminderFormat.schedule(res: Resources, r: Reminder): String`; `Permissions.canPostNotifications/canFullScreen/canExactAlarms(ctx)`, `Permissions.fullScreenSettingsIntent/exactAlarmSettingsIntent/notificationSettingsIntent(ctx)`; `HomeViewModel(app)` with `state: StateFlow<HomeUiState>`, `toggle(id, enabled)`, `delete(id)`, `preview(id)`; `HomeScreen(vm, onAdd, onOpenBuilder)`; `NudgeNavHost()` with routes `"home"`, `"add"`, `"builder/{reminderId}"` (the `add` and `builder` destinations are filled by Tasks 9 and 12; here they are placeholders that compile).

- [ ] **Step 1: Strings**

Add to `values/strings.xml`:
```xml
    <string name="home_title">Your day</string>
    <string name="water">Water</string>
    <string name="meals">Meals</string>
    <string name="stat_glasses">/ %1$d glasses</string>
    <string name="stat_on_time">/ %1$d on time</string>
    <string name="reminders">Reminders</string>
    <string name="schedule_daily">%1$s daily</string>
    <string name="schedule_water">%1$s–%2$s, every %3$s</string>
    <string name="schedule_off_suffix">· off</string>
    <string name="empty_title">No reminders yet</string>
    <string name="empty_body">Add your meal times and a hydration window. Nudge will ring until you dismiss it.</string>
    <string name="add_first_reminder">Add first reminder</string>
    <string name="add_reminder">Add reminder</string>
    <string name="delete">Delete</string>
    <string name="perm_fullscreen_title">Allow full-screen alarms</string>
    <string name="perm_fullscreen_body">Nudge needs this to ring over the lock screen.</string>
    <string name="perm_exact_title">Allow exact alarms</string>
    <string name="perm_exact_body">Without it reminders may arrive late.</string>
    <string name="perm_notifications_title">Allow notifications</string>
    <string name="perm_notifications_body">Reminders are delivered as alarm notifications.</string>
    <string name="perm_fix">Fix</string>
    <string name="interval_minutes">%1$d min</string>
    <string name="interval_hours">%1$dh</string>
    <string name="interval_hours_minutes">%1$dh%2$02d</string>
```

- [ ] **Step 2: Formatting helpers**

`app/src/main/java/dev/viniciuscole/nudge/ui/format/ReminderFormat.kt`:
```kotlin
package dev.viniciuscole.nudge.ui.format

import android.content.res.Resources
import dev.viniciuscole.nudge.R
import dev.viniciuscole.nudge.data.model.Reminder
import java.util.Locale

object ReminderFormat {

    fun time(hour: Int, minute: Int): String = String.format(Locale.ROOT, "%02d:%02d", hour, minute)

    fun hourOnly(hour: Int): String = String.format(Locale.ROOT, "%d:00", hour)

    fun interval(res: Resources, minutes: Int): String = when {
        minutes < 60 -> res.getString(R.string.interval_minutes, minutes)
        minutes % 60 == 0 -> res.getString(R.string.interval_hours, minutes / 60)
        else -> res.getString(R.string.interval_hours_minutes, minutes / 60, minutes % 60)
    }

    fun schedule(res: Resources, r: Reminder): String {
        val base = if (r.isMeal) {
            res.getString(R.string.schedule_daily, time(r.hour, r.minute))
        } else {
            res.getString(R.string.schedule_water, hourOnly(r.startHour), hourOnly(r.endHour), interval(res, r.intervalMin))
        }
        return if (r.enabled) base else "$base ${res.getString(R.string.schedule_off_suffix)}"
    }
}
```

- [ ] **Step 3: Permissions helper**

`app/src/main/java/dev/viniciuscole/nudge/permissions/Permissions.kt`:
```kotlin
package dev.viniciuscole.nudge.permissions

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

object Permissions {

    fun canPostNotifications(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun canFullScreen(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
            ctx.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()

    fun canExactAlarms(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ctx.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    fun fullScreenSettingsIntent(ctx: Context): Intent =
        Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${ctx.packageName}"))

    fun exactAlarmSettingsIntent(ctx: Context): Intent =
        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}"))

    fun notificationSettingsIntent(ctx: Context): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
}
```

`Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT` and `ACTION_REQUEST_SCHEDULE_EXACT_ALARM` are string constants; referencing them compiles on all API levels and the intents are only fired behind the `can*` checks above.

- [ ] **Step 4: HomeViewModel**

`app/src/main/java/dev/viniciuscole/nudge/ui/home/HomeViewModel.kt`:
```kotlin
package dev.viniciuscole.nudge.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.viniciuscole.nudge.NudgeApp
import dev.viniciuscole.nudge.alarm.AlarmActivity
import dev.viniciuscole.nudge.alarm.AlarmRingingService
import dev.viniciuscole.nudge.alarm.NextFire
import dev.viniciuscole.nudge.data.model.Reminder
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class HomeUiState(
    val reminders: List<Reminder> = emptyList(),
    val waterDone: Int = 0,
    val waterGoal: Int = 0,
    val mealsDone: Int = 0,
    val mealsTotal: Int = 0,
)

class HomeViewModel(private val app: NudgeApp) : ViewModel() {

    val state: StateFlow<HomeUiState> = combine(app.reminders.reminders, app.stats.stats) { list, stats ->
        val today = if (stats.date == LocalDate.now()) stats else stats.copy(date = LocalDate.now(), waterDone = 0, mealsDone = 0)
        val enabled = list.filter { it.enabled }
        HomeUiState(
            reminders = list,
            waterDone = today.waterDone,
            waterGoal = enabled.filter { it.isWater }.sumOf { NextFire.slotCount(it) },
            mealsDone = today.mealsDone,
            mealsTotal = enabled.count { it.isMeal },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState(app.reminders.reminders.value))

    fun toggle(id: Long, enabled: Boolean) {
        app.reminders.setEnabled(id, enabled)
        app.reminders.get(id)?.let { if (enabled) app.scheduler.schedule(it) else app.scheduler.cancel(id) }
    }

    fun delete(id: Long) {
        app.scheduler.cancel(id)
        app.reminders.delete(id)
    }

    fun preview(id: Long) {
        AlarmRingingService.start(app, id)
        app.startActivity(AlarmActivity.intent(app, id))
    }
}
```

- [ ] **Step 5: HomeScreen**

`app/src/main/java/dev/viniciuscole/nudge/ui/home/HomeScreen.kt`:
```kotlin
package dev.viniciuscole.nudge.ui.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.viniciuscole.nudge.R
import dev.viniciuscole.nudge.data.model.Reminder
import dev.viniciuscole.nudge.permissions.Permissions
import dev.viniciuscole.nudge.ui.components.Card24
import dev.viniciuscole.nudge.ui.components.IconTile
import dev.viniciuscole.nudge.ui.components.NudgeSwitch
import dev.viniciuscole.nudge.ui.components.PillButton
import dev.viniciuscole.nudge.ui.components.SectionLabel
import dev.viniciuscole.nudge.ui.format.ReminderFormat
import dev.viniciuscole.nudge.ui.theme.CapsLabel
import dev.viniciuscole.nudge.ui.theme.NudgeTheme
import dev.viniciuscole.nudge.ui.theme.manrope
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    vm: HomeViewModel,
    onAdd: () -> Unit,
    onOpenBuilder: (Long) -> Unit,
) {
    val c = NudgeTheme.colors
    val ctx = LocalContext.current
    val state by vm.state.collectAsStateWithLifecycle()

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Permissions.canPostNotifications(ctx)) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    var permissionTick by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        permissionTick++
        onPauseOrDispose { }
    }

    Box(Modifier.fillMaxSize().background(c.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Header(onSettings = { ctx.startActivity(Permissions.notificationSettingsIntent(ctx)) })

            if (state.reminders.isEmpty()) {
                EmptyState(onAdd)
            } else {
                StatsRow(state)
                SectionLabel(
                    stringResource(R.string.reminders),
                    Modifier.padding(start = 20.dp, top = 22.dp, bottom = 8.dp),
                )
                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    item(key = "permissions-$permissionTick") { PermissionBanners() }
                    items(state.reminders, key = { it.id }) { r ->
                        DismissibleReminderCard(
                            r = r,
                            onToggle = { vm.toggle(r.id, it) },
                            onDelete = { vm.delete(r.id) },
                            onTap = { if (r.isMeal) onOpenBuilder(r.id) else vm.preview(r.id) },
                            onLongPress = { vm.preview(r.id) },
                        )
                    }
                }
            }
        }

        if (state.reminders.isNotEmpty()) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 20.dp, bottom = 28.dp)
                    .shadow(12.dp, RoundedCornerShape(22.dp), spotColor = c.coral.copy(alpha = .5f))
                    .size(64.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(c.coral)
                    .clickable(onClick = onAdd),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_plus), stringResource(R.string.add_reminder), tint = Color.White, modifier = Modifier.size(28.dp))
            }
        }
    }
}

@Composable
private fun Header(onSettings: () -> Unit) {
    val c = NudgeTheme.colors
    val date = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.getDefault()))
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f)) {
            Text(date.uppercase(Locale.getDefault()), style = manrope(13.sp, FontWeight.Medium, letterSpacing = 1.2.sp), color = c.faint)
            Text(stringResource(R.string.home_title), style = manrope(30.sp, FontWeight.ExtraBold), color = c.ink, modifier = Modifier.padding(top = 4.dp))
        }
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(c.chip).clickable(onClick = onSettings),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(16.dp).clip(CircleShape).border(2.dp, c.faint, CircleShape))
        }
    }
}

@Composable
private fun StatsRow(state: HomeUiState) {
    val c = NudgeTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StatCard(
            Modifier.weight(1f),
            bg = c.tealContainer, label = stringResource(R.string.water), labelColor = c.tealDeep,
            value = state.waterDone, valueColor = c.tealOnContainer,
            suffix = stringResource(R.string.stat_glasses, state.waterGoal), suffixColor = c.tealMuted,
        )
        StatCard(
            Modifier.weight(1f),
            bg = c.coralContainer, label = stringResource(R.string.meals), labelColor = c.coralDeep,
            value = state.mealsDone, valueColor = c.coralOnContainer,
            suffix = stringResource(R.string.stat_on_time, state.mealsTotal), suffixColor = c.coralMuted,
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier, bg: Color, label: String, labelColor: Color,
    value: Int, valueColor: Color, suffix: String, suffixColor: Color,
) {
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(bg).padding(horizontal = 16.dp, vertical = 14.dp)) {
        Text(label.uppercase(), style = manrope(11.5.sp, FontWeight.Medium, letterSpacing = .7.sp), color = labelColor)
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 6.dp)) {
            Text("$value", style = manrope(22.sp, FontWeight.ExtraBold), color = valueColor)
            Text(" $suffix", style = manrope(13.sp, FontWeight.SemiBold), color = suffixColor, modifier = Modifier.padding(bottom = 3.dp))
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun DismissibleReminderCard(
    r: Reminder,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
) {
    val c = NudgeTheme.colors
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { v ->
            if (v == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp)).background(c.coralContainer).padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(painterResource(R.drawable.ic_close), stringResource(R.string.delete), tint = c.coralDeep)
            }
        },
    ) {
        ReminderCard(r, onToggle, onTap, onLongPress)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReminderCard(r: Reminder, onToggle: (Boolean) -> Unit, onTap: () -> Unit, onLongPress: () -> Unit) {
    val c = NudgeTheme.colors
    val res = LocalContext.current.resources
    Card24(Modifier.fillMaxWidth().alpha(if (r.enabled) 1f else .7f)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                Modifier.weight(1f).combinedClickable(onClick = onTap, onLongClick = onLongPress),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (r.isMeal) {
                    IconTile(R.drawable.ic_fork, bg = if (r.enabled) c.coralContainer else c.chip, tint = if (r.enabled) c.coralDeep else c.faint)
                } else {
                    IconTile(R.drawable.ic_drop, bg = if (r.enabled) c.tealContainer else c.chip, tint = if (r.enabled) c.tealDeep else c.faint)
                }
                Column(Modifier.weight(1f)) {
                    Text(r.label, style = manrope(16.5.sp, FontWeight.Bold), color = if (r.enabled) c.ink else c.body)
                    Text(ReminderFormat.schedule(res, r), style = manrope(13.5.sp, FontWeight.Medium), color = c.muted, modifier = Modifier.padding(top = 3.dp))
                }
            }
            NudgeSwitch(checked = r.enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun PermissionBanners() {
    val ctx = LocalContext.current
    val c = NudgeTheme.colors
    val banners = buildList {
        if (!Permissions.canPostNotifications(ctx)) add(Triple(R.string.perm_notifications_title, R.string.perm_notifications_body, Permissions.notificationSettingsIntent(ctx)))
        if (!Permissions.canFullScreen(ctx)) add(Triple(R.string.perm_fullscreen_title, R.string.perm_fullscreen_body, Permissions.fullScreenSettingsIntent(ctx)))
        if (!Permissions.canExactAlarms(ctx)) add(Triple(R.string.perm_exact_title, R.string.perm_exact_body, Permissions.exactAlarmSettingsIntent(ctx)))
    }
    if (banners.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 12.dp)) {
        banners.forEach { (title, body, intent) ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.coralContainer).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(title), style = manrope(15.sp, FontWeight.Bold), color = c.coralOnContainer)
                    Text(stringResource(body), style = manrope(13.sp, FontWeight.Medium), color = c.coralMuted, modifier = Modifier.padding(top = 2.dp))
                }
                Text(
                    stringResource(R.string.perm_fix),
                    style = manrope(14.sp, FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(c.coral).clickable { ctx.startActivity(intent) }.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun EmptyState(onAdd: () -> Unit) {
    val c = NudgeTheme.colors
    Column(
        Modifier.fillMaxSize().padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(200.dp).clip(CircleShape).background(c.chip))
            Box(
                Modifier.align(Alignment.TopStart).offset(x = 18.dp, y = 34.dp).size(78.dp).rotate(-8f)
                    .clip(RoundedCornerShape(26.dp)).background(c.tealContainer),
                contentAlignment = Alignment.Center,
            ) { Icon(painterResource(R.drawable.ic_drop), null, tint = c.tealDeep, modifier = Modifier.size(36.dp)) }
            Box(
                Modifier.align(Alignment.BottomEnd).offset(x = (-16).dp, y = (-30).dp).size(88.dp).rotate(7f)
                    .clip(RoundedCornerShape(28.dp)).background(c.coralContainer),
                contentAlignment = Alignment.Center,
            ) { Icon(painterResource(R.drawable.ic_fork), null, tint = c.coralDeep, modifier = Modifier.size(40.dp)) }
        }
        Spacer(Modifier.height(26.dp))
        Text(stringResource(R.string.empty_title), style = manrope(23.sp, FontWeight.ExtraBold), color = c.ink, textAlign = TextAlign.Center)
        Text(
            stringResource(R.string.empty_body),
            style = manrope(15.sp, FontWeight.Medium, lineHeight = 22.sp),
            color = c.muted, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp),
        )
        Spacer(Modifier.height(26.dp))
        PillButton(
            text = stringResource(R.string.add_first_reminder),
            onClick = onAdd,
            height = 54.dp,
            modifier = Modifier.width(220.dp),
        )
    }
}
```

- [ ] **Step 6: NavHost and MainActivity**

`app/src/main/java/dev/viniciuscole/nudge/ui/NudgeNavHost.kt`:
```kotlin
package dev.viniciuscole.nudge.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.viniciuscole.nudge.NudgeApp
import dev.viniciuscole.nudge.ui.home.HomeScreen
import dev.viniciuscole.nudge.ui.home.HomeViewModel

object Routes {
    const val HOME = "home"
    const val ADD = "add"
    const val BUILDER = "builder/{reminderId}"
    fun builder(id: Long) = "builder/$id"
}

@Composable
fun NudgeNavHost() {
    val nav = rememberNavController()
    val app = NudgeApp.from(LocalContext.current)

    NavHost(nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            val vm: HomeViewModel = viewModel(factory = viewModelFactory { initializer { HomeViewModel(app) } })
            HomeScreen(
                vm = vm,
                onAdd = { nav.navigate(Routes.ADD) },
                onOpenBuilder = { id -> nav.navigate(Routes.builder(id)) },
            )
        }
        composable(Routes.ADD) {
            // Task 9 replaces this with AddReminderScreen
        }
        composable(
            Routes.BUILDER,
            arguments = listOf(navArgument("reminderId") { type = NavType.LongType }),
        ) {
            // Task 12 replaces this with MealBuilderScreen
        }
    }
}
```

`MainActivity.kt` — final form:
```kotlin
package dev.viniciuscole.nudge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.viniciuscole.nudge.ui.NudgeNavHost
import dev.viniciuscole.nudge.ui.theme.NudgeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NudgeTheme { NudgeNavHost() }
        }
    }
}
```

- [ ] **Step 7: Build**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Manual check on a device (optional now, required at Task 9)**

Wireless debugging from WSL2: on the phone enable Developer options → Wireless debugging → Pair device with pairing code, then:
```bash
adb pair <ip>:<pairing-port>
adb connect <ip>:<port>
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n dev.viniciuscole.nudge/.MainActivity
```
Expected: empty state (design 1d) with the illustration and "Add first reminder".

- [ ] **Step 9: Commit**

```bash
git add -A
git commit -m "feat: home screen with stats, reminder cards, empty state and navigation"
```

---

### Task 9: Add Reminder screen (design 1a "New reminder") — saves and schedules

**Files:**
- Create: `app/src/main/java/dev/viniciuscole/nudge/ui/add/AddReminderViewModel.kt`
- Create: `app/src/main/java/dev/viniciuscole/nudge/ui/add/AddReminderScreen.kt`
- Modify: `app/src/main/java/dev/viniciuscole/nudge/ui/NudgeNavHost.kt`, `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `ReminderRepository.nextId/upsert`, `AlarmScheduler.schedule`, `ReminderFormat`, primitives (`SegmentedPill`, `NudgeChip`, `NudgeSwitch`, `PillButton`, `CircleIconButton`, `CircleTextButton`, `SectionLabel`, `Card24`).
- Produces: `AddReminderViewModel(app)` with `draft: StateFlow<Draft>`, mutators, `save(): Reminder`; `AddReminderScreen(vm, onBack)`.

- [ ] **Step 1: Strings**

Add to `values/strings.xml`:
```xml
    <string name="new_reminder">New reminder</string>
    <string name="type_meal">Meal</string>
    <string name="type_water">Water</string>
    <string name="label">Label</string>
    <string name="label_hint">e.g. Lunch</string>
    <string name="time">Time</string>
    <string name="start">Start</string>
    <string name="end">End</string>
    <string name="every">Every</string>
    <string name="custom_interval">Custom</string>
    <string name="custom_interval_hint">Minutes</string>
    <string name="insistent_title">Insistent alarm</string>
    <string name="insistent_body">Rings and vibrates until dismissed</string>
    <string name="save_reminder">Save reminder</string>
    <string name="default_meal_label">Meal</string>
    <string name="default_water_label">Drink water</string>
    <string name="back">Back</string>
    <string name="ok">OK</string>
    <string name="cancel">Cancel</string>
```

- [ ] **Step 2: ViewModel with the draft state**

`app/src/main/java/dev/viniciuscole/nudge/ui/add/AddReminderViewModel.kt`:
```kotlin
package dev.viniciuscole.nudge.ui.add

import androidx.lifecycle.ViewModel
import dev.viniciuscole.nudge.NudgeApp
import dev.viniciuscole.nudge.data.model.Reminder
import dev.viniciuscole.nudge.data.model.ReminderType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class Draft(
    val type: ReminderType = ReminderType.MEAL,
    val label: String = "",
    val hour: Int = 12,
    val minute: Int = 30,
    val startHour: Int = 8,
    val endHour: Int = 22,
    val intervalMin: Int = 90,
    val customInterval: Boolean = false,
    val insistent: Boolean = true,
)

class AddReminderViewModel(private val app: NudgeApp) : ViewModel() {

    private val _draft = MutableStateFlow(Draft())
    val draft: StateFlow<Draft> = _draft.asStateFlow()

    val presetIntervals = listOf(30, 60, 90, 120)

    fun setType(type: ReminderType) = _draft.update { it.copy(type = type) }
    fun setLabel(label: String) = _draft.update { it.copy(label = label) }
    fun setTime(hour: Int, minute: Int) = _draft.update { it.copy(hour = hour, minute = minute) }
    fun setInsistent(v: Boolean) = _draft.update { it.copy(insistent = v) }

    fun nudgeTime(deltaMinutes: Int) = _draft.update {
        val total = ((it.hour * 60 + it.minute + deltaMinutes) % 1440 + 1440) % 1440
        it.copy(hour = total / 60, minute = total % 60)
    }

    fun setStartHour(h: Int) = _draft.update { it.copy(startHour = h.coerceIn(0, 23)) }
    fun setEndHour(h: Int) = _draft.update { it.copy(endHour = h.coerceIn(0, 23)) }

    fun pickInterval(min: Int) = _draft.update { it.copy(intervalMin = min, customInterval = false) }
    fun pickCustomInterval() = _draft.update { it.copy(customInterval = true) }
    fun setCustomInterval(text: String) {
        val v = text.filter(Char::isDigit).take(3).toIntOrNull() ?: return
        _draft.update { it.copy(intervalMin = v) }
    }

    fun save(defaultMealLabel: String, defaultWaterLabel: String): Reminder {
        val d = _draft.value
        val label = d.label.trim().ifEmpty { if (d.type == ReminderType.MEAL) defaultMealLabel else defaultWaterLabel }
        val reminder = Reminder(
            id = app.reminders.nextId(),
            type = d.type,
            label = label,
            enabled = true,
            insistent = d.insistent,
            hour = d.hour,
            minute = d.minute,
            startHour = d.startHour,
            endHour = d.endHour,
            intervalMin = d.intervalMin.coerceAtLeast(5),
        )
        app.reminders.upsert(reminder)
        app.scheduler.schedule(reminder)
        return reminder
    }
}
```

- [ ] **Step 3: Screen**

`app/src/main/java/dev/viniciuscole/nudge/ui/add/AddReminderScreen.kt`:
```kotlin
package dev.viniciuscole.nudge.ui.add

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.viniciuscole.nudge.R
import dev.viniciuscole.nudge.data.model.ReminderType
import dev.viniciuscole.nudge.ui.components.Card24
import dev.viniciuscole.nudge.ui.components.CircleIconButton
import dev.viniciuscole.nudge.ui.components.CircleTextButton
import dev.viniciuscole.nudge.ui.components.NudgeChip
import dev.viniciuscole.nudge.ui.components.NudgeSwitch
import dev.viniciuscole.nudge.ui.components.PillButton
import dev.viniciuscole.nudge.ui.components.SectionLabel
import dev.viniciuscole.nudge.ui.components.SegmentedPill
import dev.viniciuscole.nudge.ui.format.ReminderFormat
import dev.viniciuscole.nudge.ui.theme.NudgeTheme
import dev.viniciuscole.nudge.ui.theme.manrope

private enum class Picker { NONE, MEAL_TIME, WATER_START, WATER_END }

@Composable
fun AddReminderScreen(vm: AddReminderViewModel, onBack: () -> Unit) {
    val c = NudgeTheme.colors
    val d by vm.draft.collectAsStateWithLifecycle()
    val isMeal = d.type == ReminderType.MEAL
    var picker by remember { mutableStateOf(Picker.NONE) }
    val defaultMeal = stringResource(R.string.default_meal_label)
    val defaultWater = stringResource(R.string.default_water_label)

    Column(Modifier.fillMaxSize().background(c.bg).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CircleIconButton(R.drawable.ic_back, onClick = onBack, contentDescription = stringResource(R.string.back))
            Text(stringResource(R.string.new_reminder), style = manrope(19.sp, FontWeight.Bold), color = c.ink)
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            SegmentedPill(
                options = listOf(stringResource(R.string.type_meal), stringResource(R.string.type_water)),
                selectedIndex = if (isMeal) 0 else 1,
                onSelect = { vm.setType(if (it == 0) ReminderType.MEAL else ReminderType.WATER) },
                colors = listOf(c.coral, c.teal),
            )

            Column {
                SectionLabel(stringResource(R.string.label), Modifier.padding(bottom = 8.dp))
                FieldBox {
                    BasicTextField(
                        value = d.label,
                        onValueChange = vm::setLabel,
                        singleLine = true,
                        textStyle = manrope(16.sp, FontWeight.SemiBold).copy(color = c.ink),
                        cursorBrush = SolidColor(c.coral),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (d.label.isEmpty()) {
                                Text(stringResource(R.string.label_hint), style = manrope(16.sp, FontWeight.SemiBold), color = c.faint)
                            }
                            inner()
                        },
                    )
                }
            }

            if (isMeal) {
                Column {
                    SectionLabel(stringResource(R.string.time), Modifier.padding(bottom = 8.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(c.card)
                            .border(1.5.dp, c.line, RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircleTextButton("−", onClick = { vm.nudgeTime(-15) }, size = 40.dp, fontSize = 20)
                        Text(
                            ReminderFormat.time(d.hour, d.minute),
                            style = manrope(44.sp, FontWeight.ExtraBold, letterSpacing = (-0.9).sp),
                            color = c.ink,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.widthIn(min = 132.dp).padding(horizontal = 10.dp).clickable { picker = Picker.MEAL_TIME },
                        )
                        CircleTextButton("+", onClick = { vm.nudgeTime(15) }, size = 40.dp, fontSize = 20)
                    }
                    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(8 to 0, 12 to 30, 19 to 0).forEach { (h, m) ->
                            NudgeChip(ReminderFormat.time(h, m), selected = d.hour == h && d.minute == m, onClick = { vm.setTime(h, m) }, selectedColor = c.coral)
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        HourTile(Modifier.weight(1f), stringResource(R.string.start), ReminderFormat.time(d.startHour, 0)) { picker = Picker.WATER_START }
                        HourTile(Modifier.weight(1f), stringResource(R.string.end), ReminderFormat.time(d.endHour, 0)) { picker = Picker.WATER_END }
                    }
                    Column {
                        SectionLabel(stringResource(R.string.every), Modifier.padding(bottom = 8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val res = androidx.compose.ui.platform.LocalContext.current.resources
                            vm.presetIntervals.forEach { min ->
                                NudgeChip(ReminderFormat.interval(res, min), selected = !d.customInterval && d.intervalMin == min, onClick = { vm.pickInterval(min) })
                            }
                            NudgeChip(stringResource(R.string.custom_interval), selected = d.customInterval, onClick = vm::pickCustomInterval)
                        }
                        if (d.customInterval) {
                            FieldBox(Modifier.padding(top = 12.dp)) {
                                BasicTextField(
                                    value = if (d.intervalMin == 0) "" else d.intervalMin.toString(),
                                    onValueChange = vm::setCustomInterval,
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    textStyle = manrope(16.sp, FontWeight.SemiBold).copy(color = c.ink),
                                    cursorBrush = SolidColor(c.teal),
                                    modifier = Modifier.fillMaxWidth(),
                                    decorationBox = { inner ->
                                        if (d.intervalMin == 0) Text(stringResource(R.string.custom_interval_hint), style = manrope(16.sp, FontWeight.SemiBold), color = c.faint)
                                        inner()
                                    },
                                )
                            }
                        }
                    }
                }
            }

            Card24(Modifier.fillMaxWidth(), radius = 20.dp) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.insistent_title), style = manrope(15.sp, FontWeight.Bold), color = c.ink)
                        Text(stringResource(R.string.insistent_body), style = manrope(13.sp, FontWeight.Medium), color = c.muted, modifier = Modifier.padding(top = 2.dp))
                    }
                    NudgeSwitch(checked = d.insistent, onCheckedChange = vm::setInsistent)
                }
            }
        }

        Box(Modifier.fillMaxWidth().background(c.bg).navigationBarsPadding().imePadding().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp)) {
            PillButton(
                text = stringResource(R.string.save_reminder),
                onClick = {
                    vm.save(defaultMeal, defaultWater)
                    onBack()
                },
                color = if (isMeal) c.coral else c.teal,
            )
        }
    }

    when (picker) {
        Picker.NONE -> Unit
        Picker.MEAL_TIME -> TimeDialog(d.hour, d.minute, onDismiss = { picker = Picker.NONE }) { h, m -> vm.setTime(h, m); picker = Picker.NONE }
        Picker.WATER_START -> TimeDialog(d.startHour, 0, onDismiss = { picker = Picker.NONE }) { h, _ -> vm.setStartHour(h); picker = Picker.NONE }
        Picker.WATER_END -> TimeDialog(d.endHour, 0, onDismiss = { picker = Picker.NONE }) { h, _ -> vm.setEndHour(h); picker = Picker.NONE }
    }
}

@Composable
private fun FieldBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val c = NudgeTheme.colors
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.card)
            .border(1.5.dp, c.line, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) { content() }
}

@Composable
private fun HourTile(modifier: Modifier, label: String, value: String, onClick: () -> Unit) {
    val c = NudgeTheme.colors
    Column(modifier) {
        SectionLabel(label, Modifier.padding(bottom = 8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(c.card)
                .border(1.5.dp, c.line, RoundedCornerShape(16.dp))
                .clickable(onClick = onClick)
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(value, style = manrope(26.sp, FontWeight.ExtraBold), color = c.ink)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(hour: Int, minute: Int, onDismiss: () -> Unit, onConfirm: (Int, Int) -> Unit) {
    val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text(stringResource(R.string.ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        text = { TimePicker(state = state) },
    )
}
```

- [ ] **Step 4: Wire into the NavHost**

In `NudgeNavHost.kt` replace the `composable(Routes.ADD) { ... }` block with:
```kotlin
        composable(Routes.ADD) {
            val vm: AddReminderViewModel = viewModel(factory = viewModelFactory { initializer { AddReminderViewModel(app) } })
            AddReminderScreen(vm = vm, onBack = { nav.popBackStack() })
        }
```
Imports: `dev.viniciuscole.nudge.ui.add.AddReminderScreen`, `dev.viniciuscole.nudge.ui.add.AddReminderViewModel`.

- [ ] **Step 5: Build**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: End-to-end manual check on a device (required)**

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n dev.viniciuscole.nudge/.MainActivity
```
1. Grant notifications when prompted. If a "Allow full-screen alarms" banner appears, tap Fix and enable it.
2. Tap "Add first reminder" → Meal → set the time to 2 minutes from now → Save. The card appears with "HH:MM daily".
3. Lock the phone and wait. Expected: screen turns on with the dark coral ringing screen (design 1b), alarm tone loops, phone vibrates.
4. Tap "Snooze 5 min" → ringing stops, screen closes; 5 minutes later it rings again.
5. Tap "Done" → the Meals stat on Home shows `1 / 1 on time`.
6. Add a Water reminder (8:00–22:00, every 30 min) and long-press its card → the teal ringing screen appears while unlocked (preview path); Done increments the Water stat.
7. `adb reboot`, wait, then `adb shell dumpsys alarm | grep nudge` — expected: an entry for `dev.viniciuscole.nudge` (BootReceiver rescheduled).

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat: add-reminder form with time pickers, intervals and insistent toggle"
```

---

### Task 10: Food data — bundled table, USDA FoodData Central client, merged search

**Files:**
- Create: `app/src/main/java/dev/viniciuscole/nudge/data/food/FoodItem.kt`
- Create: `app/src/main/java/dev/viniciuscole/nudge/data/food/LocalFoods.kt`
- Create: `app/src/main/java/dev/viniciuscole/nudge/data/food/UsdaClient.kt`
- Create: `app/src/main/java/dev/viniciuscole/nudge/data/food/FoodSearch.kt`
- Create: `app/src/test/java/dev/viniciuscole/nudge/data/food/UsdaClientTest.kt`
- Modify: `app/src/main/java/dev/viniciuscole/nudge/NudgeApp.kt`

**Interfaces:**
- Consumes: `BuildConfig.USDA_API_KEY` (Task 1).
- Produces: `data class FoodItem(name: String, unit: String, kcal100: Double, p100: Double, c100: Double, f100: Double, source: String)`; `LocalFoods.ALL: List<FoodItem>`; `UsdaClient(apiKey).search(query): List<FoodItem>` (suspend), `UsdaClient.parse(json: String): List<FoodItem>`; `FoodSearch(usda).search(query): List<FoodItem>` (suspend, local first, deduped, max 8); `NudgeApp.foodSearch: FoodSearch`.

- [ ] **Step 1: Model + bundled foods (values per 100 g/ml, from the design's table)**

`app/src/main/java/dev/viniciuscole/nudge/data/food/FoodItem.kt`:
```kotlin
package dev.viniciuscole.nudge.data.food

data class FoodItem(
    val name: String,
    val unit: String,
    val kcal100: Double,
    val p100: Double,
    val c100: Double,
    val f100: Double,
    val source: String,
) {
    val initials: String
        get() = name.replace(Regex("[^A-Za-z ]"), "").split(" ").filter { it.isNotBlank() }.take(2)
            .joinToString("") { it.first().uppercase() }

    companion object {
        const val SOURCE_LOCAL = "local"
        const val SOURCE_USDA = "usda"
    }
}
```

`app/src/main/java/dev/viniciuscole/nudge/data/food/LocalFoods.kt`:
```kotlin
package dev.viniciuscole.nudge.data.food

object LocalFoods {
    private fun g(name: String, kcal: Double, p: Double, c: Double, f: Double) =
        FoodItem(name, "g", kcal, p, c, f, FoodItem.SOURCE_LOCAL)

    private fun ml(name: String, kcal: Double, p: Double, c: Double, f: Double) =
        FoodItem(name, "ml", kcal, p, c, f, FoodItem.SOURCE_LOCAL)

    val ALL: List<FoodItem> = listOf(
        g("Chicken breast", 165.0, 31.0, 0.0, 3.6),
        g("Chicken thigh", 209.0, 26.0, 0.0, 11.0),
        g("Chickpeas, cooked", 164.0, 8.9, 27.0, 2.6),
        g("Rice, cooked", 130.0, 2.7, 28.0, 0.3),
        ml("Olive oil", 884.0, 0.0, 0.0, 100.0),
        g("Avocado", 160.0, 2.0, 9.0, 15.0),
        g("Greek yogurt", 59.0, 10.0, 3.6, 0.4),
        g("Egg", 155.0, 13.0, 1.1, 11.0),
        g("Broccoli", 34.0, 2.8, 7.0, 0.4),
        g("Almonds", 579.0, 21.0, 22.0, 50.0),
        g("Salmon", 208.0, 20.0, 0.0, 13.0),
        g("Cherry tomatoes", 18.0, 0.9, 3.9, 0.2),
        g("Spinach", 23.0, 2.9, 3.6, 0.4),
        g("Wholegrain bread", 247.0, 13.0, 41.0, 3.4),
        g("Feta", 264.0, 14.0, 4.1, 21.0),
    )

    fun search(query: String, limit: Int = 4): List<FoodItem> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        return ALL.filter { it.name.lowercase().contains(q) }.take(limit)
    }
}
```

- [ ] **Step 2: Failing parser test (USDA search response shape)**

`app/src/test/java/dev/viniciuscole/nudge/data/food/UsdaClientTest.kt`:
```kotlin
package dev.viniciuscole.nudge.data.food

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UsdaClientTest {

    private val sample = """
        {"foods":[
          {"fdcId":1,"description":"CHICKEN, BROILERS OR FRYERS, BREAST, MEAT ONLY, RAW",
           "foodNutrients":[
             {"nutrientNumber":"208","nutrientName":"Energy","unitName":"KCAL","value":120.0},
             {"nutrientNumber":"203","nutrientName":"Protein","unitName":"G","value":22.5},
             {"nutrientNumber":"204","nutrientName":"Total lipid (fat)","unitName":"G","value":2.6},
             {"nutrientNumber":"205","nutrientName":"Carbohydrate, by difference","unitName":"G","value":0.0}
           ]},
          {"fdcId":2,"description":"Olive oil",
           "foodNutrients":[
             {"nutrientNumber":"957","nutrientName":"Energy (Atwater General Factors)","unitName":"KCAL","value":884.0},
             {"nutrientNumber":"204","nutrientName":"Total lipid (fat)","unitName":"G","value":100.0}
           ]},
          {"fdcId":3,"description":"Water","foodNutrients":[]}
        ]}
    """.trimIndent()

    @Test
    fun parsesNutrientsPer100gAndCapitalizesName() {
        val items = UsdaClient.parse(sample)
        val chicken = items.first { it.name.startsWith("Chicken") }
        assertEquals("Chicken, broilers or fryers, breast, meat only, raw", chicken.name)
        assertEquals(120.0, chicken.kcal100, 0.001)
        assertEquals(22.5, chicken.p100, 0.001)
        assertEquals(0.0, chicken.c100, 0.001)
        assertEquals(2.6, chicken.f100, 0.001)
        assertEquals("g", chicken.unit)
        assertEquals(FoodItem.SOURCE_USDA, chicken.source)
    }

    @Test
    fun fallsBackToAtwaterEnergyAndDropsFoodsWithoutEnergy() {
        val items = UsdaClient.parse(sample)
        assertEquals(2, items.size)
        assertEquals(884.0, items.first { it.name == "Olive oil" }.kcal100, 0.001)
        assertTrue(items.none { it.name == "Water" })
    }

    @Test
    fun malformedJsonYieldsEmptyList() {
        assertTrue(UsdaClient.parse("not json").isEmpty())
        assertTrue(UsdaClient.parse("{}").isEmpty())
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "dev.viniciuscole.nudge.data.food.UsdaClientTest"`
Expected: compilation FAILS with `Unresolved reference: UsdaClient`.

- [ ] **Step 4: USDA client (HttpURLConnection + org.json, no extra deps)**

`app/src/main/java/dev/viniciuscole/nudge/data/food/UsdaClient.kt`:
```kotlin
package dev.viniciuscole.nudge.data.food

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class UsdaClient(private val apiKey: String) {

    val enabled: Boolean get() = apiKey.isNotBlank()

    suspend fun search(query: String, pageSize: Int = 8): List<FoodItem> = withContext(Dispatchers.IO) {
        if (!enabled || query.isBlank()) return@withContext emptyList()
        val url = URL(
            "https://api.nal.usda.gov/fdc/v1/foods/search" +
                "?api_key=" + URLEncoder.encode(apiKey, "UTF-8") +
                "&query=" + URLEncoder.encode(query.trim(), "UTF-8") +
                "&pageSize=$pageSize&dataType=Foundation,SR%20Legacy",
        )
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 5_000
            conn.readTimeout = 5_000
            if (conn.responseCode != 200) return@withContext emptyList()
            parse(conn.inputStream.bufferedReader().use { it.readText() })
        } catch (e: Exception) {
            emptyList()
        } finally {
            conn.disconnect()
        }
    }

    companion object {
        // FoodData Central nutrient numbers: 208 Energy kcal, 957/958 Atwater energy, 203 protein, 205 carbs, 204 fat
        private val ENERGY = listOf("208", "957", "958")
        private const val PROTEIN = "203"
        private const val CARBS = "205"
        private const val FAT = "204"

        fun parse(json: String): List<FoodItem> {
            val foods: JSONArray = try {
                JSONObject(json).optJSONArray("foods") ?: return emptyList()
            } catch (e: Exception) {
                return emptyList()
            }
            return (0 until foods.length()).mapNotNull { i ->
                val f = foods.optJSONObject(i) ?: return@mapNotNull null
                val nutrients = f.optJSONArray("foodNutrients") ?: JSONArray()
                val kcal = ENERGY.firstNotNullOfOrNull { n -> nutrient(nutrients, n) } ?: return@mapNotNull null
                if (kcal <= 0.0) return@mapNotNull null
                FoodItem(
                    name = prettify(f.optString("description")),
                    unit = "g",
                    kcal100 = kcal,
                    p100 = nutrient(nutrients, PROTEIN) ?: 0.0,
                    c100 = nutrient(nutrients, CARBS) ?: 0.0,
                    f100 = nutrient(nutrients, FAT) ?: 0.0,
                    source = FoodItem.SOURCE_USDA,
                )
            }
        }

        private fun nutrient(arr: JSONArray, number: String): Double? {
            for (k in 0 until arr.length()) {
                val o = arr.optJSONObject(k) ?: continue
                if (o.optString("nutrientNumber") == number) return o.optDouble("value").takeUnless { it.isNaN() }
            }
            return null
        }

        private fun prettify(raw: String): String =
            raw.trim().lowercase().replaceFirstChar { it.uppercase() }
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "dev.viniciuscole.nudge.data.food.UsdaClientTest"`
Expected: `BUILD SUCCESSFUL`, 3 tests passed.

- [ ] **Step 6: Merged search**

`app/src/main/java/dev/viniciuscole/nudge/data/food/FoodSearch.kt`:
```kotlin
package dev.viniciuscole.nudge.data.food

class FoodSearch(private val usda: UsdaClient) {

    suspend fun search(query: String, limit: Int = 8): List<FoodItem> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val local = LocalFoods.search(q)
        if (!usda.enabled) return local
        val remote = usda.search(q)
        val seen = local.map { it.name.lowercase() }.toMutableSet()
        return (local + remote.filter { seen.add(it.name.lowercase()) }).take(limit)
    }
}
```

- [ ] **Step 7: Register in NudgeApp**

Add to `NudgeApp.kt`:
```kotlin
val foodSearch: FoodSearch by lazy { FoodSearch(UsdaClient(BuildConfig.USDA_API_KEY)) }
```
Imports: `dev.viniciuscole.nudge.data.food.FoodSearch`, `dev.viniciuscole.nudge.data.food.UsdaClient`.

- [ ] **Step 8: Build and commit**

Run: `./gradlew assembleDebug testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`.

```bash
git add -A
git commit -m "feat: food search over bundled table and USDA FoodData Central"
```

---

### Task 11: Meal model, nutrition math, meal JSON codec and repository

**Files:**
- Create: `app/src/main/java/dev/viniciuscole/nudge/data/model/Meal.kt`
- Create: `app/src/main/java/dev/viniciuscole/nudge/data/json/MealJson.kt`
- Create: `app/src/main/java/dev/viniciuscole/nudge/data/MealRepository.kt`
- Create: `app/src/test/java/dev/viniciuscole/nudge/data/model/NutritionTest.kt`
- Modify: `app/src/main/java/dev/viniciuscole/nudge/NudgeApp.kt`

**Interfaces:**
- Consumes: `FoodItem` (Task 10).
- Produces:
  - `data class Ingredient(id: Long, name: String, unit: String, qty: Int, kcal100: Double, p100: Double, c100: Double, f100: Double)` with `kcal: Double`, `Ingredient.from(food: FoodItem, id: Long): Ingredient`
  - `data class MealTotals(kcal: Int, proteinG: Int, carbsG: Int, fatG: Int, proteinPct: Int, carbsPct: Int, fatPct: Int)`
  - `Nutrition.totals(list: List<Ingredient>): MealTotals`, `Nutrition.dayShare(kcal: Int, dayGoal: Int = 1800): Int` (percent)
  - `data class SavedMeal(id: Long, reminderId: Long, label: String, date: LocalDate, ingredients: List<Ingredient>)`
  - `MealJson.encode/decode`
  - `MealRepository.meals: StateFlow<List<SavedMeal>>`, `forReminderOn(reminderId, date): SavedMeal?`, `save(meal)`, `nextId(): Long`
  - `NudgeApp.meals: MealRepository`

- [ ] **Step 1: Failing nutrition test (numbers from the design's sample meal)**

`app/src/test/java/dev/viniciuscole/nudge/data/model/NutritionTest.kt`:
```kotlin
package dev.viniciuscole.nudge.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class NutritionTest {

    private val meal = listOf(
        Ingredient(11, "Chicken breast", "g", 150, 165.0, 31.0, 0.0, 3.6),
        Ingredient(12, "Rice, cooked", "g", 180, 130.0, 2.7, 28.0, 0.3),
        Ingredient(13, "Broccoli", "g", 120, 34.0, 2.8, 7.0, 0.4),
        Ingredient(14, "Olive oil", "ml", 10, 884.0, 0.0, 0.0, 100.0),
    )

    @Test
    fun totalsMatchDesignSample() {
        val t = Nutrition.totals(meal)
        assertEquals(611, t.kcal)
        assertEquals(55, t.proteinG)
        assertEquals(59, t.carbsG)
        assertEquals(16, t.fatG)
        assertEquals(36, t.proteinPct)
        assertEquals(39, t.carbsPct)
        assertEquals(25, t.fatPct)
    }

    @Test
    fun emptyMealIsAllZero() {
        assertEquals(MealTotals(0, 0, 0, 0, 0, 0, 0), Nutrition.totals(emptyList()))
    }

    @Test
    fun ingredientKcalScalesWithQuantity() {
        assertEquals(247.5, meal[0].kcal, 0.001)
        assertEquals(88.4, meal[3].kcal, 0.001)
    }

    @Test
    fun dayShareIsRoundedPercentOfGoal() {
        assertEquals(34, Nutrition.dayShare(611))
        assertEquals(0, Nutrition.dayShare(0))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "dev.viniciuscole.nudge.data.model.NutritionTest"`
Expected: compilation FAILS with `Unresolved reference: Ingredient`.

- [ ] **Step 3: Model + math**

`app/src/main/java/dev/viniciuscole/nudge/data/model/Meal.kt`:
```kotlin
package dev.viniciuscole.nudge.data.model

import dev.viniciuscole.nudge.data.food.FoodItem
import java.time.LocalDate
import kotlin.math.roundToInt

data class Ingredient(
    val id: Long,
    val name: String,
    val unit: String,
    val qty: Int,
    val kcal100: Double,
    val p100: Double,
    val c100: Double,
    val f100: Double,
) {
    val kcal: Double get() = kcal100 * qty / 100.0
    val proteinG: Double get() = p100 * qty / 100.0
    val carbsG: Double get() = c100 * qty / 100.0
    val fatG: Double get() = f100 * qty / 100.0
    val step: Int get() = if (unit == "ml") 5 else 10

    companion object {
        fun from(food: FoodItem, id: Long): Ingredient = Ingredient(
            id = id,
            name = food.name,
            unit = food.unit,
            qty = if (food.unit == "ml") 10 else 100,
            kcal100 = food.kcal100,
            p100 = food.p100,
            c100 = food.c100,
            f100 = food.f100,
        )
    }
}

data class MealTotals(
    val kcal: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val proteinPct: Int,
    val carbsPct: Int,
    val fatPct: Int,
)

object Nutrition {

    fun totals(list: List<Ingredient>): MealTotals {
        val p = list.sumOf { it.proteinG }
        val c = list.sumOf { it.carbsG }
        val f = list.sumOf { it.fatG }
        val fromMacros = p * 4 + c * 4 + f * 9
        fun pct(v: Double) = if (fromMacros > 0) (v / fromMacros * 100).roundToInt() else 0
        return MealTotals(
            kcal = list.sumOf { it.kcal }.roundToInt(),
            proteinG = p.roundToInt(),
            carbsG = c.roundToInt(),
            fatG = f.roundToInt(),
            proteinPct = pct(p * 4),
            carbsPct = pct(c * 4),
            fatPct = pct(f * 9),
        )
    }

    fun dayShare(kcal: Int, dayGoal: Int = 1800): Int = (kcal * 100.0 / dayGoal).roundToInt()
}

data class SavedMeal(
    val id: Long,
    val reminderId: Long,
    val label: String,
    val date: LocalDate,
    val ingredients: List<Ingredient>,
)
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "dev.viniciuscole.nudge.data.model.NutritionTest"`
Expected: `BUILD SUCCESSFUL`, 4 tests passed.

- [ ] **Step 5: Meal JSON codec**

`app/src/main/java/dev/viniciuscole/nudge/data/json/MealJson.kt`:
```kotlin
package dev.viniciuscole.nudge.data.json

import dev.viniciuscole.nudge.data.model.Ingredient
import dev.viniciuscole.nudge.data.model.SavedMeal
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

object MealJson {

    fun encode(list: List<SavedMeal>): String {
        val arr = JSONArray()
        list.forEach { arr.put(mealToJson(it)) }
        return arr.toString()
    }

    fun decode(raw: String?): List<SavedMeal> {
        if (raw.isNullOrBlank()) return emptyList()
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { mealFromJson(arr.getJSONObject(it)) }
    }

    private fun mealToJson(m: SavedMeal): JSONObject = JSONObject()
        .put("id", m.id)
        .put("reminderId", m.reminderId)
        .put("label", m.label)
        .put("date", m.date.toString())
        .put("ingredients", JSONArray().also { arr -> m.ingredients.forEach { arr.put(ingredientToJson(it)) } })

    private fun mealFromJson(o: JSONObject): SavedMeal {
        val ing = o.optJSONArray("ingredients") ?: JSONArray()
        return SavedMeal(
            id = o.getLong("id"),
            reminderId = o.optLong("reminderId", -1L),
            label = o.optString("label", ""),
            date = LocalDate.parse(o.getString("date")),
            ingredients = (0 until ing.length()).map { ingredientFromJson(ing.getJSONObject(it)) },
        )
    }

    private fun ingredientToJson(i: Ingredient): JSONObject = JSONObject()
        .put("id", i.id)
        .put("name", i.name)
        .put("unit", i.unit)
        .put("qty", i.qty)
        .put("kcal100", i.kcal100)
        .put("p100", i.p100)
        .put("c100", i.c100)
        .put("f100", i.f100)

    private fun ingredientFromJson(o: JSONObject): Ingredient = Ingredient(
        id = o.getLong("id"),
        name = o.getString("name"),
        unit = o.optString("unit", "g"),
        qty = o.optInt("qty", 100),
        kcal100 = o.optDouble("kcal100", 0.0),
        p100 = o.optDouble("p100", 0.0),
        c100 = o.optDouble("c100", 0.0),
        f100 = o.optDouble("f100", 0.0),
    )
}
```

- [ ] **Step 6: Repository**

`app/src/main/java/dev/viniciuscole/nudge/data/MealRepository.kt`:
```kotlin
package dev.viniciuscole.nudge.data

import android.content.Context
import dev.viniciuscole.nudge.data.json.MealJson
import dev.viniciuscole.nudge.data.model.SavedMeal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

class MealRepository(context: Context) {

    private val prefs = context.getSharedPreferences("meals", Context.MODE_PRIVATE)
    private val _meals = MutableStateFlow(MealJson.decode(prefs.getString(KEY, null)))
    val meals: StateFlow<List<SavedMeal>> = _meals.asStateFlow()

    fun nextId(): Long = (_meals.value.maxOfOrNull { it.id } ?: 0L) + 1

    fun forReminderOn(reminderId: Long, date: LocalDate): SavedMeal? =
        _meals.value.lastOrNull { it.reminderId == reminderId && it.date == date }

    fun save(meal: SavedMeal) {
        val cur = _meals.value
        write(if (cur.any { it.id == meal.id }) cur.map { if (it.id == meal.id) meal else it } else cur + meal)
    }

    private fun write(list: List<SavedMeal>) {
        _meals.value = list
        prefs.edit().putString(KEY, MealJson.encode(list)).apply()
    }

    private companion object {
        const val KEY = "list"
    }
}
```

- [ ] **Step 7: Register in NudgeApp**

Add to `NudgeApp.kt`:
```kotlin
val meals: MealRepository by lazy { MealRepository(this) }
```
Import `dev.viniciuscole.nudge.data.MealRepository`.

`NudgeApp.kt` is now complete:
```kotlin
package dev.viniciuscole.nudge

import android.app.Application
import android.content.Context
import dev.viniciuscole.nudge.alarm.AlarmScheduler
import dev.viniciuscole.nudge.alarm.Notifications
import dev.viniciuscole.nudge.data.DayStatsRepository
import dev.viniciuscole.nudge.data.MealRepository
import dev.viniciuscole.nudge.data.ReminderRepository
import dev.viniciuscole.nudge.data.food.FoodSearch
import dev.viniciuscole.nudge.data.food.UsdaClient

class NudgeApp : Application() {

    val reminders: ReminderRepository by lazy { ReminderRepository(this) }
    val scheduler: AlarmScheduler by lazy { AlarmScheduler(this) }
    val stats: DayStatsRepository by lazy { DayStatsRepository(this) }
    val meals: MealRepository by lazy { MealRepository(this) }
    val foodSearch: FoodSearch by lazy { FoodSearch(UsdaClient(BuildConfig.USDA_API_KEY)) }

    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
    }

    companion object {
        fun from(context: Context): NudgeApp = context.applicationContext as NudgeApp
    }
}
```

- [ ] **Step 8: Build and commit**

Run: `./gradlew assembleDebug testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`.

```bash
git add -A
git commit -m "feat: meal model, nutrition totals, meal persistence"
```

---

### Task 12: Meal builder screen (design 2a/2b) — search, quantities, totals, save

**Files:**
- Create: `app/src/main/java/dev/viniciuscole/nudge/ui/builder/MealBuilderViewModel.kt`
- Create: `app/src/main/java/dev/viniciuscole/nudge/ui/builder/MealBuilderScreen.kt`
- Modify: `app/src/main/java/dev/viniciuscole/nudge/ui/NudgeNavHost.kt`, `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `FoodSearch`, `FoodItem` (Task 10), `Ingredient`, `Nutrition`, `MealTotals`, `SavedMeal`, `MealRepository` (Task 11), `ReminderRepository`, `ReminderFormat`, primitives.
- Produces: `MealBuilderViewModel(app, reminderId)` with `state: StateFlow<BuilderUiState>`, `setQuery`, `clearQuery`, `add(food)`, `setQty(id, text)`, `plus(id)`, `minus(id)`, `remove(id)`, `save()`; `MealBuilderScreen(vm, onBack)`.

- [ ] **Step 1: Strings**

Add to `values/strings.xml`:
```xml
    <string name="builder_subtitle">%1$s · meal</string>
    <string name="search_hint">Search ingredient…</string>
    <string name="no_match">No match.</string>
    <string name="per_100">%1$d kcal / 100 %2$s</string>
    <plurals name="ingredient_count">
        <item quantity="one">%1$d ingredient</item>
        <item quantity="other">%1$d ingredients</item>
    </plurals>
    <string name="empty_ingredients_title">Add your first ingredient</string>
    <string name="empty_ingredients_body">Search above and tap to add. Calories and macros add up as you go.</string>
    <string name="meal_total">Meal total</string>
    <string name="kcal">kcal</string>
    <string name="kcal_caps">KCAL</string>
    <string name="goal_note">≈ %1$d%% of a 1,800 kcal day</string>
    <string name="protein_g">Protein %1$d g</string>
    <string name="carbs_g">Carbs %1$d g</string>
    <string name="fat_g">Fat %1$d g</string>
    <string name="save_meal">Save meal</string>
    <string name="meal_saved">Meal saved</string>
    <string name="clear">Clear</string>
    <string name="remove">Remove</string>
    <string name="add">Add</string>
```

- [ ] **Step 2: ViewModel (debounced search, quantity edits, totals, save)**

`app/src/main/java/dev/viniciuscole/nudge/ui/builder/MealBuilderViewModel.kt`:
```kotlin
package dev.viniciuscole.nudge.ui.builder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.viniciuscole.nudge.NudgeApp
import dev.viniciuscole.nudge.data.food.FoodItem
import dev.viniciuscole.nudge.data.model.Ingredient
import dev.viniciuscole.nudge.data.model.MealTotals
import dev.viniciuscole.nudge.data.model.Nutrition
import dev.viniciuscole.nudge.data.model.Reminder
import dev.viniciuscole.nudge.data.model.SavedMeal
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import java.time.LocalDate

data class BuilderUiState(
    val reminder: Reminder? = null,
    val query: String = "",
    val suggestions: List<FoodItem> = emptyList(),
    val searching: Boolean = false,
    val ingredients: List<Ingredient> = emptyList(),
    val totals: MealTotals = Nutrition.totals(emptyList()),
    val saved: Boolean = false,
) {
    val hasQuery: Boolean get() = query.isNotBlank()
    val noResults: Boolean get() = hasQuery && !searching && suggestions.isEmpty()
    val dayShare: Int get() = Nutrition.dayShare(totals.kcal)
}

@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MealBuilderViewModel(private val app: NudgeApp, private val reminderId: Long) : ViewModel() {

    private val _state = MutableStateFlow(BuilderUiState())
    val state: StateFlow<BuilderUiState> = _state
    private val query = MutableStateFlow("")
    private var nextIngredientId = 1L

    init {
        val reminder = app.reminders.get(reminderId)
        val existing = app.meals.forReminderOn(reminderId, LocalDate.now())
        val ingredients = existing?.ingredients ?: emptyList()
        nextIngredientId = (ingredients.maxOfOrNull { it.id } ?: 0L) + 1
        _state.value = BuilderUiState(reminder = reminder, ingredients = ingredients, totals = Nutrition.totals(ingredients))

        query
            .debounce(350)
            .distinctUntilChanged()
            .onEach { q -> _state.update { it.copy(searching = q.isNotBlank(), suggestions = if (q.isBlank()) emptyList() else it.suggestions) } }
            .mapLatest { q -> if (q.isBlank()) emptyList() else app.foodSearch.search(q) }
            .onEach { results -> _state.update { it.copy(suggestions = results, searching = false) } }
            .launchIn(viewModelScope)
    }

    fun setQuery(q: String) {
        query.value = q
        _state.update { it.copy(query = q, saved = false) }
    }

    fun clearQuery() = setQuery("")

    fun add(food: FoodItem) {
        val ingredient = Ingredient.from(food, nextIngredientId++)
        updateIngredients { it + ingredient }
        clearQuery()
    }

    fun setQty(id: Long, text: String) {
        val v = text.filter(Char::isDigit).take(4).toIntOrNull() ?: 0
        updateIngredients { list -> list.map { if (it.id == id) it.copy(qty = v.coerceIn(0, 2000)) else it } }
    }

    fun plus(id: Long) = updateIngredients { list -> list.map { if (it.id == id) it.copy(qty = (it.qty + it.step).coerceAtMost(2000)) else it } }

    fun minus(id: Long) = updateIngredients { list -> list.map { if (it.id == id) it.copy(qty = (it.qty - it.step).coerceAtLeast(0)) else it } }

    fun remove(id: Long) = updateIngredients { list -> list.filter { it.id != id } }

    fun save() {
        val s = _state.value
        val existing = app.meals.forReminderOn(reminderId, LocalDate.now())
        app.meals.save(
            SavedMeal(
                id = existing?.id ?: app.meals.nextId(),
                reminderId = reminderId,
                label = s.reminder?.label ?: "",
                date = LocalDate.now(),
                ingredients = s.ingredients,
            ),
        )
        _state.update { it.copy(saved = true) }
    }

    private fun updateIngredients(f: (List<Ingredient>) -> List<Ingredient>) {
        _state.update { val list = f(it.ingredients); it.copy(ingredients = list, totals = Nutrition.totals(list), saved = false) }
    }
}
```

- [ ] **Step 3: Screen**

`app/src/main/java/dev/viniciuscole/nudge/ui/builder/MealBuilderScreen.kt`:
```kotlin
package dev.viniciuscole.nudge.ui.builder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.viniciuscole.nudge.R
import dev.viniciuscole.nudge.data.food.FoodItem
import dev.viniciuscole.nudge.data.model.Ingredient
import dev.viniciuscole.nudge.data.model.MealTotals
import dev.viniciuscole.nudge.ui.components.Card24
import dev.viniciuscole.nudge.ui.components.CircleIconButton
import dev.viniciuscole.nudge.ui.components.CircleTextButton
import dev.viniciuscole.nudge.ui.components.PillButton
import dev.viniciuscole.nudge.ui.components.SectionLabel
import dev.viniciuscole.nudge.ui.format.ReminderFormat
import dev.viniciuscole.nudge.ui.theme.NudgeTheme
import dev.viniciuscole.nudge.ui.theme.manrope
import kotlin.math.roundToInt

@Composable
fun MealBuilderScreen(vm: MealBuilderViewModel, onBack: () -> Unit) {
    val c = NudgeTheme.colors
    val s by vm.state.collectAsStateWithLifecycle()
    val reminder = s.reminder

    Column(Modifier.fillMaxSize().background(c.bg).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CircleIconButton(R.drawable.ic_back, onClick = onBack, contentDescription = stringResource(R.string.back))
            Column {
                Text(reminder?.label ?: "", style = manrope(19.sp, FontWeight.Bold), color = c.ink)
                if (reminder != null) {
                    Text(
                        stringResource(R.string.builder_subtitle, ReminderFormat.time(reminder.hour, reminder.minute)),
                        style = manrope(12.5.sp, FontWeight.Medium), color = c.faint, modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }

        Box(Modifier.fillMaxWidth().zIndex(4f).padding(start = 20.dp, end = 20.dp, top = 12.dp)) {
            SearchField(s.query, s.hasQuery, vm::setQuery, vm::clearQuery)
            if (s.hasQuery) {
                SuggestionsDropdown(
                    suggestions = s.suggestions,
                    noResults = s.noResults,
                    onAdd = vm::add,
                    modifier = Modifier.align(Alignment.TopCenter).offset(y = 60.dp),
                )
            }
        }

        if (s.ingredients.isEmpty()) {
            EmptyIngredients(Modifier.weight(1f))
        } else {
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item { SectionLabel(pluralStringResource(R.plurals.ingredient_count, s.ingredients.size, s.ingredients.size)) }
                items(s.ingredients, key = { it.id }) { ing ->
                    IngredientRow(ing, onQty = { vm.setQty(ing.id, it) }, onPlus = { vm.plus(ing.id) }, onMinus = { vm.minus(ing.id) }, onRemove = { vm.remove(ing.id) })
                }
            }
        }

        Column(Modifier.fillMaxWidth().background(c.bg).navigationBarsPadding().imePadding().padding(start = 20.dp, end = 20.dp, bottom = 20.dp)) {
            TotalsCard(s.totals, s.dayShare)
            PillButton(
                text = stringResource(if (s.saved) R.string.meal_saved else R.string.save_meal),
                onClick = { vm.save() },
                modifier = Modifier.padding(top = 12.dp),
                color = if (s.saved) c.teal else c.coral,
            )
        }
    }
}

@Composable
private fun SearchField(query: String, hasQuery: Boolean, onQuery: (String) -> Unit, onClear: () -> Unit) {
    val c = NudgeTheme.colors
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .shadow(2.dp, shape, ambientColor = c.ink.copy(alpha = .04f), spotColor = c.ink.copy(alpha = .06f))
            .clip(shape)
            .background(c.card)
            .border(1.5.dp, if (hasQuery) c.coral else c.line, shape)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(painterResource(R.drawable.ic_search), null, tint = if (hasQuery) c.coral else c.faint, modifier = Modifier.size(20.dp))
        BasicTextField(
            value = query,
            onValueChange = onQuery,
            singleLine = true,
            textStyle = manrope(15.5.sp, FontWeight.SemiBold).copy(color = c.ink),
            cursorBrush = SolidColor(c.coral),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (query.isEmpty()) Text(stringResource(R.string.search_hint), style = manrope(15.5.sp, FontWeight.SemiBold), color = c.faint)
                inner()
            },
        )
        if (hasQuery) {
            CircleIconButton(R.drawable.ic_close, onClick = onClear, size = 24.dp, bg = c.chip, tint = c.muted, iconSize = 12.dp, contentDescription = stringResource(R.string.clear))
        }
    }
}

@Composable
private fun SuggestionsDropdown(suggestions: List<FoodItem>, noResults: Boolean, onAdd: (FoodItem) -> Unit, modifier: Modifier) {
    val c = NudgeTheme.colors
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier
            .fillMaxWidth()
            .shadow(16.dp, shape, ambientColor = c.ink.copy(alpha = .16f), spotColor = c.ink.copy(alpha = .16f))
            .clip(shape)
            .background(c.card)
            .padding(6.dp),
    ) {
        suggestions.forEach { f ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { onAdd(f) }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.size(34.dp).clip(RoundedCornerShape(12.dp)).background(c.coralContainer), contentAlignment = Alignment.Center) {
                    Text(f.initials, style = manrope(12.5.sp, FontWeight.Bold), color = c.coralDeep)
                }
                Column(Modifier.weight(1f)) {
                    Text(f.name, style = manrope(14.5.sp, FontWeight.Bold), color = c.ink, maxLines = 1)
                    Text(stringResource(R.string.per_100, f.kcal100.roundToInt(), f.unit), style = manrope(12.5.sp, FontWeight.Medium), color = c.faint, modifier = Modifier.padding(top = 1.dp))
                }
                Icon(painterResource(R.drawable.ic_plus), stringResource(R.string.add), tint = c.coral, modifier = Modifier.size(18.dp))
            }
        }
        if (noResults) {
            Text(stringResource(R.string.no_match), style = manrope(13.5.sp, FontWeight.Medium), color = c.faint, modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp))
        }
    }
}

@Composable
private fun IngredientRow(ing: Ingredient, onQty: (String) -> Unit, onPlus: () -> Unit, onMinus: () -> Unit, onRemove: () -> Unit) {
    val c = NudgeTheme.colors
    Card24(Modifier.fillMaxWidth(), radius = 20.dp) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(ing.name, style = manrope(15.5.sp, FontWeight.Bold), color = c.ink, maxLines = 1)
                Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        Modifier.clip(RoundedCornerShape(10.dp)).background(c.field).padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        BasicTextField(
                            value = ing.qty.toString(),
                            onValueChange = onQty,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = manrope(14.sp, FontWeight.Bold).copy(color = c.ink, textAlign = TextAlign.End),
                            cursorBrush = SolidColor(c.coral),
                            modifier = Modifier.width(38.dp),
                        )
                        Text(ing.unit, style = manrope(12.5.sp, FontWeight.SemiBold), color = c.muted)
                    }
                    CircleTextButton("−", onClick = onMinus)
                    CircleTextButton("+", onClick = onPlus)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(ing.kcal.roundToInt().toString(), style = manrope(16.sp, FontWeight.ExtraBold), color = c.ink)
                Text(stringResource(R.string.kcal_caps), style = manrope(11.sp, FontWeight.SemiBold, letterSpacing = .7.sp), color = c.faint, modifier = Modifier.padding(top = 4.dp))
            }
            CircleIconButton(R.drawable.ic_close, onClick = onRemove, size = 34.dp, tint = c.line, iconSize = 16.dp, contentDescription = stringResource(R.string.remove))
        }
    }
}

@Composable
private fun EmptyIngredients(modifier: Modifier) {
    val c = NudgeTheme.colors
    Column(modifier.fillMaxWidth().padding(horizontal = 46.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(96.dp).clip(RoundedCornerShape(32.dp)).background(c.chip), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_fork), null, tint = c.faint, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.empty_ingredients_title), style = manrope(20.sp, FontWeight.ExtraBold), color = c.ink, textAlign = TextAlign.Center)
        Text(
            stringResource(R.string.empty_ingredients_body),
            style = manrope(14.5.sp, FontWeight.Medium, lineHeight = 21.sp),
            color = c.muted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun TotalsCard(t: MealTotals, dayShare: Int) {
    val c = NudgeTheme.colors
    Card24(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    SectionLabel(stringResource(R.string.meal_total))
                    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 6.dp)) {
                        Text(t.kcal.toString(), style = manrope(40.sp, FontWeight.ExtraBold, letterSpacing = (-0.8).sp), color = c.ink)
                        Text(" " + stringResource(R.string.kcal), style = manrope(14.sp, FontWeight.Bold), color = c.muted, modifier = Modifier.padding(bottom = 6.dp))
                    }
                }
                Text(stringResource(R.string.goal_note, dayShare), style = manrope(12.5.sp, FontWeight.SemiBold), color = c.faint, textAlign = TextAlign.End, modifier = Modifier.padding(bottom = 4.dp))
            }
            Row(Modifier.fillMaxWidth().padding(top = 14.dp).height(8.dp).clip(RoundedCornerShape(5.dp)).background(c.chip)) {
                if (t.proteinPct > 0) Box(Modifier.weight(t.proteinPct.toFloat()).fillMaxSize().background(c.coral))
                if (t.carbsPct > 0) Box(Modifier.weight(t.carbsPct.toFloat()).fillMaxSize().background(c.carbs))
                if (t.fatPct > 0) Box(Modifier.weight(t.fatPct.toFloat()).fillMaxSize().background(c.teal))
            }
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Legend(c.coral, stringResource(R.string.protein_g, t.proteinG))
                Legend(c.carbs, stringResource(R.string.carbs_g, t.carbsG))
                Legend(c.teal, stringResource(R.string.fat_g, t.fatG))
            }
        }
    }
}

@Composable
private fun Legend(color: Color, text: String) {
    val c = NudgeTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(Modifier.size(8.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Text(text, style = manrope(12.5.sp, FontWeight.SemiBold), color = c.body)
    }
}
```

- [ ] **Step 4: Wire into the NavHost**

In `NudgeNavHost.kt` replace the `composable(Routes.BUILDER, ...) { ... }` block with:
```kotlin
        composable(
            Routes.BUILDER,
            arguments = listOf(navArgument("reminderId") { type = NavType.LongType }),
        ) { entry ->
            val reminderId = entry.arguments?.getLong("reminderId") ?: -1L
            val vm: MealBuilderViewModel = viewModel(
                key = "builder-$reminderId",
                factory = viewModelFactory { initializer { MealBuilderViewModel(app, reminderId) } },
            )
            MealBuilderScreen(vm = vm, onBack = { nav.popBackStack() })
        }
```
Imports: `dev.viniciuscole.nudge.ui.builder.MealBuilderScreen`, `dev.viniciuscole.nudge.ui.builder.MealBuilderViewModel`.

- [ ] **Step 5: Build**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Manual check on a device**

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
1. On Home, tap a meal card (e.g. "Lunch") → builder opens with the header "Lunch / 12:30 · meal" and the empty-ingredients state.
2. Type `chick` → dropdown lists "Chicken breast", "Chicken thigh", "Chickpeas, cooked" with "165 kcal / 100 g" etc. (design 2b). Tap one → it appears as a row with qty 100 g.
3. Tap `+` → qty 110, kcal updates; type `150` in the field → 248 kcal. Footer shows the total, the macro bar and "Protein/Carbs/Fat … g".
4. Tap the × on a row → it disappears and totals update.
5. Tap "Save meal" → button turns teal and reads "Meal saved". Leave with back, reopen the same card → the ingredients are restored.
6. With a USDA key in `local.properties` (rebuild first), search `quinoa` → remote results appear below any local matches.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat: ingredient-based meal builder with live totals and USDA search"
```

---

### Task 13: pt-BR strings, adaptive launcher icon, README, final verification

**Files:**
- Create: `app/src/main/res/values-pt-rBR/strings.xml`
- Create: `app/src/main/res/drawable/ic_launcher_foreground.xml`, `app/src/main/res/values/ic_launcher_background.xml`, `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- Create: `README.md`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes: every string key introduced in Tasks 6–12 (all must have a pt-BR entry — the list below is the complete key set).

- [ ] **Step 1: Brazilian Portuguese strings**

`app/src/main/res/values-pt-rBR/strings.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Nudge</string>

    <string name="channel_alarms">Alarmes</string>
    <string name="channel_alarms_desc">Lembretes de refeição e água</string>
    <string name="ring_sub_meal">Tocando · vibrando</string>
    <string name="ring_sub_water">Copo %1$d de %2$d · tocando</string>
    <string name="action_done">Concluído</string>
    <string name="action_snooze">Adiar 5 min</string>
    <string name="reminder">Lembrete</string>

    <string name="home_title">Seu dia</string>
    <string name="water">Água</string>
    <string name="meals">Refeições</string>
    <string name="stat_glasses">/ %1$d copos</string>
    <string name="stat_on_time">/ %1$d no horário</string>
    <string name="reminders">Lembretes</string>
    <string name="schedule_daily">%1$s todo dia</string>
    <string name="schedule_water">%1$s–%2$s, a cada %3$s</string>
    <string name="schedule_off_suffix">· desligado</string>
    <string name="empty_title">Nenhum lembrete ainda</string>
    <string name="empty_body">Adicione seus horários de refeição e uma janela de hidratação. O Nudge toca até você dispensar.</string>
    <string name="add_first_reminder">Adicionar primeiro lembrete</string>
    <string name="add_reminder">Adicionar lembrete</string>
    <string name="delete">Excluir</string>
    <string name="perm_fullscreen_title">Permitir alarmes em tela cheia</string>
    <string name="perm_fullscreen_body">Necessário para tocar sobre a tela de bloqueio.</string>
    <string name="perm_exact_title">Permitir alarmes exatos</string>
    <string name="perm_exact_body">Sem isso os lembretes podem atrasar.</string>
    <string name="perm_notifications_title">Permitir notificações</string>
    <string name="perm_notifications_body">Os lembretes chegam como notificações de alarme.</string>
    <string name="perm_fix">Corrigir</string>
    <string name="interval_minutes">%1$d min</string>
    <string name="interval_hours">%1$dh</string>
    <string name="interval_hours_minutes">%1$dh%2$02d</string>

    <string name="new_reminder">Novo lembrete</string>
    <string name="type_meal">Refeição</string>
    <string name="type_water">Água</string>
    <string name="label">Nome</string>
    <string name="label_hint">ex.: Almoço</string>
    <string name="time">Horário</string>
    <string name="start">Início</string>
    <string name="end">Fim</string>
    <string name="every">A cada</string>
    <string name="custom_interval">Personalizado</string>
    <string name="custom_interval_hint">Minutos</string>
    <string name="insistent_title">Alarme insistente</string>
    <string name="insistent_body">Toca e vibra até ser dispensado</string>
    <string name="save_reminder">Salvar lembrete</string>
    <string name="default_meal_label">Refeição</string>
    <string name="default_water_label">Beber água</string>
    <string name="back">Voltar</string>
    <string name="ok">OK</string>
    <string name="cancel">Cancelar</string>

    <string name="builder_subtitle">%1$s · refeição</string>
    <string name="search_hint">Buscar ingrediente…</string>
    <string name="no_match">Nada encontrado.</string>
    <string name="per_100">%1$d kcal / 100 %2$s</string>
    <plurals name="ingredient_count">
        <item quantity="one">%1$d ingrediente</item>
        <item quantity="other">%1$d ingredientes</item>
    </plurals>
    <string name="empty_ingredients_title">Adicione o primeiro ingrediente</string>
    <string name="empty_ingredients_body">Busque acima e toque para adicionar. Calorias e macros somam conforme você monta.</string>
    <string name="meal_total">Total da refeição</string>
    <string name="kcal">kcal</string>
    <string name="kcal_caps">KCAL</string>
    <string name="goal_note">≈ %1$d%% de um dia de 1.800 kcal</string>
    <string name="protein_g">Proteína %1$d g</string>
    <string name="carbs_g">Carbo %1$d g</string>
    <string name="fat_g">Gordura %1$d g</string>
    <string name="save_meal">Salvar refeição</string>
    <string name="meal_saved">Refeição salva</string>
    <string name="clear">Limpar</string>
    <string name="remove">Remover</string>
    <string name="add">Adicionar</string>
</resources>
```

Sanity check that every default key is translated:
```bash
diff <(grep -oE 'name="[a-z_]+"' app/src/main/res/values/strings.xml | sort) \
     <(grep -oE 'name="[a-z_]+"' app/src/main/res/values-pt-rBR/strings.xml | sort)
```
Expected: no output.

- [ ] **Step 2: Adaptive launcher icon (coral background, white fork mark)**

`app/src/main/res/values/ic_launcher_background.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="ic_launcher_background">#E9704B</color>
</resources>
```

`app/src/main/res/drawable/ic_launcher_foreground.xml` (108dp canvas, artwork inside the 72dp safe zone):
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
    <group android:scaleX="2.2" android:scaleY="2.2" android:translateX="27.6" android:translateY="27.6">
        <path android:pathData="M7 3v7a2.5 2.5 0 0 0 5 0V3" android:strokeColor="#FFFFFF" android:strokeWidth="1.8" android:strokeLineCap="round" android:fillColor="#00000000"/>
        <path android:pathData="M9.5 10v11" android:strokeColor="#FFFFFF" android:strokeWidth="1.8" android:strokeLineCap="round" android:fillColor="#00000000"/>
        <path android:pathData="M16.5 3v18" android:strokeColor="#FFFFFF" android:strokeWidth="1.8" android:strokeLineCap="round" android:fillColor="#00000000"/>
        <path android:pathData="M16.5 3c1.8 0 2.6 1.4 2.6 3.4S18.3 10 16.5 10" android:strokeColor="#FFFFFF" android:strokeWidth="1.8" android:strokeLineCap="round" android:fillColor="#00000000"/>
    </group>
</vector>
```

`app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
```

Manifest — add to the `<application>` element:
```xml
        android:icon="@mipmap/ic_launcher"
```

`minSdk` is 26, so the adaptive icon alone is sufficient (no legacy PNG mipmaps needed).

- [ ] **Step 3: README**

`README.md`:
```markdown
# Nudge

Personal Android app: alarm-style reminders for meals and water (rings and vibrates until you dismiss), plus an ingredient-based meal builder with calories and macros.

## Build (WSL2 / Linux, no Android Studio)

Requirements: JDK 17, Android SDK with `platforms;android-35` and `build-tools;35.0.0` (see `docs/superpowers/plans/2026-09-21-nudge-android-app.md`, Task 1).

```bash
cp local.properties.example local.properties   # then set sdk.dir and, optionally, USDA_API_KEY
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

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
```

Also create `local.properties.example` (committed) so the README instruction works:
```properties
sdk.dir=/home/you/Android/Sdk
USDA_API_KEY=
```

- [ ] **Step 4: Final verification**

Run:
```bash
./gradlew clean assembleDebug testDebugUnitTest
```
Expected: `BUILD SUCCESSFUL`; unit test report at `app/build/reports/tests/testDebugUnitTest/index.html` shows 17 tests, 0 failures (3 ReminderJson + 7 NextFire + 3 UsdaClient + 4 Nutrition).

Then on the phone (`adb install -r ...`):
1. Switch the system language to Português (Brasil) → Home reads "Seu dia", the alarm screen buttons read "Concluído" / "Adiar 5 min".
2. Switch dark mode on → Home matches design 1c (dark cards `#211C19`, teal toggles `#3ABDB8`).
3. The launcher shows the coral icon with the white fork mark.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: pt-BR translation, adaptive icon, README"
```

---

## Decisions that differ from the original written spec (and why)

| Spec said | Plan does | Why |
|---|---|---|
| XML Views + ViewBinding + RecyclerView + ConstraintLayout | Jetpack Compose | The design is 100% custom (Manrope, pill segmented control, custom toggles, pulsing rings, macro bar). In Compose each is a 10–30 line composable; in Views each needs a layout + adapter/custom drawable. Compose is also the Android default since 2023. |
| Groovy DSL | Kotlin DSL + version catalog | Current Android Studio default; typed, refactor-safe. |
| targetSdk 34 | targetSdk 35 | Required by Play for new apps since Aug 2025; `enableEdgeToEdge()` handles the 35 edge-to-edge enforcement. |
| `SCHEDULE_EXACT_ALARM` + runtime request | `USE_EXACT_ALARM` | On Android 14 `SCHEDULE_EXACT_ALARM` is denied by default and revocable; `USE_EXACT_ALARM` is auto-granted to alarm-clock apps. The runtime settings deep-link is still shown as a banner if the check ever fails. |
| `setExactAndAllowWhileIdle` | `setAlarmClock` | Both are exact; `setAlarmClock` is fully Doze-exempt (no 9-minute throttling) and semantically an alarm clock. Falls back to `setAndAllowWhileIdle` if exact alarms are unavailable. |
| Sound + vibration in `AlarmActivity` | In a foreground `AlarmRingingService` | When the phone is unlocked and in use, Android shows a heads-up notification instead of the full-screen Activity — the alarm would be silent. The service rings regardless; the Activity is just a view. |
| No automated tests | 17 unit tests on pure logic only | They cover the four places bugs hide (next-fire math, JSON codec, USDA parsing, macro math), run headless in WSL in seconds, and cost nothing on the UI side. No UI/instrumented tests. |
| Interval as numeric EditText | Preset chips (30 min/1h/1h30/2h) + "Custom" chip revealing a numeric field | Matches the design; keeps the free-form entry. |
| — | "Insistent alarm" per-reminder toggle | It's in the design; when off, a normal high-priority notification is posted without looping sound. |
| — | Daily stats (glasses / meals on time), swipe-to-delete, long-press alarm preview | Stats are in the design header; delete is table stakes; preview is how you test the ring chain without waiting. |
| — | English default + pt-BR | Design copy is English; user's spec copy is Portuguese. Both ship. |

Out of scope for v1 (explicitly): editing an existing reminder (delete + re-add), "add as custom ingredient" from the no-match state, weekly history.

## Self-review

**Spec coverage.** Design 1a home → Task 8; 1a add form → Task 9; 1a ring overlay + 1b lock-screen → Tasks 6–7; 1c dark → Task 2 palette + Task 8; 1d empty → Task 8; 2a builder → Tasks 10–12; 2b search open → Task 12 (`SearchField` + `SuggestionsDropdown`). Written spec: AlarmManager exact + self-reschedule → Task 5; full-screen intent + AlarmActivity over lock screen → Tasks 6–7; loop sound + vibration until Done/Snooze → Task 6; BootReceiver → Task 5; permissions → Tasks 5, 8; USDA via `local.properties` → Tasks 1, 10; ingredient list with editable qty, kcal, remove → Task 12; footer total + macro badges → Task 12; Save meal → Tasks 11–12; Material 3 / rounded / teal-water / coral-meal → Task 2; `./gradlew assembleDebug` passes → every task.

**Type consistency.** `NudgeApp` members `reminders`, `scheduler`, `stats`, `meals`, `foodSearch` are introduced in Tasks 3, 5, 6, 11, 10 and match the consolidated file in Task 11 Step 7. `AlarmRingingService.intent(context, id, action)` / `start(context, id)` are used identically in Tasks 6, 7, 8. `Notifications.subtitle(context, r)` is defined in Task 6 and used in Task 7. `ReminderFormat.time/hourOnly/interval/schedule` (Task 8) are used in Tasks 9 and 12. `Ingredient.from(food, id)` and `Ingredient.step` (Task 11) are used in Task 12. Route constants live in `Routes` (Task 8) and are extended in Tasks 9 and 12.
