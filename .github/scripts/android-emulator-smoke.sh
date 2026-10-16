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
tap_ui 275 1400
capture shop SHOP
tap_ui 700 687
capture shop-purchase SHOP
adb logcat -d -s SkyPeepShop:I '*:S' | grep -q 'selected=Cyan Dash' || {
  echo "FAIL: SHOP did not purchase and equip cyan flight trail"
  exit 1
}
tap_ui 470 1360
capture shop-back MENU
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
  adb shell am start -W --ei sky_peep_qa_world "$world" --ez sky_peep_qa_pillars true -n com.brendigo.skypeep/.MainActivity
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
  if [[ "$world" == 1 ]]; then
    adb logcat -d -s SkyPeepShop:I '*:S' | grep -q 'loaded=Cyan Dash' || {
      echo "FAIL: purchased trail was not restored after Android process restart"
      exit 1
    }
  fi
  adb exec-out screencap -p > "smoke-results/world-$world.png"
done
echo "PASS: all six reconstructed worlds opened and rendered."
# Each purchased skin must render and keep the app alive on the Android emulator.
# The override is ignored by production (non-debuggable) builds.
for skin in 0 1 2 3 4; do
  adb logcat -c
  adb shell am force-stop com.brendigo.skypeep
  adb shell am start -W --ei sky_peep_qa_world 1 --ei sky_peep_qa_skin "$skin" \
    -n com.brendigo.skypeep/.MainActivity
  sleep 3
  adb shell pidof com.brendigo.skypeep >/dev/null || {
    echo "FAIL: Bird skin $skin crashes on launch"
    exit 1
  }
  current="$(adb logcat -d -s SkyPeepNav:I '*:S' | grep 'screen=' | tail -n 1 || true)"
  if [[ "$current" != *"screen=PLAY"* ]]; then
    echo "FAIL: Skin $skin did not enter gameplay: $current"
    exit 1
  fi
  adb exec-out screencap -p > "smoke-results/skin-$skin.png"
done
echo "PASS: five bird skins displayed during Android gameplay."
# Ensure the illustrated winged panel displays DYNAMIC points rather than
# the old hardcoded 27. Both values are tested on actual Android GL frames.
for points in 0 147; do
  adb logcat -c
  adb shell am force-stop com.brendigo.skypeep
  adb shell am start -W --ei sky_peep_qa_world 3 --ei sky_peep_qa_score "$points" \
    -n com.brendigo.skypeep/.MainActivity
  sleep 3
  adb shell pidof com.brendigo.skypeep >/dev/null || {
    echo "FAIL: score=$points crashed the app"; exit 1;
  }
  adb logcat -d -s SkyPeepHUD:I '*:S' | grep -q "debug_score=$points level=3" || {
    echo "FAIL: live dynamic score=$points did not load"; exit 1;
  }
  adb exec-out screencap -p > "smoke-results/dynamic-score-$points.png"
done
echo "PASS: both dynamic score values render on Android."
# Verify actual progression state on the illustrated level map. World 3 is
# OPEN but not falsely CLEAR, with earlier worlds already completed.
adb logcat -c
adb shell am force-stop com.brendigo.skypeep
adb shell am start -W --ei sky_peep_qa_map_unlocked 3 \
  -n com.brendigo.skypeep/.MainActivity
sleep 4
adb shell pidof com.brendigo.skypeep >/dev/null || {
  echo "FAIL: map progression fixture crashed"; exit 1;
}
adb logcat -d -s SkyPeepMap:I '*:S' | grep -q 'map_loaded unlocked=3 selected=3' || {
  echo "FAIL: debug map state not initialized"; exit 1;
}
capture map-progress MAP
tap_ui 660 710
sleep 1
adb logcat -d -s SkyPeepMap:I '*:S' | grep -q 'world_locked=4 unlocked=3' || {
  echo "FAIL: locked island accepted or event not logged"; exit 1;
}
tap_ui 730 1140
sleep 1
adb logcat -d -s SkyPeepMap:I '*:S' | grep -q 'selected_world=2' || {
  echo "FAIL: unlocked island cannot be selected"; exit 1;
}
tap_ui 760 1580
capture map-play PLAY
echo "PASS: map unlock, selection and Play navigation."


# The win state uses a DIFFERENT screen composition from the lose-only
# GAME OVER art. Verify it actually opens as RESULT and the NEXT button works.
adb logcat -c
adb shell am force-stop com.brendigo.skypeep
adb shell am start -W --ei sky_peep_qa_world 1 --ez sky_peep_qa_victory true \
  -n com.brendigo.skypeep/.MainActivity
sleep 4
adb shell pidof com.brendigo.skypeep >/dev/null || {
  echo "FAIL: victory screen crashed"; exit 1;
}
result="$(adb logcat -d -s SkyPeepNav:I '*:S' | grep 'screen=' | tail -n 1 || true)"
if [[ "$result" != *"screen=RESULT"* ]]; then
  echo "FAIL: expected victory RESULT, got $result"; exit 1;
fi
adb exec-out screencap -p > smoke-results/victory.png
tap_ui 470 1380
sleep 3
next="$(adb logcat -d -s SkyPeepNav:I '*:S' | grep 'screen=' | tail -n 1 || true)"
if [[ "$next" != *"screen=PLAY"* ]]; then
  echo "FAIL: NEXT from victory didn't open gameplay: $next"; exit 1;
fi
echo "PASS: victory scene and NEXT action."
# Regression for formerly oversized invisible RESULT hitboxes.
# Victory illustration draws Home/Next/Share at y=1308..1465 (top-left);
# GAME OVER reference draws them at about y=1240..1435.
adb logcat -c
adb shell am force-stop com.brendigo.skypeep
adb shell am start -W --ei sky_peep_qa_world 1 --ez sky_peep_qa_victory true \
  -n com.brendigo.skypeep/.MainActivity
capture victory-touch-before RESULT
tap_ui 320 1380
capture victory-gap RESULT
tap_ui 470 1210
capture victory-above RESULT
tap_ui 185 1380
capture victory-home MENU

adb logcat -c
adb shell am force-stop com.brendigo.skypeep
adb shell am start -W --ei sky_peep_qa_world 1 --ez sky_peep_qa_loss true \
  -n com.brendigo.skypeep/.MainActivity
capture loss-before RESULT
tap_ui 470 1210
capture loss-above RESULT
tap_ui 470 1490
capture loss-below RESULT
tap_ui 320 1380
capture loss-gutter RESULT
tap_ui 470 1380
capture loss-retry PLAY
echo "PASS: accurate result buttons and inert margins, HOME and loss RETRY."

# Verify that the newly introduced background motion actually renders on
# Android, not merely in the pure Kotlin crop tests. This debug-only fixture
# holds the bird still so normal gravity cannot end the run during capture.
adb logcat -c
adb shell am force-stop com.brendigo.skypeep
adb shell am start -W --ei sky_peep_qa_world 1 --ez sky_peep_qa_background_motion true \
  -n com.brendigo.skypeep/.MainActivity
sleep 3
capture background-motion-start PLAY
sleep 6
capture background-motion-end PLAY
offsets="$(adb logcat -d -s SkyPeepBackground:I '*:S' \
  | sed -n 's/.*offset_px=\([0-9][0-9]*\).*/\1/p' | sort -u | wc -l)"
echo "SkyPeep active background crop offsets: $offsets distinct"
if (( offsets < 2 )); then
  echo "FAIL: animated world background did not move during GL capture"; exit 1
fi
# A PAUSE freezes the actual game scene, including its independent motion clock.
tap_ui 868 65
capture background-motion-paused PAUSE
echo "PASS: animated background moves in active flight and PAUSE opens."

# Compare actual emulator pixels with the project reference artwork.
# This is a transparent diagnostic, not an unearned 1:1 certification.
java -Djava.awt.headless=true tools/CompareSkyPeepScreens.java
test -s smoke-results/visual-comparison.txt || {
  echo "FAIL: screenshot visual comparison report missing"; exit 1;
}
# SkyPeep now has three independent route licenses in Settings. Validate real
# taps, visible controls, saved selection and an actual process restart.
adb shell pm clear com.brendigo.skypeep >/dev/null
adb logcat -c
adb shell am start -W -n com.brendigo.skypeep/.MainActivity
sleep 5
adb logcat -d -s SkyPeepDifficulty:I '*:S' | grep -q 'loaded=EXPLORER' || {
  echo "FAIL: new installation did not default to original Explorer mode"; exit 1;
}
tap_ui 470 1300
capture route-main MENU
tap_ui 650 1400
capture route-settings SETTINGS
tap_ui 695 1040
capture route-tempest SETTINGS
adb logcat -d -s SkyPeepDifficulty:I '*:S' | grep -q 'selected=TEMPEST' || {
  echo "FAIL: difficulty selector not activated"; exit 1;
}
adb shell am force-stop com.brendigo.skypeep
adb logcat -c
adb shell am start -W -n com.brendigo.skypeep/.MainActivity
sleep 4
adb logcat -d -s SkyPeepDifficulty:I '*:S' | grep -q 'loaded=TEMPEST' || {
  echo "FAIL: difficulty was not persisted across cold restart"; exit 1;
}
# Android QA extras are ignored by non-debuggable production builds.
adb shell am force-stop com.brendigo.skypeep
adb logcat -c
adb shell am start -W --ei sky_peep_qa_world 4 \
  --ei sky_peep_qa_difficulty 2 --ez sky_peep_qa_background_motion true \
  -n com.brendigo.skypeep/.MainActivity
capture route-wind-gate PLAY
adb logcat -d -s SkyPeepDifficulty:I '*:S' | grep -q 'loaded=TEMPEST' || {
  echo "FAIL: Wind Gate did not start in the selected Tempest license"; exit 1;
}
echo "PASS: three-mode difficulty selection persists and Wind Gate runs."

echo "PASS: Android app process survives boot and splash navigation."
