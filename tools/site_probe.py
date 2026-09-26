"""Prints what the app's sources really return, for debugging from CI."""
import json, re, urllib.request
from collections import Counter, defaultdict

UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0 Safari/537.36"

def get(url, timeout=180):
    try:
        with urllib.request.urlopen(urllib.request.Request(url, headers={"User-Agent": UA}), timeout=timeout) as r:
            return r.status, r.read()
    except urllib.error.HTTPError as e:
        return e.code, e.read()
    except Exception as e:  # noqa
        return None, repr(e).encode()

def base(url):
    name = url.split("?")[0].rsplit("/", 1)[-1]
    stem = name.rsplit(".", 1)[0]
    stem = re.sub(r"-\d+x\d+$", "", stem)
    stem = re.sub(r"-scaled$", "", stem)
    return url.split("/wp-content/uploads/")[-1].rsplit("/", 1)[0] + "/" + stem.lower()

def width_of(url):
    m = re.search(r"-(\d+)x(\d+)\.\w+$", url.split("?")[0])
    return (int(m.group(1)), int(m.group(2))) if m else None

import time
rows = []
queries = [
    "url=darulhudaudupi.org/wp-content/uploads/*&limit=5000",
    "url=darulhudaudupi.org/wp-content/uploads/2024/&matchType=prefix&limit=5000",
    "url=darulhudaudupi.org/wp-content/uploads/2024/10/&matchType=prefix&limit=5000",
]
for q in queries:
    for attempt in range(4):
        s, b = get(f"https://web.archive.org/cdx/search/cdx?{q}&output=json&fl=original,timestamp,statuscode,mimetype,length&filter=statuscode:200&collapse=urlkey")
        if s == 200: break
        time.sleep(10 * (attempt + 1))
    got = json.loads(b)[1:] if s == 200 and b.strip().startswith(b"[") else []
    print(q, "status", s, "rows", len(got))
    rows += got
rows = list({r[0]: r for r in rows}.values())
print("distinct archived uploads:", len(rows))

variants = defaultdict(list)
for original, ts, status, mime, length in rows:
    if mime.startswith("image/") or mime == "application/pdf":
        variants[base(original)].append((width_of(original), int(length or 0), original, ts))

manifest = json.load(open("library/flyers/_from-website/manifest.json"))
flyers = [(l["code"], f) for l in manifest["languages"] for f in l.get("flyers", [])]
best = Counter(); per_lang = defaultdict(Counter); examples = []
for code, f in flyers:
    vs = variants.get(base(f["image"]), [])
    full = [v for v in vs if v[0] is None]
    sized = sorted((v for v in vs if v[0]), key=lambda v: -v[0][0] * v[0][1])
    if full: bucket = "original"
    elif sized and max(sized[0][0]) >= 1000: bucket = ">=1000px"
    elif sized and max(sized[0][0]) >= 700: bucket = "700-999px"
    elif sized: bucket = "<700px"
    else: bucket = "none"
    best[bucket] += 1; per_lang[code][bucket] += 1
    if bucket in (">=1000px", "700-999px") and len(examples) < 8: examples.append((code, sized[0]))
print("#### best archived copy per flyer:", dict(best))
for code, c in per_lang.items(): print("   ", code, dict(c))
for e in examples: print("   example", e)
sizes = Counter(v[0] for vs in variants.values() for v in vs if v[0])
print("#### most common archived sizes:", sizes.most_common(25))
