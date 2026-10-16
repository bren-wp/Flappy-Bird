# SkyPeep v0.3.11 — code and resource audit

Scope: Kotlin application source, all Java asset processing helpers, the current Android Gradle configuration, instrumentation smoke script, offline permission posture, GPU/audio resource lifecycle and save-key compatibility.

## Removed or repaired

- Removed the unreferenced `kotlin.math.sqrt` import from `SkyPeepGame.kt`.
- Removed the unused `score` chrome palette entry and its unnecessary small GPU texture.
- Replaced the nonfunctional `LANGUAGE: ENGLISH` settings row with an actual three-way difficulty selector.
- Moved new physics, difficulty rules and precision reward math out of the large renderer into `SkyRouteRules.kt`, a pure and unit-testable module.
- Added a reproducible `tools/dead_code_audit.py` CI gate scanning production Kotlin/Java files for unused imports and unreferenced private declarations.
- Reused the established local save file, retaining the original `best`, `stars_N`, `unlocked`, `coins`, `gems`, `skin` and `trail_selected` keys. New `route_stars_MODE_WORLD` and `route_best_MODE` keys are additive; prior star data migrates into Explorer.
- Main game disposes textures, fonts, SpriteBatch, sounds, music, costumes, pillar textures and generated flight trails in its existing `dispose()`. Existing screen/world textures are disposed when switching screens/worlds.

## Runtime and static checks

The GitHub Actions workflow executes Kotlin/Java dead-code analysis, Gradle unit tests, debug APK and unsigned AAB builds, source and merged-manifest permission validation, packaged-artwork integrity/native ABI checks, and Android emulator navigation / gameplay smoke with screenshots.

The tests now also cover three difficulty modes across six worlds, legacy Explorer physics compatibility, deterministic world air currents and precision combo/star thresholds. The emulator validates the visible difficulty control and saving its selection across a cold restart.

## Remaining structural and visual work

- `SkyPeepGame.kt` is still a large UI/gameplay controller. Further separation into scene and input components requires additional emulator regression coverage.
- Static source analysis cannot prove reflection/dynamic asset reachability; bundled image, audio and native resources are checked independently during Android build.
- Pixel-perfect visual comparison remains incomplete: the six backgrounds and game screen still differ materially from the reference illustrations. CI metrics are diagnostic, not a blanket 1:1 guarantee.
- Physical Android device GPU/memory profiling, signed Play release and real-device lifecycle testing remain pending.

Do not delete source artwork, audio or the dedicated existing tests solely because a basic text search shows no direct reference: filenames are resolved dynamically and art is assembled from hashed chunks.
