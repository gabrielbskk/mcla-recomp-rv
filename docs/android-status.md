# Android ARM64 status

Last reviewed: 2026-11-08. This is the canonical handoff for the Android port.
The important historical decisions are summarized in
[android-history.md](android-history.md); the detailed build recipe is in
[android-build.md](android-build.md).

## Scope

The port recompiles the Xbox 360 PowerPC code to ARM64 C++ and runs it through
the ReXGlue runtime, SDL3 and Vulkan. It is native ARM64 Android integration,
not an x86 emulator or a Linux compatibility layer.

## Verified state

- The tested phone is a Samsung Galaxy S23 with an Adreno 740 GPU, 8 GB RAM and
  256 GB storage. Older logs show `realme/RMX5070` because device spoofing was
  enabled; that string is not the physical device model.
- Menus, audio, fullscreen and a 3D road scene with HUD reached gameplay in
  earlier installed candidates. Touch was used only for launcher/menu
  navigation; it is not an in-game control method.
- The Android launcher is in English and provides **SELECT TURNIP (.ZIP)**,
  **USE SYSTEM DRIVER** and **PLAY**.
- Single-gamepad input is the only currently supported and validated in-game
  input method. Two-gamepad/two-player input is not supported at this time.
  Keyboard and mouse support is possible through Android/SDL but remains
  unverified. Touch controls are planned and are not implemented yet.
- The system Vulkan driver is the recommended choice on the tested phone.
  Both tested Turnip variants loaded, but both produced a rear-light flashing
  artifact. The system driver removed that artifact in the user's comparison.
- FPS remains low and stutters occur. GPU thermal and power limits were seen
  during testing, so no sustained performance improvement has been established.
- The final local APK was built and statically checked, but that exact APK was
  not installed or played after the final cache and log-export changes.

## Android implementation

The SDK source is ReXGlue v0.10.0 at commit
`f5337cdc947ff6d4c4196737e2c807a48f2a1fc2`. The active patch recipe is:

1. `patches/rexglue-android-arm64.patch`
2. `patches/rexglue-turnip-loader.patch`
3. `patches/rexglue-driver-cache-path.patch`

The first patch is a reviewed combined SDK/fork delta, not an Android-only
patch. The source strategy and future dedicated-fork option are documented in
[android-sdk-source.md](android-sdk-source.md).

The Java launcher imports the 13 extracted game files into app-private storage,
loads an optional user-selected ARM64 Vulkan driver through AdrenoTools, and
exports private logs to `Documents/MCLA/logs/` through MediaStore. Vulkan cache
files are separated by driver identity and cache UUID. These final cache and
log-name changes compiled but still require focused runtime validation.

## Evidence and limits

The Android FPS overlay was validated on the Galaxy S23 during gameplay. It
counts game frame redraws over a half-second window and does not request
additional presents. A captured driving scene showed 16.9 FPS with the overlay
visible in the top-left corner. Generated frames, if enabled later, would need
a separate presentation-rate measurement.

The earlier warm-cache test reduced pipeline-storage preparation from about
15.8 seconds to 0.47 seconds on two runs, but this does not establish higher
sustained FPS. The tested phone uses 4 KiB pages; a 16 KiB page device has not
been tested. Clean-profile folder-picker import, final `.txt` log names,
per-driver cache behavior, two-gamepad/two-player input, long sessions and
formal FPS comparison remain open.

The rear-light issue is currently isolated to the tested Turnip variants. Keep
the system driver selected while investigating it. Do not treat the historical
spoofed model string or a single screenshot as evidence about the physical
device or a transient artifact.

## Verification policy

Static repository checks and CI may run without a phone or game data. A device
test requires an explicit test round with a defined hypothesis, driver,
thermal state, scene and pass/fail observation. Compilation alone must not be
described as a runtime fix. No additional APK or gameplay test is implied by
this documentation update.
