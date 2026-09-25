# Nabi ur Rahmah ﷺ

Android app by **Dar-ul-Huda Charitable Trust, Udupi**. It introduces the life, character and
teachings of Prophet Muhammad ﷺ through short flyers in many languages. People can read the
flyers, zoom into them, save them and share them.

## Repository layout

| Path | What it is |
|---|---|
| [`mobile_app/NabiUrRahmahApp`](mobile_app/NabiUrRahmahApp) | The Android app (Kotlin, Jetpack Compose) |
| [`content/`](content) | The flyer catalogue the app downloads. Edit this to publish flyers, no app update needed |
| [`.github/workflows`](.github/workflows) | CI: unit tests, lint and a debug APK on every push |

## The app

- **Home**: the emblem and Al-Anbiyāʾ 21:107, then every language with its flyer count, with search.
- **Flyers**: a grid of flyers for one language.
- **Viewer**: full-screen, swipe between flyers, pinch or double-tap to zoom. Share the image
  itself (not just a link), save it to the gallery, or open the PDF version.
- **About us**: the project, Dar-ul-Huda Udupi, phone, WhatsApp, email, website, map and social links.

It works offline. The last downloaded catalogue and every image already seen stay available,
and a copy of the catalogue ships inside the APK, so the app is never empty on first launch.
It supports light and dark themes, right-to-left scripts, and screen readers.

### Tech

Kotlin 2.3 · Jetpack Compose (Material 3) · type-safe Navigation · kotlinx.serialization ·
OkHttp · Coil · Telephoto (zoom). minSdk 24, targetSdk 36.

```
app/src/main/kotlin/org/darulhuda/nabiurrahmah/
├── data/          catalogue model, parser, repository (offline-first), sources
├── platform/      downloading, saving to the gallery, share / call / email intents
└── ui/
    ├── theme/     colours from the emblem, Inter + Noto Naskh Arabic
    ├── navigation/
    ├── common/    shared components
    ├── home/ flyers/ viewer/ about/
```

### Build and run

Open `mobile_app/NabiUrRahmahApp` in Android Studio (latest stable) and press Run. From a terminal:

```bash
cd mobile_app/NabiUrRahmahApp
./gradlew testDebugUnitTest   # unit tests, including validation of content/catalog.json
./gradlew assembleDebug       # app/build/outputs/apk/debug/
```

CI builds a debug APK on every push. Download it from the run's **Artifacts** section in the
Actions tab.

### Release builds

Create `mobile_app/NabiUrRahmahApp/keystore.properties`. It is git-ignored, so never commit it.

```properties
storeFile=/absolute/path/to/upload-key.jks
storePassword=…
keyAlias=…
keyPassword=…
```

Then run `./gradlew bundleRelease`. The application id stays `org.darulhuda.udupi`, so the
Play Store listing keeps updating the existing app. Raise `versionCode` in
`app/build.gradle.kts` for every release.
