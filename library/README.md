# Library

PDF books shown in the app. **Uploading a PDF here publishes it; no app update needed.**

| Folder | Home tile |
|---|---|
| [`prophetic-biography/`](prophetic-biography) | Biography of Prophet Muhammad ﷺ (IslamHouse Seerah books are added to it) |
| [`what-they-say/`](what-they-say) | What they say about Prophet Muhammad ﷺ |
| [`books-on-prophet/`](books-on-prophet) | Books on Prophet Muhammad ﷺ |

## Adding a book

1. Open the folder on GitHub, then choose **Add file → Upload files**.
2. Drop in the PDF. GitHub's upload page takes files up to 25 MB. For a bigger book, attach it
   to the release with the same name as the folder (Releases, then the release, then **Edit**).
3. Add its details to the folder's `books.txt`:

   ```
   Book Name: Short Seerah of Prophet ﷺ
   Author: Shaikh Dr. Abu Omar Parvez Nakwa Madani
   ```

   **Book Name** must match the PDF's file name, without `.pdf`. You can also add
   `Language: Urdu` and `Description: …` lines. Without a language line, the app works
   the language out from the title.

Phones pick up changes within 12 hours, or right away when someone pulls down to refresh.

## Flyers and videos

The **Teachings** tiles read one file, [`catalog.json`](catalog.json), which the
[Catalogue workflow](../.github/workflows/catalog.yml) rebuilds every night (and whenever
flyers are uploaded). Nobody edits it by hand. Each run:

- reads the Nabi ur Rahmah pages on darulhudaudupi.org, or the Wayback Machine's copy
  while the site is down, and copies every flyer here in full size (`flyers/_from-website/`);
- adds the flyers uploaded to `flyers/<Language>/`;
- reads both YouTube playlists in full.

A flyer only reaches phones once its full-size image is here, so nothing blurry is shown.
Ones the website can't serve yet are tried again on every run.

### Adding flyers

Create a folder named after the language, in English or its own script (`flyers/Hindi/`,
`flyers/اردو/`), and upload images (JPG, PNG) or a **PDF**. Each page of a PDF becomes its own
flyer, rendered at print quality, so one PDF per language is enough. Phones get them after the
next run, usually within minutes of the upload.
