# Flyer catalogue

The app reads [`catalog.json`](catalog.json) from this folder on GitHub:

```
https://raw.githubusercontent.com/dh-app/DH/main/content/catalog.json
```

Anything merged into `main` reaches users the next time they open the app or pull down to
refresh, usually within about 5 minutes. No app update is needed. A copy is also bundled into
each APK as an offline fallback.

## Adding a flyer

1. Put the image in `flyers/<language code>/`, for example `flyers/ur/seerah-01.jpg`.
   - Use JPEG or WebP, about 1200–1600 px on the long side, ideally under 500 KB.
   - **Never replace a file in place.** The app caches images forever. To change a flyer,
     upload a new file name (for example `seerah-01-v2.jpg`) and update the entry.
2. Add an entry to that language's `flyers` list in `catalog.json`:

   ```json
   {
     "id": "ur-seerah-01",
     "title": "رحمت للعالمین",
     "image": "flyers/ur/seerah-01.jpg",
     "width": 1240,
     "height": 1754
   }
   ```

3. Change `updatedAt` to today's date, then commit to `main`.

CI checks every change. It fails if the JSON is invalid, an `id` is duplicated, or a referenced
file is missing.

## Fields

| Field | Required | Notes |
|---|---|---|
| `schemaVersion` | – | Currently `1` |
| `updatedAt` | yes | `YYYY-MM-DD`. Must increase with each change |
| `languages[].code` | yes | ISO code, for example `ur`, `hi`, `en`. Must be unique |
| `languages[].name` | yes | English name, used for search |
| `languages[].nativeName` | – | Name in its own script, shown large |
| `languages[].rtl` | – | `true` for Urdu, Arabic |
| `flyers[].id` | yes | Unique, never reused |
| `flyers[].image` | yes | Path relative to this folder, or a full `https://` URL |
| `flyers[].title` | – | Shown under the flyer and in the viewer |
| `flyers[].thumbnail` | – | Smaller image for the grid. Defaults to `image` |
| `flyers[].pdf` | – | Adds an **Open PDF** button |
| `flyers[].width`, `height` | – | Pixel size. Lets the grid lay out before images load |
| `about` | yes | Contact details on the About screen |

Languages appear in the order they are listed. Languages with no flyers show "Coming soon".
