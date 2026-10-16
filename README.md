<div align="center">

# 🐦 SkyPeep

**Six fantastic sky worlds. One brave little aviator. Your next flight starts here.**

![SkyPeep gameplay](assets/previews/gameplay.jpg)

A vivid 2D Android arcade adventure with floating islands, enchanted ruins, glowing crystals and a blue bird with a red scarf.

</div>

## Sky Route adventures — 18 challenges

Choose a flight license in **Settings → Difficulty** and master all six worlds:

- **Breeze:** wider sky corridors, gentler gravity and longer flights to practise the route.
- **Explorer:** the classic SkyPeep difficulty and default for existing installs; older world unlocks and scores stay intact.
- **Tempest:** stronger winds, faster scenery and narrower precision routes with higher completion rewards.

Each world has its own air-current profile. Aim for the **centre of the opening** to build precision streaks: every third precise passage awards an optional coin bonus, while a completed route earns one to three stars based on flight accuracy. Records and earned stars are tracked separately for each license, so there are **6 worlds × 3 routes = 18 challenges** without overwriting existing progress. There are no purchases or network services required to access difficulty modes.

## Explore the sky

The pictures below are the **user-provided design references**, not substitute screenshots from an Android emulator. Actual emulator captures and logs are downloadable from the Android CI run's `SkyPeep-Emulator-Smoke-*` artifact.

| Home | Six worlds | Your bird |
|:---:|:---:|:---:|
| ![Main menu](assets/previews/menu.jpg) | ![Level select](assets/previews/levels.jpg) | ![Skins](assets/previews/skins.jpg) |

Fly through **Sky Start**, **Cloud Pass**, **Crystal Drop**, **Wind Gate**, **Moon Glow** and **Sun Peak**. Each world features distinct, original 2D scenery and obstacle styling.

### What is inside

- 🌬️ Three selectable Sky Route flight licenses with world-specific currents and 18 challenge variants
- 🧭 Optional precision-centre streaks, skill-based one-to-three-star victories and independent mode records
- 🎮 Responsive touch-to-flap flight and score progression
- 🗺️ Six themed levels with stars and unlocks
- 🐦 Three-frame character flight animation with distinct Captain, Forest, Cosmic and Royal 2D costumes
- 🌈 Dedicated Flight Trails shop with four cosmetics, real unlock and equip actions, and saved coin/gem transactions
- ✨ Collectible coins, gems and alternate bird skins
- 🏆 Personal bests and locally saved progress
- ⭐ Dedicated Level Complete screen, three-star rewards and working Home / Next / Share actions
- 🔊 Music, sound effects and safe pause/resume when returning to Android
- 🚀 Lower texture-memory usage with on-demand menu and active-world-only assets
- 🌤️ Clean six-world flight backgrounds with moving bird, gates, HUD and pickups rendered in separate 2D layers
- 🏝️ Original photographic floating-island detail better preserved in Sky Start, Cloud Pass, Crystal Drop and Wind Gate, including protected moon, waterfalls and ruined bridges
- 🪙 Original illustrated coin and gemstone HUD plates cleaned of baked-in demo digits; balances update dynamically without replacing their artwork
- 🪽 Six independently colour-matched winged score plates with live outlined points instead of baked-in sample value 27
- 🎯 Exact victory and game-over button hit areas; empty margins and gutters cannot trigger an accidental navigation
- ⭐ Live level map: selected worlds, separately marked unlocks and completions, and earned three-star progress
- 🏛️ Textured 2D stone shafts anchored to both screen boundaries for every illustrated obstacle
- ☁️ Soft unified cloud layers and distant world-specific mountain silhouettes, with more accurate collectible hitboxes
- 📤 Share your score through Android
- 🔐 Offline by design, with no network or location permission; safer local score sharing and resilient progress rewards

## Android builds and quality assurance

CI runs Gradle unit tests, restores and validates the six-world artwork, checks Android permissions and native ABIs, executes a Kotlin/Java dead-code audit, and captures real Android emulator screenshots including difficulty selection and cold-restart persistence. A successful Gradle compile alone is not treated as proof of game readiness.

## Android builds

Download the newest **preview** release from [GitHub Releases](https://github.com/bren-wp/Flappy-Bird/releases). Debug APKs are installable for testing; bundled AABs are intentionally **unsigned** until release signing is configured. The app's final testing and store readiness have not been certified.

## Credits

SkyPeep illustrations were created for this project using the user-provided design references. Android implementation uses Kotlin and [libGDX](https://libgdx.com/). See [LICENSE](LICENSE) for repository licensing.