# SkyPeep 0.3.13 — Source pixel preservation QA

## Scope
The supplied design reference remains authoritative. Screens and worlds must eventually match without using static screenshots as fake interactive UI.

### Changes in this iteration
- Sky Start and Cloud Pass: overwide art masks replaced with tight regions for baked HUD, bird, motion trails, pillars and individual coins. More original illustrated clouds, mountains, islands and waterfalls remain visible.
- Asset builder prints percentage of original midground retained and fails below 49% for either world.
- Gameplay bird now uses an undistorted 295×232 rendering rect consistent with the original 443×349 texture. Costumes, pickup positions and collisions follow the bird layout.
- Java real-emulator comparison reports an additional untouched original-island ROI metric for Sky Start alongside existing screen MAE.

### Corrective verification after v0.3.12

- PR #25 was merged and its preview published successfully, but a subsequent review detected that Cloud Pass's **lower fixed-pillar plume oval** also replaced genuine floating-island foliage and part of the waterfall.
- Removed the overwide oval centred at source (127,764), radius (218,52). The original illustrated foliage and waterfall remain unmodified in that section. Dynamic lower pillars still render from separate sprites and shafts.
- Added a **pixel-exact source-retention guard before JPEG compression** for reference-scene points (75,780), (100,780), (160,783), (100,800), and (245,810) in the 706×1254 generated artwork. Two additional samples (280,750) and (300,750) protect real white cloud crowns bordering the island. If a future source clean-up touches any of these legitimate scenery pixels, the Android build fails.
- Tightened the old photographed-bird mask in Cloud Pass from (207,670; radius 137×103) to (207,644; radius 141×100), retaining more of the original cloud bank beneath its flight path instead of painting it cyan. Original illustrated foreground pillars remain handled by separate pillar cleanup regions.
- In addition to seven source-pixel landmarks, the builder now samples the Cloud Pass illustrated island/bridge region x=40..279, y=750..824 at 4px intervals and requires **at least 88%** untouched source pixels. The former large lower-plume oval would fail this check.
- The mask-coverage and visual pixel-comparison metrics remain separate: more retained original pixels does **not** prove every game frame is 1:1.
- v0.3.13 uses Android versionCode 25; the unsigned AAB is not a production Play release.

### Still not certified as 1:1
- Crystal Drop, Wind Gate, Moon Glow and Sun Peak retain their older masks for now.
- Covered pixels behind photographed bird and obstacles are not recoverable without independent original layered artwork.
- Dynamic HUD typography, animation registration, safe areas and physical device GPU tests require additional QA.
- Google Play AAB signing is not available.

### Release gates
1. Gradle unit tests, source-hash-checked art, debug APK, unsigned AAB and manifest security checks.
2. Bird aspect/skin alignment JVM regressions.
3. Real Android emulator: six worlds, live input, pause, levels, menus, settings, result and saving.
4. Diagnostic screenshot comparison against user references. A green build is not blanket 1:1 proof.
5. Preview Release only after passing Android build and emulator checks.
