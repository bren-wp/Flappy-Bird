# SkyPeep Android — Static QA / hardening audit, 0.3.7

This is a documented code-level audit of the Android application and its CI
pipeline, **not** a claim of full device testing, malware certification, or
pixel-exact matching to the design references.

## Concrete changes

| Finding | Severity | Action |
|---|---|---|
| Production wallet balances in `SkyPeepShop` logcat | Privacy | Removed balance values from runtime logs; retained only cosmetic name for navigation QA |
| Unchecked `Int` arithmetic on coins, gems and level rewards | Stability | Saturating credit helper, direct unit tests including `Int.MAX_VALUE` |
| Unused `centered()` renderer | Dead code | Deleted |
| `elapsed` timer written/reset but never read | Dead code | Deleted |
| Sharing chooser launched after destroyed Activity or without handler | Stability | Guard lifecycle and catch `ActivityNotFoundException` |
| Workflow-wide `contents: write` permissions | CI security | Read-only build/emulator jobs; isolated write-enabled release job only on successful trusted branch push |
| No automated manifest permission regression gate | Privacy | Verify source **and effective merged debug/release manifests**; reject network/location/contacts/SMS permissions and backup/cleartext regressions introduced by any dependency |

## Reviewed, retained deliberately

- Launcher activity must be exported to receive Android LAUNCHER intents.
- `allowBackup=false` and `usesCleartextTraffic=false` are explicitly declared.
- Local-only Android Preferences store progress and settings; no accounts or analytics SDK are declared.
- `Texture`, `Pixmap`, `Sound`, `Music`, `SpriteBatch`, fonts, and UI chrome follow explicit dispose calls.
- Level assets are loaded for the active world and released on world changes.
- Original art is reconstructed from a fixed SHA-256-verified asset pack during Gradle builds.
- Debug-only QA Intent flags are rejected in non-debuggable builds.

## Open items requiring separate verification

- Pixel-exact comparisons of every screen and every world against the supplied artwork.
- Device matrix for different aspect ratios, GPUs, Android API levels and 60/90/120 Hz.
- App bundle signing, Google Play policy review, store privacy declaration and physical-device release test.
- Extended automated performance profiling and Android lint/static analyzer integration.
- The monolithic `SkyPeepGame.kt` can be split into smaller state/render/input units once visual parity is stabilized; broad refactoring now would increase regression risk.

## Release criteria

JVM tests, Android APK/AAB build, original assets, and all existing emulator
scenarios must pass before publishing. The bundle remains unsigned until a
verified signing workflow is configured.
