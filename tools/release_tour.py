"""Walks through the installed release build on an emulator, like a first-time user.

Opens every home tile, a flyer, a video, a book and the reader, saving a
screenshot of each screen (for the Play Store listing) and failing if the app
crashes or a screen never appears. Run by .github/workflows/release.yml.
"""
import os, re, subprocess, sys, time
import xml.etree.ElementTree as ET

PACKAGE = os.environ.get("APP_ID", "org.darulhuda.udupi")
OUT = sys.argv[1] if len(sys.argv) > 1 else "screenshots"
os.makedirs(OUT, exist_ok=True)
problems = []


def adb(*args, check=False):
    return subprocess.run(["adb", *args], capture_output=True, text=True, check=check).stdout


def screen():
    """The visible UI as (text, content-desc, bounds) nodes; retried while animations settle."""
    for _ in range(6):
        adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
        xml = adb("shell", "cat", "/sdcard/ui.xml")
        if xml.strip().startswith("<?xml"):
            try:
                root = ET.fromstring(xml[xml.index("<hierarchy"):])
            except (ET.ParseError, ValueError):
                root = None
            if root is not None:
                return [(n.get("text", ""), n.get("content-desc", ""), n.get("bounds", ""), n.get("clickable") == "true")
                        for n in root.iter("node")]
        time.sleep(1.5)
    return []


def centre(bounds):
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", bounds))
    return (x1 + x2) // 2, (y1 + y2) // 2


def tap_text(pattern, wait=4.0, timeout=20.0, index=0):
    """Taps the [index]th element whose text or description matches [pattern]."""
    regex = re.compile(pattern, re.I)
    deadline = time.time() + timeout
    scrolls = 0
    while time.time() < deadline:
        matches = [n for n in screen() if regex.search(n[0]) or regex.search(n[1])]
        if len(matches) > index:
            adb("shell", "input", "tap", *map(str, centre(matches[index][2])))
            time.sleep(wait)
            return True
        if scrolls < 3:  # it may be further down the screen
            scroll_down()
            scrolls += 1
        else:
            time.sleep(2)
    problems.append(f"never found on screen: {pattern!r}")
    print(f"  ! never found: {pattern!r}")
    return False


def tap_fraction(fx, fy, wait=4.0):
    size = re.search(r"(\d+)x(\d+)", adb("shell", "wm", "size"))
    w, h = (int(size.group(1)), int(size.group(2))) if size else (1440, 2880)
    adb("shell", "input", "tap", str(int(w * fx)), str(int(h * fy)))
    time.sleep(wait)


def scroll_down():
    size = re.search(r"(\d+)x(\d+)", adb("shell", "wm", "size"))
    w, h = (int(size.group(1)), int(size.group(2))) if size else (1440, 2880)
    adb("shell", "input", "swipe", str(w // 2), str(int(h * 0.75)), str(w // 2), str(int(h * 0.35)), "400")
    time.sleep(1.5)


def shot(name):
    path = os.path.join(OUT, f"{name}.png")
    with open(path, "wb") as f:
        f.write(subprocess.run(["adb", "exec-out", "screencap", "-p"], capture_output=True).stdout)
    print("  saved", path)
    check_alive(name)


def back(times=1, wait=2.0):
    for _ in range(times):
        adb("shell", "input", "keyevent", "KEYCODE_BACK")
        time.sleep(wait)


def check_alive(step):
    if not adb("shell", "pidof", PACKAGE).strip():
        problems.append(f"app not running after: {step}")
        print(f"  ! app not running after {step}")


def go_home():
    adb("shell", "am", "start", "-W", "-n", f"{PACKAGE}/org.darulhuda.nabiurrahmah.MainActivity",
        "-a", "android.intent.action.MAIN", "-c", "android.intent.category.LAUNCHER", "--activity-reorder-to-front")
    time.sleep(2)


print("Launching", PACKAGE)
adb("logcat", "-c")
adb("shell", "monkey", "-p", PACKAGE, "-c", "android.intent.category.LAUNCHER", "1")
# The Durood shows for a few seconds after the system splash: catch it as soon as its Arabic text is up.
arabic = re.compile("[\u0600-\u06FF]{3,}")
deadline = time.time() + 10
while time.time() < deadline and not any(arabic.search(n[0]) for n in screen()):
    time.sleep(0.3)
time.sleep(0.8)  # let the meaning fade in beneath it
shot("01-opening-durood")
time.sleep(6)
shot("02-home")

print("Flyers")
if tap_text(r"Flyers in multiple languages", wait=6):
    shot("03-flyer-languages")
    if tap_text(r"^(English|Urdu|اردو)$", wait=8):
        shot("04-flyers")
        tap_fraction(0.27, 0.42, wait=6)
        shot("05-flyer")
        back(3)

print("Videos")
go_home()
if tap_text(r"Videos in multiple languages", wait=6):
    shot("06-videos")
    if tap_text(r"Hadith Series - ", wait=12):
        shot("07-video")
        back(2)

print("Biography")
go_home()
if tap_text(r"^Biography of Prophet", wait=8):
    shot("08-biography")
    tap_fraction(0.27, 0.45, wait=5)
    shot("09-book")
    if tap_text(r"^Read$", wait=10):
        shot("10-reader")
        back()
    back(2)

print("About")
go_home()
if tap_text(r"Contact us", wait=4):
    shot("11-about")
    back()

crashes = [l for l in adb("logcat", "-d", "-b", "crash").splitlines() if PACKAGE in l or "FATAL EXCEPTION" in l]
if crashes:
    problems.append("crash in logcat:\n" + "\n".join(crashes[:40]))
    print("\n".join(crashes[:80]))

summary = os.environ.get("GITHUB_STEP_SUMMARY")
report = "## Release walkthrough\n\n" + ("All screens opened, no crashes.\n" if not problems else "\n".join(f"- {p}" for p in problems) + "\n")
print(report)
if summary:
    with open(summary, "a") as f:
        f.write(report)
# A missing button is reported; only a crash fails the build.
sys.exit(1 if any(p.startswith(("crash", "app not running")) for p in problems) else 0)
