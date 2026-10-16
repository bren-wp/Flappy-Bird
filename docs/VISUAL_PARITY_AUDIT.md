# SkyPeep 0.3.14 — Source pixel preservation QA

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
- The independently reconstructed lower-pillar cap remains covered by its own mask. A build-time geometry check verifies coverage at (400,830) and its existing left overlap (345,810); it deliberately does **not** assume these positions have particular RGB colours in the immutable source package. Source-art pixel values and a separately resized design reference may differ.
- The mask-coverage and visual pixel-comparison metrics remain separate: more retained original pixels does **not** prove every game frame is 1:1.
- v0.3.14 uses Android versionCode 26; the unsigned AAB is not a production Play release.

### Crystal Drop and Wind Gate — v0.3.14 artwork restoration

- Removed the old horizontal erasure bands that destroyed the moon, floating islands, distant crystals, castles, waterfalls and stone bridges in worlds 3–4.
- Replaced the Crystal Drop top/bottom fixed-pillar and static bird masks with narrow rectangles and organic ovals, preserving the photographic moon, landscape and ruins. Verified original-scene sample pixels at seven landmarks in the 706×1254 immutable source.
- Replaced Wind Gate's massive fixed arch rectangles with round-ended **capsule masks** that follow the irregular articulated stone silhouettes, plus targeted static-bird removal. Six original-scenery landmarks are protected byte-exactly before JPEG compression.
- Three photographic Wind Gate coins are removed with independent round masks and local blue-sky annulus inpainting, leaving the white wind-flow lines and the illustrated islands outside the masks.
- The artwork builder now enforces **>=48% original midground retention in each of worlds 3 and 4**, checks mask coverage of baked bird/obstacle centres, and retains all prior world 1–2 source-pixel regression gates.
- Existing separately animated 2D bird, world-specific obstacle sprites, live pickups, dynamic HUD, currency, and 18 Sky Route challenges are unchanged.

### Still not certified as 1:1
- Moon Glow and Sun Peak retain their older background masks for now; their reference-grade independent gameplay scene layers still require visual reconstruction.
- Covered pixels behind photographed bird and obstacles are not recoverable without independent original layered artwork.
- Dynamic HUD typography, animation registration, safe areas and physical device GPU tests require additional QA.
- Google Play AAB signing is not available.

### Release gates
1. Gradle unit tests, source-hash-checked art, debug APK, unsigned AAB and manifest security checks.
2. Bird aspect/skin alignment JVM regressions.
3. Real Android emulator: six worlds, live input, pause, levels, menus, settings, result and saving.
4. Diagnostic screenshot comparison against user references. A green build is not blanket 1:1 proof.
5. Preview Release only after passing Android build and emulator checks.
