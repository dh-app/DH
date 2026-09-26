"""Prints what the app's sources really return, for debugging from CI."""
import json, urllib.request
from collections import Counter

UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0 Safari/537.36"
KEY = "paV29H2gm56kvLPy"

def get(url, timeout=60):
    try:
        with urllib.request.urlopen(urllib.request.Request(url, headers={"User-Agent": UA}), timeout=timeout) as r:
            return r.status, r.read()
    except urllib.error.HTTPError as e:
        return e.code, e.read()
    except Exception as e:  # noqa
        return None, repr(e).encode()

base = f"https://api3.islamhouse.com/v3/{KEY}/main/get-category-items/795"
for form in ["showall/showall/en", "showall/en/showall", "books/showall/en", "showall/showall/showall", "books/showall/showall", "showall/ar/en", "showall/ur/en"]:
    s, b = get(f"{base}/{form}/1/50/json")
    print(f"#### {form}: status {s}, {len(b)} bytes")
    try:
        d = json.loads(b)
    except Exception:
        print("   ", b[:300]); continue
    items = d.get("data") if isinstance(d, dict) else d
    if not isinstance(items, list):
        print("   ", str(d)[:300]); continue
    print("    links:", json.dumps(d.get("links"))[:400] if isinstance(d, dict) else None)
    langs = Counter((i.get("translated_language"), i.get("source_language")) for i in items)
    print("    items", len(items), "langs", langs.most_common(20))
    types = Counter(i.get("type") for i in items); print("    types", types)
    if items:
        i = items[0]
        print("    keys", sorted(i.keys()))
        print("    sample", json.dumps({k: i.get(k) for k in ("id", "title", "type", "source_id", "translated_language", "source_language", "prepared_by", "api_url")}, ensure_ascii=False)[:900])
        print("    attachment", json.dumps((i.get("attachments") or [None])[0], ensure_ascii=False)[:400])

print("#### available languages for the category (site languages API)")
for u in [f"https://api3.islamhouse.com/v3/{KEY}/main/sitecontent/en/json",
          f"https://api3.islamhouse.com/v3/{KEY}/main/get-category-languages/795/en/json"]:
    s, b = get(u); print(s, u.split(KEY)[1], b[:600])
