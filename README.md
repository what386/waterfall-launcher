# Waterfall Launcher

Waterfall Launcher is a small Android home screen focused on fast app access, readable lists, and a wallpaper-first layout. It is built with Kotlin and Jetpack Compose.

## Features

- Favorites screen for the apps you launch most often.
- Search overlay for quickly filtering installed apps.
- A-Z rail for jumping through the app list by letter.
- Drag-and-drop favorite reordering.
- Hidden-app mode for keeping rarely used apps out of the normal list.
- Android app widgets, including widget stacks.
- Wallpaper background with a minimal foreground interface.
- Settings for status bar visibility, app icons, search button visibility, clean home screen mode, home-row navigation buttons, and font selection.
- App details and uninstall actions from app-row menus.
- Live app-list refresh when packages are installed, removed, changed, or replaced.

## Requirements

- Android 8.0 or newer.
- Android Studio, or a local Android SDK plus the Gradle wrapper.
- JDK 17.

## Build

```bash
./gradlew :app:assembleDebug
```

The debug APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

To build the release variant:

```bash
./gradlew :app:assembleRelease
```

## Install

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

After installation, open Android's default home app settings and select Waterfall Launcher.

## Permissions

Waterfall Launcher requests `android.permission.REQUEST_DELETE_PACKAGES` so it can start Android's standard uninstall flow from an app-row menu. Android still prompts before uninstalling apps.

The app queries launchable applications so it can display and launch the installed app list.

## F-Droid

This repository includes Fastlane-compatible metadata under:

```text
fastlane/metadata/android/en-US/
```

Before submitting to F-Droid, confirm that the package name, version name, version code, screenshots, icon, release tag, and license are final. The official F-Droid build recipe should be submitted separately to the `fdroiddata` repository as `metadata/<applicationId>.yml`.

## License

Apache-2.0. See [LICENSE](LICENSE).

