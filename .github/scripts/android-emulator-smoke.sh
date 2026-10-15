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

# Navigate all major screens and capture their real emulator rendering.
# UI coordinates use the original 941x1672 reference image's top-left origin.
tap_ui() {
  local x="$1" top="$2"
  local canvas_h=$(( w * 1672 / 941 ))
  if (( canvas_h > h )); then canvas_h="$h"; fi
  local canvas_w=$(( canvas_h * 941 / 1672 ))
  local offset_x=$(( (w - canvas_w) / 2 ))
  local offset_y=$(( (h - canvas_h) / 2 ))
  adb shell input tap "$(( offset_x + x*canvas_w/941 ))" "$(( offset_y + top*canvas_h/1672 ))"
}
capture() {
  sleep 2
  adb shell pidof com.brendigo.skypeep >/dev/null || {
    echo "FAIL: crashed while opening $1"
    exit 1
  }
  adb exec-out screencap -p > "smoke-results/$1.png"
}
tap_ui 270 1210
capture levels
tap_ui 95 1540
capture map-back
tap_ui 660 1230
capture skins
tap_ui 100 1560
capture skins-back
tap_ui 660 1400
capture settings
tap_ui 465 1380
capture settings-back
tap_ui 470 1000
capture gameplay
tap_ui 868 65
capture pause
tap_ui 460 1040
capture pause-home
echo "PASS: all major SkyPeep screens launched without process death."
echo "PASS: Android app process survives boot and splash navigation."
