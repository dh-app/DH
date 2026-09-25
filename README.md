# Nabi ur Rahmah ﷺ

Android app by **Dar-ul-Huda Charitable Trust, Udupi**. It introduces the life, character and
teachings of Prophet Muhammad ﷺ through flyers, videos and books in many languages, all free to
read, watch, save and share.

## Repository layout

```
DH/
├── mobile_app/NabiUrRahmahApp/   the live Android app (open this folder in Android Studio)
├── library/                      PDF books shown in the app; upload here to publish
│   ├── prophetic-biography/
│   ├── what-they-say/
│   └── books-on-prophet/
├── archive/                      earlier uploads, kept for reference; not used by the app
└── .github/workflows/            CI: tests, lint and a debug APK on every push
```

## The app

Opening the app shows the Durood with the salawat recording, which fades into six tiles:

| Tile | What it opens |
|---|---|
| **Teachings of Prophet Muhammad ﷺ**: flyers in multiple languages | Every language, then its flyers: full screen, zoom, save, share to WhatsApp |
| **Teachings of Prophet Muhammad ﷺ**: videos in multiple languages | The YouTube playlists, played in the app, with fullscreen and a floating window when you leave |
| **Biography of Prophet Muhammad ﷺ** | Seerah books from `library/prophetic-biography/` and IslamHouse, in every language |
| **What they say about Prophet Muhammad ﷺ** | Books from `library/what-they-say/` |
| **Books on Prophet Muhammad ﷺ** | Books from `library/books-on-prophet/` |
| **Contact us · About us** | Dar-ul-Huda Udupi: phone, WhatsApp, email, website, map, social links and settings |

Books open in the built-in reader. It resumes at the last page, has night mode and
go-to-page, and zooms when you tap a page. Books can be downloaded for offline reading,
shared to WhatsApp or anywhere else, and saved to Downloads.

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
│   ├── youtube/   playlist reader (page + RSS fallback)
│   └── library/   book shelves: repository folders, releases, IslamHouse
├── platform/      downloads, PDF rendering, salawat, picture-in-picture, share / call / email
└── ui/
    ├── theme/     colours from the emblem, Inter + Noto Naskh Arabic
    ├── navigation/
    ├── common/    shared components
    ├── landing/ home/ flyers/ viewer/ videos/ library/ about/
```

### Build and run

Open `mobile_app/NabiUrRahmahApp` in Android Studio (latest stable) and press Run. From a terminal:

```bash
cd mobile_app/NabiUrRahmahApp
./gradlew testDebugUnitTest   # unit tests: website, YouTube and book parsing, repositories
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

Upload PDFs on GitHub with **Add file → Upload files** into the tile's folder on `main`
(see [`library/README.md`](library/README.md)):

| Tile | Folder on `main` | Release tag (for files over 25 MB) |
|---|---|---|
| Biography of Prophet Muhammad ﷺ | `library/prophetic-biography/` | `prophetic-biography` |
| What they say about Prophet Muhammad ﷺ | `library/what-they-say/` | `what-they-say` |
| Books on Prophet Muhammad ﷺ | `library/books-on-prophet/` | `books-on-prophet` |

The Biography tile also lists IslamHouse's Seerah category in every language.

List each book's proper title and author in the folder's `books.txt`:

```
Book Name: Short Seerah of Prophet ﷺ
Author: Shaikh Dr. Abu Omar Parvez Nakwa Madani
```

The **Book Name** must match the PDF's file name. The language is worked out from the title's
script and words, so an Urdu title is shown as Urdu. GitHub's upload page accepts files up
to 25 MB; for bigger books, attach them to the release instead. The app picks up changes
within 12 hours, or right away when someone pulls down to refresh.

### Configuration

- The website address is `nur.siteUrl` in `mobile_app/NabiUrRahmahApp/gradle.properties`.
- Playlists that are always shown are listed in `nur.youtubePlaylists` in the same file.
- Book shelf sources are `nur.shelf.*` in the same file (repository folders, GitHub releases, IslamHouse categories, web pages).
- The salawat played on opening is `app/src/main/assets/salawat.mp3`. Without it, the app opens silently.
- Contact details on the About screen are in `app/src/main/assets/about.json`.
