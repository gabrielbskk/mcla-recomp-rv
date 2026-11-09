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

The earlier warm-cache test reduced pipeline-storage preparation from about
15.8 seconds to 0.47 seconds on two runs, but this does not establish higher
sustained FPS. The tested phone uses 4 KiB pages; a 16 KiB page device has not
been tested. Clean-profile folder-picker import, final `.txt` log names,
per-driver cache behavior, two-gamepad/two-player input, long sessions and
formal FPS comparison remain open.

On the 2026-11-09 run (Turnip Mesa 27.0.0-devel git-79cb04951e, shadows and
anti-aliasing off, LOD 0.5, traffic distance 250, no pedestrians or parked
cars), SurfaceFlinger frame times were p50 33.4 ms (the 30 FPS cap), p95
83-92 ms and max 133-225 ms, for 22-24 FPS. The log warned that no shader pack
was present: `545407F8.shaderpack` exists in the desktop tree but is not in the
APK assets and the launcher does not import it. 782 pipelines were created in
about five minutes, 288/215/173 per minute right after loading. The GPU was held
at 475-550 MHz of 719 MHz by thermal levels 3-4. Tracked in
[issue 1](https://github.com/gabrielbskk/mcla-recomp-rv/issues/1).

The same day, after switching to the system Adreno driver, the first run
started with 0 bytes of driver pipeline cache and created 782 pipelines in
13.6 s (`Presenter: no guest frame for 15744 ms`); the next run loaded a
15,170,901-byte cache and created 808 pipelines in 0.5 s. The loading screen
then held 30.03 FPS at 615 MHz with the GPU 15% busy, but gameplay measured
13.39 FPS with the GPU 96% busy at 348 MHz: the GPU was thermally limited
(`max_clock_mhz` 295, `thermal_pwrlevel` 6, 81.2 C), so the driver comparison
is not clean. The system-driver runs also locked the aspect ratio from a
transient 1080x2340 surface, choosing a 1280x720 pillarboxed image instead of
1560x720; tracked in
[issue 4](https://github.com/gabrielbskk/mcla-recomp-rv/issues/4). The source
now takes the landscape orientation from that transient surface before using
it, and the guest video mode width follows the same aspect ratio; not yet
measured on the device.

The car-load burst was captured in the warm system-driver run: between
02:15:15 and 02:15:24 the GPU thread created 76 pipelines and loaded about
20 textures, the log skipped presenting a frame at 02:15:17, and the driver
pipeline-cache file grew to 18,417,450 bytes in the same window. Two source
changes now target that burst, not yet measured on the device:

- `vulkan_async_skip_incomplete_frames_max_ms` defaults to 40 ms on Android
  (`src/mcla_app.cpp`), so a frame that used a pipeline still being created
  holds the last picture for at most about one 30 FPS interval instead of
  100 ms.
- The new `patches/rexglue-vulkan-pipeline-background-work.patch` sets the
  pipeline creation and storage-writing threads to a lower priority on
  Android, and defers the driver pipeline-cache save until the creation queue
  has drained and no pipeline was created for 2 seconds, so a save of tens of
  megabytes does not compete with a loading car.
- `mcla_render_height` defaults to 644 on Android (`src/aspect_ratio.cpp`,
  `src/mcla_app.cpp`): the guest video mode becomes 1396x644 on a 19.5:9
  display instead of 1560x720, with the width following the display aspect
  ratio. The 3D scene and the HUD are drawn at that size and the presenter
  scales the picture to the phone's screen. On a display whose aspect would
  take the width below 1280 pixels (16:9 gives 1145), 720 is kept because the
  SDK reports a mode below 1280x720/720 lines as standard definition to the
  game. This is a fill-rate reduction, not a picture-quality option: it is
  still 644 lines of real geometry, upscaled by the presenter.

The remaining candidate, not changed: a pipeline first needed by a draw inside
an occlusion query is still created on the GPU command thread (the SDK
documented up to 400 ms for one such frame), and one queued by an earlier draw
is waited for up to 500 ms. Both paths should be timed on the device before
changing query behavior.

The rear-light issue is currently isolated to the tested Turnip variants. Keep
the system driver selected while investigating it. A wheel flicker was also
reported on the same Turnip run and has not yet been reproduced on the system
driver. Do not treat the historical spoofed model string or a single screenshot
as evidence about the physical device or a transient artifact. Tracked in
[issue 2](https://github.com/gabrielbskk/mcla-recomp-rv/issues/2) and
[issue 3](https://github.com/gabrielbskk/mcla-recomp-rv/issues/3).

## Verification policy

Static repository checks and CI may run without a phone or game data. A device
test requires an explicit test round with a defined hypothesis, driver,
thermal state, scene and pass/fail observation. Compilation alone must not be
described as a runtime fix. No additional APK or gameplay test is implied by
this documentation update.
