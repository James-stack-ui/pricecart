# PriceCart Agent Guide

**Project Type:** Android Native Application (Kotlin + Gradle)  
**Min SDK:** 24 | **Target SDK:** 35 | **Compile SDK:** 35

## Architecture Overview

PriceCart is an early-stage Android app (v1.0) with a single-module structure:
- **Module:** `:app` - Contains all source code, resources, and tests
- **Language:** Kotlin exclusively
- **Build System:** Gradle with version catalog (`gradle/libs.versions.toml`)
- **Empty Java source tree:** `app/src/main/java/com/example/pricecart/` - development stage

## Build & Development Workflow

### Essential Commands
```bash
# Build debug APK
./gradlew build

# Run instrumented tests on device/emulator
./gradlew connectedAndroidTest

# Run unit tests locally
./gradlew test

# Sync Gradle (required after dependency changes)
./gradlew sync
```

### Build Configuration
- **API Compatibility:** Java 11 (`compileOptions.sourceCompatibility`)
- **Dependency Management:** Centralized in `gradle/libs.versions.toml`
  - Uses version aliases: `android-application`, `kotlin-android` plugins
  - Core dependencies: `androidx.core.ktx`, `androidx.appcompat`, `material`
- **ProGuard:** Disabled in release builds (`isMinifyEnabled = false`)

## Code Organization Patterns

### Package Structure
```
com.example.pricecart
  └── (Currently empty; new features added here)
```

### Resource Organization
```
app/src/main/res/
  ├── values/strings.xml          # App strings & localization
  ├── values/themes.xml           # Material 3 theme (DarkActionBar variant)
  ├── values/colors.xml           # Color palette
  ├── values-night/themes.xml     # Dark mode theme
  ├── mipmap-*/                   # Launcher icons (WebP format)
  └── xml/                        # Data extraction & backup rules
```

**Theme Convention:** Material Design (Theme.MaterialComponents.DayNight.DarkActionBar)  
Primary colors: Purple 500/700 | Secondary: Teal 200/700

## Testing Framework

### Test Structure
- **Unit Tests:** `app/src/test/java/` (JUnit 4)
- **Instrumented Tests:** `app/src/androidTest/java/` (AndroidJUnit4 + Espresso)

### Key Dependencies
```gradle
junit = "4.13.2"
androidx.junit = "1.3.0"
androidx.espresso.core = "3.7.0"
```

**Pattern:** Test classes inherit test names with examples (ExampleUnitTest, ExampleInstrumentedTest)

## Manifest & Configuration

### AndroidManifest.xml Defaults
```xml
<application
  android:allowBackup="true"
  android:theme="@style/Theme.PriceCart"
  android:icon="@mipmap/ic_launcher"
  android:roundIcon="@mipmap/ic_launcher_round"
  android:supportsRtl="true"
  android:fullBackupContent="@xml/backup_rules"
  android:dataExtractionRules="@xml/data_extraction_rules"
/>
```
- **No Activities defined yet** - app is in scaffolding phase
- **Data Backup:** Configured via XML rules files

### Gradle Properties
- `android.useAndroidX=true` - Forces AndroidX libraries
- `kotlin.code.style=official` - Official Kotlin code style
- `android.nonTransitiveRClass=true` - Namespaced R class (reduces APK size)
- JVM Args: `-Xmx2048m` (2GB max heap for Gradle daemon)

## Development Guidelines for Agents

1. **Kotlin Only:** All new code must be in Kotlin, not Java
2. **Namespace:** All new classes use `com.example.pricecart` package
3. **Dependencies:** Add to `libs.versions.toml` version table first, then reference via `libs.*` aliases
4. **Testing:** Pair new features with unit tests (`src/test`) before instrumented tests
5. **Theme Extension:** Material 3 colors/styles defined in `values/themes.xml` - extend rather than override
6. **API Compat:** Target SDK 35; avoid APIs below minSdk 24 without `@RequiresApi` annotation
7. **Drawable Resources:** Use WebP format for new images (already used for launcher icons)
8. **Firebase Usage:** Initialize services lazily in Activities/ViewModels; `PriceCartApplication` handles global setup

## Common Tasks

**Add a new dependency:**
1. Update `gradle/libs.versions.toml` (add version, then library alias)
2. Reference in `app/build.gradle.kts` via `implementation(libs.*)`

**Add a new Activity:**
1. Create class in `app/src/main/java/com/example/pricecart/`
2. Register in `AndroidManifest.xml` with appropriate intent-filters
3. Add theme via `android:theme="@style/Theme.PriceCart"`

**Build Flavors/Variants:** Not yet configured - single defaultConfig variant only

