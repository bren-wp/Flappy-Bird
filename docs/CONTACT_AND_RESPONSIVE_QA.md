# SkyPeep 0.3.15 — Fair Contact and Edge-to-Edge Android QA

## Collision correction

Prior versions used a broad axis-aligned collision rectangle across the animated bird's wing/scarf/transparent margins and almost the entire illustrated stone-cap width. This caused a failure before the visible torso actually touched a pillar.

The new pure Kotlin `SkyPeepContact` subsystem uses the bird's **central-body ellipse** (68×65 virtual-pixel radii), excludes the cap's transparent side margins (22 virtual pixels), and tests ellipse-to-rectangle nearest-point penetration against the top and bottom walls separately. Tangency, a corner near-miss and a scarf/wing near-miss do not end the flight. Real stone penetration does. Floor/ceiling contacts are checked against the visible world bounds, not the old hidden HUD/floor margins.

`SkyPeepContactTest` validates near-wall safety, contact vs tangency, diagonal-corner false positives, transparent cap margins, real solid contact and exact world boundaries. The legacy `FlightRules` pickup and progression APIs remain intact.

## Fullscreen without deformation

The game still uses the original **941×1672 design coordinates**, including the photographed, interactive menu, map, skins, pause and results illustrations. `ExtendViewport` fills the entire device display while maintaining the original design aspect: on screens with additional vertical/horizontal room, only the exterior region is filled with narrow edge samples from the existing illustrated screen. There is no stretched bird, warped button or global scrolling.

The virtual reference rectangle remains centred and untouched; a `SkyPeepScreenFrame` geometry class computes actual visible top/bottom/left/right bounds. Gameplay score/header/pause stay anchored to the device's **physical top** on tall displays. Stone shafts extend to the physical boundaries. Ordinary menu/dialogue buttons work only in the original artwork region; gameplay taps outside that region still flap.

## UI polish

Shared buttons used by Settings, Shop, Pause overlays and Victory now have more reference-like espresso outlines, subtle rounded highlights, bevelled edges and shaded lower surfaces. Shop and Settings use the original transparent SkyPeep bird mascot, including the player's equipped costume, without static screenshots standing in for interactive UI.

## Device testing (real Android emulator)

CI takes actual screenshots and tests original menu/PLAY/PAUSE touch targets at:

- 1080×2400 (very tall)
- 720×1280 (compact phone)
- 1200×1920 (wider screen/tablet)
- Original emulator resolution plus standard all-six-world gameplay, settings, map, shop, skins, loss, victory and 18-route persistence.

The JDK-only `tools/ResponsiveViewportQa.java` compares the centre of every new screenshot to the reference image and checks that extended top/bottom or side areas are painted, rather than being unfilled letterbox bars. The report is uploaded with the emulator artifact.

## Open visual fidelity work

This update corrects collision fairness and viewport scaling, and improves UI chrome. It **does not prove total 1:1 fidelity** across every Android aspect ratio. The original art remains aligned 1:1 *within the centred reference canvas*, but the margins must show additional illustration rather than independently authored extension art. Custom Settings/Shop screenshots were not among the supplied original reference images, so those screens follow the established cartoon visual language rather than claiming an exact pixel match to an absent reference. Crystal Drop, Wind Gate, Moon Glow and Sun Peak still require world-layer visual reconstruction, physical device/notch testing, and signed Google Play build configuration.
