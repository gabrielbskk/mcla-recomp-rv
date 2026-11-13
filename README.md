<p align="center">
  <img src="docs/assets/mcla-recomp-project-cover.png" alt="Midnight Club Los Angeles Complete Edition: Android recompilation" width="480">
</p>

<h1 align="center">MCLA RECOMP <sub>RV</sub></h1>
<p align="center"><strong>LOS ANGELES, RECOMPILED.</strong><br>
An unofficial Android ARM64 port of <em>Midnight Club: Los Angeles Complete Edition</em> for Xbox 360.</p>

<p align="center">
  <a href="https://github.com/gabrielbskk/mcla-recomp-rv/releases/tag/stable-snap"><strong>DOWNLOAD: SNAPDRAGON + MALI DIAGNOSTIC</strong></a>
  &nbsp; · &nbsp;
  <a href="docs/android-testing.md">INSTALL &amp; PLAY</a>
  &nbsp; · &nbsp;
  <a href="docs/android-build.md">BUILD FROM SOURCE</a>
</p>

---

### 01 / THE DRIVE

Static recompilation brings the Xbox 360 game to native ARM64 using C++23,
SDL3, Vulkan and the ReXGlue runtime. This is **not** an x86 emulator, a
Linux compatibility layer, or an official release.

| Device / evidence | Graphics | Did it run? / compatibility |
| :--- | :--- | :--- |
| Galaxy S23 · Snapdragon 8 Gen 2 · Adreno 740 · 8 GB RAM (**maintainer test**) | System Vulkan / Turnip 27 | **Yes:** the maintainer reports that the current Snapdragon v0.1.45 performed well on this phone. Earlier builds reached menus, audio and driving; the current report has no comparable FPS/driver benchmark. |
| [Zenfone 9 (tester report, issue #5)](https://github.com/gabrielbskk/mcla-recomp-rv/issues/5) · Snapdragon 8+ Gen 1 · Adreno 730 · 6 GB RAM · Android 14 | Turnip 27 on v0.1.25; later system driver on v0.1.24/25 | **Reported to run, but poorly:** tester estimated 5–15 FPS with the system driver and reported freezes/lag. The displayed 60 FPS appeared inaccurate; there is no validated benchmark or v0.1.45 retest. |
| [Galaxy Z Fold 5 (tester report, issue #6)](https://github.com/gabrielbskk/mcla-recomp-rv/issues/6) · Snapdragon 8 Gen 2 · Adreno 740 · 12 GB RAM · Android 16 | Driver not established | **No confirmed gameplay:** v0.1.24 reached a file-I/O error screen. The tester reported trying three game copies; the cause remains unverified. No v0.1.45 retest. |
| Galaxy A06 · Helio G85 · Mali-G52 MC2 (**tester report**) | Stock Arm / tested PanVK | **No validated gameplay:** stock driver lacks a required Vulkan feature; PanVK failed swapchain creation. The Mali APK is diagnostic, not a compatibility claim. |
| RedMagic 11 Air · Snapdragon 8 Elite (**tester report**) | Adreno | **Audio test only:** stereo output confirmed in logs, but cuts reported. Gameplay performance/compatibility is not established; its APK is withheld. |

**Performance evidence (Galaxy S23, older test builds; not v0.1.45 benchmarks):**
a 150-second `0.1.6` drive with system Vulkan and no shader pack averaged
**14.223 presented FPS** (p95 frame interval 110 ms). A separate Turnip 27
run measured **22–24 presented FPS**, with different settings and thermal
limits. The Zenfone 9 tester's **5–15 FPS** is an estimate, not a measured
comparison; its reported 60 FPS display was suspect. These are not a
controlled driver comparison or a prediction for the current APK: cache
warm-up, shader compilation and thermal throttling can cause substantial
stutter. No comparable gameplay FPS measurement is available for the Fold 5,
Mali or RedMagic. See [Android test evidence](docs/android-status.md)
and [performance methodology](docs/android-performance-handoff.md).
The maintainer's positive v0.1.45 S23 result is qualitative; do not infer a
specific FPS from it.

**Likely requirements, not a verified minimum specification:** ARM64 Android
(APK declares API 23 / Android 6.0 as its *installation* minimum), a working
Vulkan driver with `vertexPipelineStoresAndAtomics` and compatible Android
swapchain support, and your own legally obtained Xbox 360 game files. The
maintainer's measured playable reference is a Snapdragon 8 Gen 2 / Adreno 740
phone with 8 GB RAM. A Zenfone 9 tester with **6 GB RAM** reported that older
builds ran, but slowly; a Fold 5 tester with **12 GB RAM** encountered a
file-I/O error. Neither establishes a reliable RAM minimum, and more RAM
alone does not guarantee compatibility. Plan for about **6.2 GB free for the app's
imported copy**, plus space for your extracted source files, shader caches
and Android itself; roughly **13 GB free** is a cautious starting estimate
when the source files are also stored on the device, not a measured hard
minimum. Android 10+ supports the documented shared-log export. Devices with
16 KiB memory pages and other chipsets still need runtime testing. Vulkan
1.3 is needed for optional FSR features, not established as the base game's
minimum.

The system Vulkan driver avoids the artifacts seen with Turnip on this device,
but was slower in tested runs. With the tested Turnip 27 build, rear lights and
wheels flicker unless the diagnostic `TU_DEBUG=syncdraw` switch is applied
before launch; v0.1.45 offers a launcher selector for this diagnostic switch.
Compare one setting at a time. Stutters and thermal
slowdown remain. See the [A740 test matrix](docs/turnip-a740-rendering-artifacts.md),
[known issues](KNOWN-ISSUES.md) and [Android status](docs/android-status.md).

The Mali APK is diagnostic only: gameplay is **not validated**. The RedMagic
11 Air / 8 Elite APK is withheld while reported audio cuts are investigated.
The `stable-snap` tag is not a new gameplay validation.

### 02 / GET ON THE ROAD

1. Choose [Snapdragon v0.1.45 or Mali diagnostic v0.1.23 from the same release](https://github.com/gabrielbskk/mcla-recomp-rv/releases/tag/stable-snap). Mali gameplay is not validated. The maintainer reports good results with v0.1.45 on the Galaxy S23, but there is no controlled FPS comparison with the previously recommended v0.1.24.
2. Supply the 13 extracted files from **your own legally obtained Xbox 360 copy**.
   The game data is not included; the import needs about 6.2 GB of free
   destination space in addition to your extracted source files.
3. Follow [the installation and import guide](docs/android-testing.md).

**Controls:** A single gamepad remains the only validated in-game input
method. Keyboard and mouse do not work as in-game controls in this Android
port. v0.1.24 includes an **experimental** Xbox-style touch overlay with analog
sticks, buttons, triggers and a HUD editor; it is included but not yet
validated in gameplay. The overlay hides when a gamepad connects and, without
a gamepad, hides after six seconds of inactivity and returns on touch. The
launcher has a new card layout and a generic **Select Vulkan driver (.zip)**
button for compatible driver packages. Use the launcher's **Graphics** card
to choose a driver; change visual settings inside the game's **Options / Display**
menu. Driver selection does not replace the in-game graphics settings.

The APK is **debug-signed** and includes a native library generated from the
game executable, but no extracted game assets, saves, Turnip package, shader
pack or signing key. It has been tested on a limited set of devices; clean-profile
import and 16 KiB memory-page runtime are not yet verified. The source
repository does **not** include game data, generated code, APKs or private
artifacts. Do not clear app data when updating if you want to keep your imported
game and saves.

**Logs:** Open `Documents/MCLA/logs/` in a file manager for the exported
`.txt` logs. The app keeps its original `.log` files privately under
`files/logs/`; reopen the launcher if a log was not exported after a crash.

### 03 / UNDER THE HOOD

The [Android build recipe](docs/android-build.md) pins ReXGlue SDK commit
`f5337cdc947ff6d4c4196737e2c807a48f2a1fc2` and applies the Android
patches in `patches/`. Push CI checks the repository; pull requests and manual
CI runs also build the patched SDK and check the host source without game
files. See the [release notes](docs/android-release.md)
for APK hashes, device test scope and remaining limitations.

---

<p align="center"><strong>COMMUNITY PROJECT · UNOFFICIAL PORT</strong></p>

This work builds on [holdmysocks/mcla-recomp](https://github.com/holdmysocks/mcla-recomp),
the [RenanQueiroz fork](https://github.com/RenanQueiroz/mcla-recomp), ReXGlue,
SDL3 and AdrenoTools. [Credits and provenance](CREDITS.md) ·
[Third-party notices](THIRD-PARTY-NOTICES.md) · [GPL-3.0-or-later](LICENSE).
This project is not affiliated with the game's rights holders. The original
game and its data remain the property of their respective owners.
