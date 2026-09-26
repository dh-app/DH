"""Prints what the app's sources really return, for debugging from CI."""
import json, re, socket, ssl, subprocess, urllib.request

UA = "Mozilla/5.0 (Linux; Android 14) NabiUrRahmah/2.0"

def get(url):
    req = urllib.request.Request(url, headers={"User-Agent": UA, "Accept": "*/*"})
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            return r.status, r.geturl(), dict(r.headers), r.read()
    except urllib.error.HTTPError as e:
        return e.code, url, dict(e.headers), e.read()
    except Exception as e:  # noqa
        return None, url, {}, repr(e).encode()

def sh(cmd):
    print("$", cmd)
    out = subprocess.run(cmd, shell=True, capture_output=True, text=True, timeout=60)
    print((out.stdout + out.stderr)[-3000:])

print("#### DNS / TLS")
sh("getent hosts darulhudaudupi.org www.darulhudaudupi.org")
for host in ("darulhudaudupi.org", "www.darulhudaudupi.org"):
    sh(f"echo | timeout 20 openssl s_client -connect {host}:443 -servername {host} 2>&1 | grep -E 'subject=|issuer=|Verify return|alert|DNS:|error' | head")
    sh(f"echo | timeout 20 openssl s_client -connect {host}:443 -servername {host} 2>/dev/null | openssl x509 -noout -ext subjectAltName -dates 2>&1 | head")
    sh(f"echo | timeout 20 openssl s_client -connect {host}:443 2>/dev/null | openssl x509 -noout -subject -ext subjectAltName 2>&1 | head")
sh("curl -sSI --max-time 20 http://darulhudaudupi.org/ | head -12")
sh("curl -sSI --max-time 20 https://www.darulhudaudupi.org/ | head -12")
sh("curl -sSIk --max-time 20 https://darulhudaudupi.org/ | head -12")

print("#### PAGES (whichever address works)")
for base in ("https://www.darulhudaudupi.org", "http://darulhudaudupi.org", "http://www.darulhudaudupi.org"):
    for path in ("/nabi-ur-rahmah%EF%B7%BA/", "/nabi-ur-rahmah%ef%b7%ba/hindi-nabi-ur-rahmah/"):
        status, final, headers, body = get(base + path)
        print(base + path, "->", status, final, headers.get("Content-Type"), len(body))
        if status == 200 and b"<html" in body[:5000].lower():
            html = body.decode("utf-8", "replace")
            main = re.search(r"<main.*?</main>|<article.*?</article>|class=\"entry-content.*", html, re.S)
            chunk = (main.group(0) if main else html)
            links = re.findall(r'<a[^>]+href="([^"]+)"[^>]*>(.*?)</a>', chunk, re.S)
            imgs = re.findall(r"<img[^>]+>", chunk)
            print("   links:", len(links), "imgs:", len(imgs))
            for href, text in links[:60]:
                t = re.sub(r"<[^>]+>", " ", text); t = re.sub(r"\s+", " ", t).strip()
                print("   A", href[:150], "|", t[:60])
            for tag in imgs[:25]:
                print("   IMG", re.sub(r"\s+", " ", tag)[:300])
            with open("/tmp/page.html", "w") as f: f.write(html)
            break

print("#### YOUTUBE structure")
status, _, _, body = get("https://www.youtube.com/playlist?list=PLcF_nL7kXdt2VWfAg3XkhXaOEjH5KYviq")
text = body.decode("utf-8", "replace")
for key in ("playlistVideoRenderer", "lockupViewModel", "playlistVideoListRenderer", "richItemRenderer", "videoRenderer", "contentId", "\"videoId\"", "playlistHeaderRenderer", "pageHeaderViewModel", "playlistMetadataRenderer", "lengthSeconds", "thumbnailOverlayTimeStatusRenderer"):
    print(f"   {key}: {text.count(key)}")
i = text.find('"videoId"')
print("   around first videoId:", text[max(0, i - 800):i + 1500].replace("\\u0026", "&"))
m = re.search(r'"title":\{"simpleText":"([^"]+)"', text); print("   a simpleText title:", m and m.group(1))
m = re.search(r'<meta property="og:title" content="([^"]+)"', text); print("   og:title:", m and m.group(1))

print("#### ISLAMHOUSE item")
status, _, _, body = get("https://api3.islamhouse.com/v3/paV29H2gm56kvLPy/main/get-category-items/795/showall/en/showall/1/2/json")
data = json.loads(body)
print(json.dumps(data.get("links"), ensure_ascii=False))
print(json.dumps(data.get("data", [None])[0], ensure_ascii=False, indent=1)[:3500])
