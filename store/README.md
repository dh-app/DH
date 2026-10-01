# Publishing to Google Play

Everything the Play Console asks for is in this folder:

| File | Use it for |
|---|---|
| [`listing.md`](listing.md) | App name, short and full description, category, contact details |
| [`icon-512.png`](icon-512.png) | App icon (512 × 512) |
| [`feature-graphic.png`](feature-graphic.png) | Feature graphic (1024 × 500) |
| [`privacy-policy.md`](privacy-policy.md) | The privacy policy (its link goes in the console) |
| [`play-console-answers.md`](play-console-answers.md) | Ads, content rating, target audience, data safety |
| [`release-notes.txt`](release-notes.txt) | "What's new" for this release |

Phone screenshots and the files to upload come from the **Release** workflow (below).

## 1. The signing key (once)

Google Play only accepts an app signed with your **upload key**. Google then re-signs it with the app's own key ("Play App Signing").

The app is new to the Play Store (`org.darulhuda.nabiurrahmah`), so it uses a newly created upload key. Keep the key file and its passwords safe and private. If they are ever lost, Play Console → **Test and release → App integrity → Upload key → Request upload key reset** replaces the key, usually within 2 days.

Then add it to GitHub once, under **Settings → Secrets and variables → Actions → New repository secret**:

| Secret | Value |
|---|---|
| `UPLOAD_KEYSTORE_BASE64` | the key file, base64-encoded (`base64 -w0 upload.jks` on Linux/macOS, `certutil -encode upload.jks out.txt` on Windows, keep only the lines between the BEGIN/END markers) |
| `UPLOAD_KEYSTORE_PASSWORD` | the keystore password |
| `UPLOAD_KEY_ALIAS` | the key alias |
| `UPLOAD_KEY_PASSWORD` | the key password |

Keep a copy of the key file and passwords somewhere safe outside GitHub.

## 2. Build the release

**Actions → Release → Run workflow.** In about 20 minutes it:

1. builds the app exactly as Play receives it (shrunk, optimised and signed);
2. installs it on an Android phone emulator, opens every section, and fails if anything crashes;
3. takes the store screenshots.

Download from the run's **Artifacts**:
- `play-store-bundle`: the `.aab` to upload (only when the signing secrets are set), plus `mapping.txt`;
- `release-apk-…`: the same build as an APK, to try on your own phone first;
- `store-screenshots`: phone screenshots at 1440 × 2880.

## 3. Upload in Play Console

1. **Test and release → Production** (or **Internal testing** for a first trial) → **Create new release**.
2. Upload the `.aab`. Under **App bundle explorer → Downloads → Deobfuscation file**, upload `mapping.txt` so crash reports are readable.
3. Paste `release-notes.txt` into **Release notes**.
4. Fill in the store listing from `listing.md` and the graphics.
5. Complete **Policy → App content** using `play-console-answers.md`.
6. **Send for review.** First reviews usually take 1–7 days.

## Later releases

Raise `versionCode` (and `versionName`) in `mobile_app/NabiUrRahmahApp/app/build.gradle.kts`, run the Release workflow, and upload the new `.aab`. New flyers, books and videos never need a release: the app picks them up by itself.
