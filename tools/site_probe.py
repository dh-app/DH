"""Prints what the app's sources really return, for debugging from CI."""
import json, re, subprocess, urllib.request

DESKTOP = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0 Safari/537.36"

def get(url, ua=DESKTOP, extra=None):
    headers = {"User-Agent": ua, "Accept": "*/*", "Accept-Language": "en"}
    headers.update(extra or {})
    try:
        with urllib.request.urlopen(urllib.request.Request(url, headers=headers), timeout=30) as r:
            return r.status, r.geturl(), dict(r.headers), r.read()
    except urllib.error.HTTPError as e:
        return e.code, url, dict(e.headers), e.read()
    except Exception as e:  # noqa
        return None, url, {}, repr(e).encode()

def sh(cmd):
    print("$", cmd); out = subprocess.run(cmd, shell=True, capture_output=True, text=True, timeout=90)
    print((out.stdout + out.stderr)[-4000:])

print("#### Where does the domain forward?")
sh("curl -sS -i --max-time 20 'http://darulhudaudupi.org/nabi-ur-rahmah%EF%B7%BA/'")
sh("curl -sS -i --max-time 20 'http://darulhudaudupi.org/'")
sh("curl -sS -L -o /dev/null -w 'final=%{url_effective} code=%{http_code}\\n' --max-time 30 'http://darulhudaudupi.org/nabi-ur-rahmah%EF%B7%BA/'")
sh("dig +short darulhudaudupi.org NS; dig +short darulhudaudupi.org A; dig +short www.darulhudaudupi.org CNAME; dig +short darulhudaudupi.org TXT | head -5")

print("#### YouTube (desktop)")
for pl in ("PLcF_nL7kXdt2VWfAg3XkhXaOEjH5KYviq",):
    status, _, _, body = get(f"https://www.youtube.com/playlist?list={pl}", extra={"Cookie": "SOCS=CAI; CONSENT=YES+"})
    text = body.decode("utf-8", "replace")
    for key in ("playlistVideoRenderer", "lockupViewModel", "playlistVideoListRenderer", "\"videoId\"", "lengthSeconds", "playlistMetadataRenderer", "pageHeaderViewModel", "\\x22videoId\\x22"):
        print(f"   {key}: {text.count(key)}")
    i = text.find("playlistVideoRenderer")
    if i < 0: i = text.find('"videoId"')
    print("   sample:", text[i:i + 1800])
    m = re.search(r'"playlistMetadataRenderer":\{"title":"([^"]+)"', text); print("   metadata title:", m and m.group(1))
# phone user agent variant, to see the escaped form
status, _, _, body = get("https://www.youtube.com/playlist?list=PLcF_nL7kXdt2VWfAg3XkhXaOEjH5KYviq", ua="NabiUrRahmah/2.0 (Android)")
text = body.decode("utf-8", "replace")
i = text.find("ytInitialData")
print("   android UA ytInitialData context:", text[i:i + 400])

print("#### IslamHouse authors")
status, _, _, body = get("https://api3.islamhouse.com/v3/paV29H2gm56kvLPy/main/get-item/2844636/en/json")
print(json.dumps(json.loads(body).get("prepared_by"), ensure_ascii=False)[:800])
status, _, _, body = get("https://api3.islamhouse.com/v3/paV29H2gm56kvLPy/main/get-category-items/795/showall/ur/showall/1/2/json")
d = json.loads(body); print("ur listing:", json.dumps([(x.get("title"), x.get("translated_language"), [p.get("title") for p in x.get("prepared_by", [])]) for x in d.get("data", [])], ensure_ascii=False))
print("languages in first 50:")
status, _, _, body = get("https://api3.islamhouse.com/v3/paV29H2gm56kvLPy/main/get-category-items/795/showall/en/showall/1/50/json")
d = json.loads(body); from collections import Counter
print(Counter(x.get("translated_language") for x in d.get("data", [])), "authors:", [p.get("title") for x in d["data"][:8] for p in x.get("prepared_by", [])])
