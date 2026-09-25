# Nabi ur Rahmah ﷺ

Android app by **Dar-ul-Huda Charitable Trust, Udupi**. It introduces the life, character and
teachings of Prophet Muhammad ﷺ through short flyers in many languages. People can read the
flyers, zoom into them, save them and share them.

## Repository layout

| Path | What it is |
|---|---|
| [`mobile_app/NabiUrRahmahApp`](mobile_app/NabiUrRahmahApp) | The Android app (Kotlin, Jetpack Compose) |
| [`.github/workflows`](.github/workflows) | CI: unit tests, lint and a debug APK on every push |

## The app

- **Home**: the emblem and Al-Anbiyāʾ 21:107, then every language with its flyer count, with search.
- **Flyers**: a grid of flyers for one language.
- **Viewer**: full-screen, swipe between flyers, pinch or double-tap to zoom. Share the image
  itself (not just a link), save it to the gallery, or open the PDF version.
- **Videos**: the Nabi ur Rahmah YouTube playlists, played inside the app with the official
  YouTube player. Fullscreen turns to landscape, the next video plays automatically, and a video
  resumes where you left it. There's also a "Watch on YouTube" button.
- **About us**: the project, Dar-ul-Huda Udupi, phone, WhatsApp, email, website, map and social links.

### Videos come straight from YouTube

The playlists listed in `nur.youtubePlaylists` (`gradle.properties`) are read from YouTube
without an API key. The app uses the playlist page (up to 100 videos, with durations) and
falls back to the RSS feed. **Any YouTube playlist linked or embedded on the Nabi ur Rahmah web
page is added automatically**, and new videos in a playlist appear by themselves.

### Flyers come straight from the website

The app reads [darulhudaudupi.org/nabi-ur-rahmah](https://darulhudaudupi.org/nabi-ur-rahmah/)
and the language pages it links to. **Publishing a flyer on the website is all it takes.**
There is nothing to upload to GitHub and no app update to release.

The reader (`data/site/NabiSiteParser.kt`) doesn't depend on one exact page layout. It understands:
- links to a page per language, labelled in English or the language's own script
  (`Urdu`, `اردو`, `ಕನ್ನಡ`), or with the language in the URL (`/nabi-ur-rahmah-tamil/`);
- flyers placed under a language heading, tab or accordion on the page itself;
- flyer images (WordPress galleries, lazy-loaded images), PDF links and Google Drive links.

It ignores the header, footer, menus, logos and icons, plus banners that repeat across pages.
For each flyer it picks a small rendition for the grid and the largest one for the viewer.
A PDF with no preview image gets its first page rendered on the phone.

### Performance

- **Instant start.** The last catalogue is saved on the phone and shown immediately.
- **Progressive loading.** The language list appears as soon as the main page is read. Each
  language's flyers fill in as its page arrives, and language pages load in parallel.
- **Minimal data.** Pages are fetched with conditional requests, so an unchanged page costs a
  tiny HTTP 304 and isn't parsed again. The website is read at most once every 15 minutes
  unless you pull down to refresh.
- **Right-sized images.** Grids load small renditions and the viewer loads large ones. Images
  are cached in memory and on disk (512 MB), so flyers already seen work offline.
- **Resilient.** If one language page fails, the other languages still update and that one
  keeps its saved flyers. If the website can't be read at all, the saved flyers stay.

It supports light and dark themes, right-to-left scripts, and screen readers.

### Tech

Kotlin 2.3 · Jetpack Compose (Material 3) · type-safe Navigation · Coroutines ·
OkHttp · jsoup · kotlinx.serialization · Coil · Telephoto (zoom) · android-youtube-player. minSdk 24, targetSdk 36.

```
app/src/main/kotlin/org/darulhuda/nabiurrahmah/
├── data/          model, repository (offline-first), website source
│   ├── site/      website parser, language recognition
│   └── youtube/   playlist reader (page + RSS fallback)
├── platform/      downloading, saving to the gallery, share / call / email intents
└── ui/
    ├── theme/     colours from the emblem, Inter + Noto Naskh Arabic
    ├── navigation/
    ├── common/    shared components
    ├── home/ flyers/ viewer/ videos/ about/
```

### Build and run

Open `mobile_app/NabiUrRahmahApp` in Android Studio (latest stable) and press Run. From a terminal:

```bash
cd mobile_app/NabiUrRahmahApp
./gradlew testDebugUnitTest   # unit tests: parser, language matching, repository
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

### Books: adding PDFs without touching code

Each book tile reads the PDFs attached to a GitHub **release** of this repository:

| Tile | Release tag |
|---|---|
| Biography of Prophet Muhammad ﷺ | `prophetic-biography` (added to the IslamHouse books) |
| What they say about Prophet Muhammad ﷺ | `what-they-say` |
| Books on Prophet Muhammad ﷺ | `books-on-prophet` |

To publish a book, open the release on GitHub (**Releases**, then the release, then **Edit**), drag the
PDF in, and save. Name files `Title - Language.pdf`, for example `The Sealed Nectar - Urdu.pdf`.
Files with the same title become one book with several languages. The app picks up
changes within 12 hours, or right away when someone pulls down to refresh.

### Configuration

- The website address is `nur.siteUrl` in `mobile_app/NabiUrRahmahApp/gradle.properties`.
- Playlists that are always shown are listed in `nur.youtubePlaylists` in the same file.
- Book shelf sources are `nur.shelf.*` in the same file (IslamHouse categories, web pages, GitHub releases).
- The salawat played on opening is `app/src/main/assets/salawat.mp3`. Without it, the app opens silently.
- Contact details on the About screen are in `app/src/main/assets/about.json`.
