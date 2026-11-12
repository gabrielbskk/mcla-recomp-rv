# Android ARM64 release notes

## Version 0.1.24

APK `MCLA-Recomp-Snapdragon-Launcher-v0.1.24.apk` (versionCode 24), SHA-256
`718b3c138554a69ed210b1f301cd707bdf127cd72e1f4e2a07dac6fce2623f84`.
Debug-signed with the same certificate as the installed test builds, so it
updates over an existing installation without clearing app data. It was
installed with `pm install -r` on the Samsung Galaxy S23 (Adreno 740, 8 GB
RAM) and reports `versionName=0.1.24`; gameplay, the touch overlay and the new
launcher were **not** yet re-validated in a device session after this build.

Changes since 0.1.15:

- **Launcher redesign.** Dark card layout with separate *Game files*,
  *Graphics*, *Extras* and *Launch* sections, the project cover as the app
  icon, an explicit driver status line, and a **Select Vulkan driver (.zip)**
  button that replaces the Turnip-only label. The same ZIP import path still
  expects an AdrenoTools-compatible driver saved as
  `libvulkan_freedreno.so`. No Vulkan driver is shipped in the APK.
- **Xbox-style touch controls (experimental).** An on-screen overlay with
  analog sticks, face buttons, bumpers, triggers, dpad and start/back, plus a
  HUD editor for size and position. The overlay hides automatically when a
  gamepad is detected. Without a gamepad it hides after six seconds of
  inactivity and returns on touch. The layout is stored in app-private
  storage. Touch input has not yet been validated in gameplay, so a gamepad
  remains the only validated control method.
- No change to the Vulkan defaults: the system driver remains the safe default
  and the Turnip `TU_DEBUG=syncdraw` workaround still has to be placed in
  `files/driver.env` before launch.

The APK contains `libmain.so`, generated from the game's executable, but no
ISO, extracted game assets, saves, shader pack, Vulkan driver ZIP or signing
key. The source repository does not include any of these private artifacts.
Do not clear app data when updating if you want to keep the imported files and
saves. Exported logs are in `Documents/MCLA/logs/` as `.txt` files; original
`.log` files remain in the app-private `files/logs/`.

## Version 0.1.15

APK `MCLA-Recomp-arm64-v0.1.15.apk` (versionCode 15), SHA-256
`f436dc6742629e7e26410d406e1786f8183b364825535c0d4b825367d318e012`.
Debug-signed with the same signing certificate as the installed test builds.
The APK was installed and tested on a Samsung Galaxy S23 with Adreno 740,
8 GB RAM and Turnip Mesa 27.0.0-devel (`git-79cb04951e`). No clean-profile
import, other SoCs or 16 KiB page-size runtime were validated. See
[testing instructions](android-testing.md) and the
[A740 rendering test matrix](turnip-a740-rendering-artifacts.md).

This APK adds a diagnostic, allowlisted `files/driver.env` switch for Turnip
`TU_DEBUG` and opt-in Vulkan synchronization experiments (disabled by default).
The best tested playable combination on the Galaxy S23 is Turnip 27 with
`TU_DEBUG=syncdraw` and threaded submission on: both observed rear-light and
wheel artifacts disappear, while frame rate and stability still depend on
scene and temperature. The 0.1.15 selective synchronization experiment did
not fix the artifacts and reduced performance. The system Vulkan driver
avoided the observed artifacts without a driver debug switch, but ran slower
in the tested sessions. The FPS overlay reports host presentations and is
configured to match the requested `mcla_fps` target, not display scanout.

The APK contains `libmain.so`, generated from the game's executable, but no
ISO, extracted game assets, saves, shader pack, Turnip driver ZIP or signing
key. The **source repository** does not include generated game code or any of
these private artifacts. Users must supply their own legally obtained game
files. The prior release below is preserved for reference; do not replace or
delete its APK asset when publishing this version.

Input: a single gamepad is the only validated in-game control method.
Keyboard and mouse do not work as in-game controls in this Android port.
Xbox-style touch controls are in development and are not supported by this APK.

For troubleshooting, exported logs are in `Documents/MCLA/logs/` as `.txt`
files. The original `.log` files remain in the app-private `files/logs/`.

## 2026-11-08 development APK (previous release)

This release contains the **same APK** delivered locally on 2026-11-08, not a newer rebuild. It is an experimental, debug-signed build for ARM64 Android. The game data is **not** in the APK. Users must provide and import the 13 extracted files from their own copy of *Midnight Club: Los Angeles Complete Edition* for Xbox 360 (Title ID `545407F8`). See [Android testing and import](android-testing.md).

Expected asset name: `MCLA-Recomp-arm64.apk` (about 105 MiB). SHA-256: `94b5fc1ac47556efa790b858099ed96226fff9f15c5826de44133854a519de3e`.

The package contains `libmain.so`, which is generated from the game's executable. Unlike the source repository, the APK therefore contains generated game code. It contains no ISO, extracted game assets, save files, Turnip driver ZIP, personal credentials, or private debug signing key. This previous release and its APK asset are preserved; a future source-only distribution would be a separate decision.

The last installed candidates reached gameplay on the maintainer's Samsung Galaxy S23, 8 GB RAM and 256 GB storage. Runtime logs reported a spoofed `realme/RMX5070` identity, so that string must not be treated as the physical device model. The GPU reported by Vulkan was Adreno 740. The **exact release APK was not installed or played** after the final per-driver Vulkan cache and log-export name changes. It passed build, ELF64 AArch64 and 16 KiB LOAD-alignment checks, ZIP alignment and APK signature checks. A device with 16 KiB memory pages has not been tested.

Known limits:

- Use the system Vulkan driver on the tested phone. Turnip OneUI and Standard v27.0.0-20261107 both caused rear lights to flash white. The system driver removed the flash in the user's test, with somewhat lower/uneven performance.
- Sustained FPS remains low and stutters occur; GPU thermal/power limits were observed during tests. A controlled performance comparison has not been completed.
- Empty-profile game-folder import, final `.txt` log naming, the new per-driver cache, two-gamepad/two-player input, long-session stability and 16 KiB page runtime remain unverified.
- The launcher checks that the 13 game files exist and are nonempty, but does not verify their hashes. A damaged file may pass import. An imported Turnip ZIP contains native code; use only a trusted source.

Source provenance and AI assistance are explained in [CREDITS.md](../CREDITS.md) and [README.md](../README.md). Technical evidence and the exact build steps are in [android-status.md](android-status.md) and [android-build.md](android-build.md).
