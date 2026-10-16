# SkyPeep 0.3.12 — Source pixel preservation QA

## Scope
The supplied design reference remains authoritative. Screens and worlds must eventually match without using static screenshots as fake interactive UI.

### Changes in this iteration
- Sky Start and Cloud Pass: overwide art masks replaced with tight regions for baked HUD, bird, motion trails, pillars and individual coins. More original illustrated clouds, mountains, islands and waterfalls remain visible.
- Asset builder prints percentage of original midground retained and fails below 49% for either world.
- Gameplay bird now uses an undistorted 295×232 rendering rect consistent with the original 443×349 texture. Costumes, pickup positions and collisions follow the bird layout.
- Java real-emulator comparison reports an additional untouched original-island ROI metric for Sky Start alongside existing screen MAE.

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
