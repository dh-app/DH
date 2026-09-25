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
