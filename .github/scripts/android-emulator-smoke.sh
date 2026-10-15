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
adb shell settings put secure immersive_mode_confirmations confirmed || true
adb shell am start -W -n com.brendigo.skypeep/.MainActivity
# Dismiss the emulator's one-time Android immersive-mode tutorial.
screen_size="$(adb shell wm size | grep -oE '[0-9]+x[0-9]+' | tail -n 1)"
screen_w="${screen_size%x*}"
screen_h="${screen_size#*x}"
# Android may present the immersive-mode tutorial several seconds AFTER launch.
sleep 7
adb shell input tap "$((screen_w * 85 / 100))" "$((screen_h * 26 / 100))" || true
sleep 3

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
# Retry opening the main menu if the emulator was still showing the
# system immersive help overlay rather than receiving the first touch.
menu_ready=false
for attempt in 1 2 3 4 5; do
  adb shell input tap "$((w / 2))" "$((h * 82 / 100))"
  sleep 2
  last="$(adb logcat -d -s SkyPeepNav:I '*:S' | grep 'screen=' | tail -n 1 || true)"
  echo "SkyPeep entry attempt $attempt => $last"
  if [[ "$last" == *"screen=MENU"* ]]; then menu_ready=true; break; fi
  adb shell input tap "$((w * 85 / 100))" "$((h * 26 / 100))" || true
  sleep 1
done
if [[ "$menu_ready" != true ]]; then
  echo "FAIL: SkyPeep did not enter the main menu"
  exit 1
fi
adb shell pidof com.brendigo.skypeep >/dev/null || { echo "SkyPeep process terminated"; exit 1; }

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
  local last
  last="$(adb logcat -d -s SkyPeepNav:I '*:S' | grep 'screen=' | tail -n 1 || true)"
  echo "SkyPeep navigation checkpoint: $1 => $last"
  if [[ -n "${2:-}" && "$last" != *"screen=$2"* ]]; then
    echo "FAIL: expected $2 after navigating $1"
    exit 1
  fi
  adb exec-out screencap -p > "smoke-results/$1.png"
}
tap_ui 270 1210
capture levels MAP
tap_ui 95 1540
capture map-back MENU
tap_ui 660 1230
capture skins SKINS
tap_ui 100 1560
capture skins-back MENU
tap_ui 660 1400
capture settings SETTINGS
tap_ui 465 1380
capture settings-back MENU
tap_ui 470 1000
capture gameplay PLAY
tap_ui 868 65
capture pause PAUSE
tap_ui 460 1040
capture pause-home MENU
echo "PASS: all major SkyPeep screens launched without process death."

# Diagnostic-only level override exists exclusively in debuggable APKs.
# Capture every restored world on a real Android emulator before publication.
for world in 1 2 3 4 5 6; do
  adb logcat -c
  adb shell am force-stop com.brendigo.skypeep
  adb shell am start -W --ei sky_peep_qa_world "$world" -n com.brendigo.skypeep/.MainActivity
  sleep 5
  adb shell pidof com.brendigo.skypeep >/dev/null || {
    echo "FAIL: World $world crashed on launch"
    exit 1
  }
  last="$(adb logcat -d -s SkyPeepNav:I '*:S' | grep 'screen=' | tail -n 1 || true)"
  echo "SkyPeep world $world => $last"
  if [[ "$last" != *"screen=PLAY"* ]]; then
    echo "FAIL: World $world did not open gameplay"
    exit 1
  fi
  adb exec-out screencap -p > "smoke-results/world-$world.png"
done
echo "PASS: all six reconstructed worlds opened and rendered."
echo "PASS: Android app process survives boot and splash navigation."
