"""Prints what the app's sources really return, for debugging from CI."""
import json, re, sys, urllib.parse, urllib.request
from html.parser import HTMLParser

UA = "NabiUrRahmah/2.0 (Android)"

def get(url, ua=UA):
    req = urllib.request.Request(url, headers={"User-Agent": ua, "Accept": "text/html,application/json,*/*"})
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            body = r.read()
            return r.status, r.geturl(), dict(r.headers), body
    except urllib.error.HTTPError as e:
        return e.code, url, dict(e.headers), e.read()
    except Exception as e:  # noqa
        return None, url, {}, str(e).encode()

class Links(HTMLParser):
    def __init__(self):
        super().__init__(); self.items = []; self._a = None; self._text = []
    def handle_starttag(self, tag, attrs):
        a = dict(attrs)
        if tag == "a" and a.get("href"):
            self._a = a["href"]; self._text = []
        if tag == "img":
            self.items.append(("img", a.get("src") or "", {k: v for k, v in a.items() if k in ("data-src", "data-lazy-src", "srcset", "data-srcset", "alt", "width", "height", "class")}))
        if tag == "iframe":
            self.items.append(("iframe", a.get("src") or a.get("data-src") or "", {}))
    def handle_data(self, data):
        if self._a is not None: self._text.append(data.strip())
    def handle_endtag(self, tag):
        if tag == "a" and self._a is not None:
            self.items.append(("a", self._a, " ".join(t for t in self._text if t)[:80])); self._a = None

def probe(url, limit=150):
    print("=" * 100); print("GET", url)
    status, final, headers, body = get(url)
    print("status:", status, "| final:", final, "| type:", headers.get("Content-Type"), "| bytes:", len(body))
    for h in ("Server", "CF-RAY", "Cache-Control", "ETag", "Last-Modified"):
        if h in headers: print(f"  {h}: {headers[h]}")
    if status != 200 or b"<" not in body[:2000]:
        print(body[:500].decode("utf-8", "replace")); return None
    html = body.decode("utf-8", "replace")
    title = re.search(r"<title>(.*?)</title>", html, re.S)
    print("title:", title.group(1).strip() if title else None)
    p = Links(); p.feed(html)
    shown = 0
    for kind, ref, extra in p.items:
        if kind == "a" and not re.search(r"(wp-content|nabi|rahmah|\.pdf|\.jpe?g|\.png|drive\.google|youtu)", ref, re.I):
            continue
        print(f"  {kind:6} {ref[:160]}  {extra if extra else ''}")
        shown += 1
        if shown >= limit: print("  ..."); break
    return html

site = "https://darulhudaudupi.org/nabi-ur-rahmah%EF%B7%BA/"
probe("https://darulhudaudupi.org/nabi-ur-rahmah/", limit=10)
index = probe(site)
if index:
    langs = sorted(set(re.findall(r'href="([^"]*nabi-ur-rahmah[^"]*)"', index)))
    print("language-like links:", len(langs))
    for l in langs[:40]: print("   ", l)
probe("https://darulhudaudupi.org/nabi-ur-rahmah%ef%b7%ba/hindi-nabi-ur-rahmah/")

print("=" * 100)
for pl in ("PLcF_nL7kXdt2VWfAg3XkhXaOEjH5KYviq", "PLcF_nL7kXdt0udlvCNfprkl5r0upR9iir"):
    status, _, _, body = get(f"https://www.youtube.com/playlist?list={pl}")
    text = body.decode("utf-8", "replace")
    print("youtube page", pl, status, "bytes", len(body), "ytInitialData:", "ytInitialData" in text,
          "renderers:", text.count("playlistVideoRenderer"), "title:", (re.search(r'"playlistMetadataRenderer":\{"title":"([^"]+)"', text) or [None, None])[1])
    status, _, _, body = get(f"https://www.youtube.com/feeds/videos.xml?playlist_id={pl}")
    print("youtube feed", pl, status, "entries:", body.count(b"<entry>"))

print("=" * 100)
key = "paV29H2gm56kvLPy"
for path in ("showall/showall/en/1/5/json", "showall/en/showall/1/5/json", "books/showall/en/1/5/json", "books/en/en/1/5/json"):
    url = f"https://api3.islamhouse.com/v3/{key}/main/get-category-items/795/{path}"
    status, _, _, body = get(url)
    print("islamhouse", path, status, body[:300].decode("utf-8", "replace").replace("\n", " "))
status, _, _, body = get("https://islamhouse.com/en/category/795/showall/showall/1/")
print("islamhouse web", status, "bytes", len(body), "item links:", len(set(re.findall(rb'/en/books/\d+', body))))
