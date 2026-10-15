#!/usr/bin/env bash
set -Eeuo pipefail

mkdir -p smoke-results

# Always collect the real logcat, including failures before the first frame.
collect_diagnostics() {
  adb logcat -d -v time > smoke-results/logcat.txt 2>/dev/null || true
  adb shell dumpsys activity activities > smoke-results/activities.txt 2>/dev/null || true
  adb exec-out screencap -p > smoke-results/last-screen.png 2>/dev/null || true
}
trap collect_diagnostics EXIT

apk="smoke-artifacts/apk/debug/app-debug.apk"
test -s "$apk" || { echo "Missing APK: $apk"; find smoke-artifacts -type f; exit 1; }

adb logcat -c
adb install -r "$apk"
adb shell am start -W -n com.brendigo.skypeep/.MainActivity
sleep 12

if ! adb shell pidof com.brendigo.skypeep >/dev/null; then
  echo "FAIL: SkyPeep process crashed during launch"
  adb logcat -d -v time | grep -E -A 70 -B 10 'FATAL EXCEPTION|AndroidRuntime|libgdx|com.brendigo.skypeep' | tail -220 || true
  exit 1
fi

adb exec-out screencap -p > smoke-results/splash.png

# Advance to the menu and check that UI navigation does not terminate the process.
size="$(adb shell wm size | grep -oE '[0-9]+x[0-9]+' | tail -n 1)"
w="${size%x*}"
h="${size#*x}"
adb shell input tap "$((w / 2))" "$((h * 84 / 100))"
sleep 5
if ! adb shell pidof com.brendigo.skypeep >/dev/null; then
  echo "FAIL: SkyPeep crashed after tapping the splash screen"
  exit 1
fi

adb exec-out screencap -p > smoke-results/menu.png
echo "PASS: Android app process survives boot and splash navigation."
