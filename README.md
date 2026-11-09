# Cassette

**An offline music player for Android.**

Cassette is a local music player built with Kotlin and Jetpack Compose. It is designed around a simple idea: your music library should stay usable without a streaming subscription or an internet connection.

The current app version in the project is **1.0.1** (version code **3**).

## Features

- **Local music library:** choose a music folder on your device and browse its tracks by folder.
- **Background playback:** playback is handled by Media3, with media-session controls.
- **Cassette deck:** a dedicated landscape playback screen with transport controls and seeking.
- **Live audio visualizer:** a 48-band spectrum calculated from decoded PCM audio.
- **Visualizer styles:** Thick Bars, Fine Bars, Squiggle, Mirror, and Dots.
- **Themes:** Carbon, Neon, Jade, Violet, and Gold, each with light and dark appearance modes.
- **Saved preferences:** theme, appearance, visualizer style, repeat, shuffle, and selected music folder settings.

Audio format support depends on the Android device and the codec used by the file. Cassette filters common formats when scanning, but that does not guarantee every file will decode on every phone.

## Screenshots

Add screenshots of the app to the `screenshots/` folder. These filenames are a useful starting point:

- `screenshots/library.png`
- `screenshots/cassette-deck.png`
- `screenshots/visualizer.png`
- `screenshots/themes.png`

## Tech stack

- Kotlin
- Jetpack Compose and Material 3
- Media3 / ExoPlayer and MediaSession
- AndroidX DocumentFile for folder access
- Jetpack DataStore for saved preferences
- Gson for playlist/cache serialization

## Requirements

- Android Studio with Android SDK Platform 36 installed
- JDK 21 (the Gradle daemon toolchain is set to Java 21)
- Android device or emulator running **Android 7.0 (API 24) or newer**

The project uses the Gradle wrapper, so you do not need to install Gradle separately. The wrapper downloads the configured Gradle version on the first build.

## Build

Clone the repository and open the project folder in Android Studio, or build it from a terminal.

**Windows:**

```bat
gradlew.bat test
gradlew.bat assembleDebug
```

**macOS / Linux:**

```bash
./gradlew test
./gradlew assembleDebug
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

To install from Android Studio, select the `app` run configuration and run it on a connected device or emulator.

## Project structure

```text
Cassette/
├── app/
│   └── src/
│       ├── main/
│       │   ├── java/com/example/cassettev01/
│       │   │   ├── model/       # Song and playlist models
│       │   │   ├── player/      # Media3 playback and visualizer processing
│       │   │   ├── storage/     # Folder scanning, cache, and settings
│       │   │   ├── ui/          # Compose screens and themes
│       │   │   └── MainActivity.kt
│       │   └── res/             # App resources, fonts, and launcher icons
│       └── test/                # Unit tests
├── .github/                     # Issue templates
├── gradle/                      # Wrapper and version catalog
├── screenshots/
├── README.md
└── TESTING.md
```

## Manual testing

See [TESTING.md](TESTING.md) for the device checklist covering folder access, playback, seeking, background playback, themes, and visualizer behaviour.

## Releases

Source code belongs in the repository. When publishing an installable build, attach the APK to the repository's GitHub Releases page instead of committing generated APK files to the source tree.

## License

A license has not been added yet. Until one is chosen and included in this repository, please do not assume the source is available for reuse or redistribution.
