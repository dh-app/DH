#!/usr/bin/env bash
# Installs the release APK on the running emulator, walks through it and
# shakes it with random taps. Fails on any crash. Used by release.yml.
set -euo pipefail
APK="$1"; OUT="$2"; APP_ID="${APP_ID:-org.darulhuda.udupi}"

adb wait-for-device
# A clean status bar for the store screenshots.
adb shell settings put global sysui_demo_allowed 1
adb shell am broadcast -a com.android.systemui.demo -e command enter >/dev/null
adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 1000 >/dev/null
adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false >/dev/null
adb shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4 >/dev/null
adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false >/dev/null

adb install -r "$APK"
python3 tools/release_tour.py "$OUT"

echo "Random-tap stress test"
adb logcat -c
if ! adb shell monkey -p "$APP_ID" --pct-syskeys 0 --throttle 250 -s 42 -v 1500 > "$OUT/monkey.txt" 2>&1; then
  echo "monkey reported a problem"; tail -50 "$OUT/monkey.txt"
fi
if grep -q "// CRASH" "$OUT/monkey.txt" || adb logcat -d -b crash | grep -q "$APP_ID"; then
  echo "::error::The release build crashed under random taps"
  adb logcat -d -b crash | tail -80
  exit 1
fi
echo "No crashes."
