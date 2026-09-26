"""Prints what the app's sources really return, for debugging from CI."""
import json, re, urllib.request, urllib.parse
from collections import Counter

DESKTOP = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0 Safari/537.36"

def get(url, extra=None, timeout=60):
    headers = {"User-Agent": DESKTOP, "Accept": "*/*", "Accept-Language": "en"}
    headers.update(extra or {})
    try:
        with urllib.request.urlopen(urllib.request.Request(url, headers=headers), timeout=timeout) as r:
            return r.status, r.read()
    except urllib.error.HTTPError as e:
        return e.code, e.read()
    except Exception as e:  # noqa
        return None, repr(e).encode()

print("#### Wayback: archived pages")
q = "https://web.archive.org/cdx/search/cdx?url=darulhudaudupi.org/nabi*&output=json&fl=original,timestamp,statuscode,mimetype&collapse=urlkey&limit=500"
s, b = get(q); rows = json.loads(b) if s == 200 and b.strip().startswith(b"[") else []
print("status", s, "rows", len(rows))
for r in rows[1:80]: print("  ", r)

print("#### Wayback: archived uploads (images/pdf)")
q = "https://web.archive.org/cdx/search/cdx?url=darulhudaudupi.org/wp-content/uploads/*&output=json&fl=original,timestamp,statuscode,mimetype,length&filter=statuscode:200&collapse=urlkey&limit=5000"
s, b = get(q, timeout=120); rows = json.loads(b) if s == 200 and b.strip().startswith(b"[") else []
print("status", s, "rows", len(rows))
kinds = Counter(r[3] for r in rows[1:]); print("mimetypes:", kinds.most_common(10))
big = [r for r in rows[1:] if not re.search(r"-\d+x\d+\.", r[0])]
print("originals (no -WxH):", len(big))
for r in big[:120]: print("  ", r[1], r[3], r[4], r[0])

print("#### Wayback: newest snapshot of the index page")
s, b = get("https://archive.org/wayback/available?url=darulhudaudupi.org/nabi-ur-rahmah%EF%B7%BA/")
print(s, b[:400])
s, b = get("https://archive.org/wayback/available?url=darulhudaudupi.org/nabi-ur-rahmah/")
print(s, b[:400])

print("#### YouTube item structure")
s, b = get("https://www.youtube.com/playlist?list=PLcF_nL7kXdt2VWfAg3XkhXaOEjH5KYviq", {"Cookie": "SOCS=CAI"})
text = b.decode("utf-8", "replace")
m = re.search(r"ytInitialData\s*=\s*(\{.*?\});\s*</script>", text, re.S)
data = json.loads(m.group(1))
paths = Counter(); examples = {}
def walk(o, path):
    if isinstance(o, dict):
        if "videoId" in o and isinstance(o["videoId"], str):
            shape = "/".join(p for p in path[-6:] if not p.isdigit())
            paths[shape] += 1; examples.setdefault(shape, (path, o))
        for k, v in o.items(): walk(v, path + [k])
    elif isinstance(o, list):
        for i, v in enumerate(o): walk(v, path + [str(i)])
walk(data, [])
for shape, n in paths.most_common(8): print(f"  {n:4} {shape}")
top = paths.most_common(1)[0][0]
path, obj = examples[top]
print("  example path:", "/".join(path))
# climb to the item container (5 levels up) and print it
node = data
for p in path[:-3]:
    node = node[int(p)] if isinstance(node, list) else node[p]
print("  container:", json.dumps(node, ensure_ascii=False)[:2500])

print("#### YouTube feed")
s, b = get("https://www.youtube.com/feeds/videos.xml?playlist_id=PLcF_nL7kXdt2VWfAg3XkhXaOEjH5KYviq")
t = b.decode("utf-8", "replace")
print("  titles:", re.findall(r"<entry>.*?<title>(.*?)</title>", t, re.S)[:6])
print("  links:", re.findall(r'<link rel="alternate" href="([^"]+)"', t)[:4])
for pl in ("PLcF_nL7kXdt0udlvCNfprkl5r0upR9iir",):
    s, b = get(f"https://www.youtube.com/feeds/videos.xml?playlist_id={pl}")
    t = b.decode("utf-8", "replace")
    print("  2nd playlist title:", re.search(r"<title>(.*?)</title>", t).group(1), re.findall(r"<entry>.*?<title>(.*?)</title>", t, re.S)[:4])
