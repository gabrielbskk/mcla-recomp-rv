# Android ARM64 status

> **2026-11-14 scope update:** Mali/Immortalis diagnostics and PanVK distribution
> have been discontinued. Current development targets Snapdragon/Adreno.
> Historical Mali investigation notes below are not support instructions.
> See [the retirement rationale](android-mali-driver.md).

Last reviewed: 2026-11-12. This is the canonical handoff for the Android port.
The important historical decisions are summarized in
[android-history.md](android-history.md); the detailed build recipe is in
[android-build.md](android-build.md).

## Scope

The port recompiles the Xbox 360 PowerPC code to ARM64 C++ and runs it through
the ReXGlue runtime, SDL3 and Vulkan. It is native ARM64 Android integration,
not an x86 emulator or a Linux compatibility layer.

## Current release (0.1.25)

- [Android ARM64 v0.1.25](android-release.md) is debug-signed with the existing
  certificate and was installed with `pm install -r` on the Galaxy S23
  (Adreno 740, 8 GB RAM); versionCode 25 is what the device reports. The
  maintainer confirmed audio now plays without screen recording. Touch and the
  redesigned launcher still have no gameplay validation. The public release
  carries the APK as a release asset, not a tracked source file.
- 0.1.25 opens the normal AAudio mixer path instead of the low-latency stream
  by default (the fast stream was silent on the affected device until another
  stream opened the mixer), keeps `mcla_audio_low_latency` as the opt-in for
  the old behaviour, and requests audio focus from the game activity. Latency
  was not measured.
- A later local build (versionCode 26) adds an audio mode button to the
  launcher's Extras card, between the two paths. The maintainer reports better
  performance on the low-latency path and working audio only on the normal
  mixer path; a controlled before/after frame-rate measurement is still
  missing, so no default has been chosen from data. The touch overlay was
  confirmed working in gameplay on the same phone.
- 0.1.24 added the card-based launcher (Game files / Graphics / Extras /
  Launch), the generic **Select Vulkan driver (.zip)** label and the
  experimental Xbox-style touch overlay with a HUD editor. Touch input is not
  yet validated in gameplay; a gamepad remains the only validated method.
- The previous validated gameplay release remains
  [Android ARM64 v0.1.15](android-release.md), played on the same phone. The
  public release carries the tested APK and artwork as release assets, not
  tracked source files.
- The selected Turnip 27 build runs without the observed rear-light and wheel
  flashes **only with the opt-in** `TU_DEBUG=syncdraw` diagnostic switch on the
  tested phone. Without that switch the artifacts return. The system Vulkan
  driver avoided the observed artifacts without a switch, but was slower in
  the tested sessions. See the [A740 test matrix](turnip-a740-rendering-artifacts.md).
- The latest two-minute on-device sample observed the battery rise from
  32.9 to 38.8 C while the GPU maximum clock moved between 615 and 550 MHz,
  reaching 475 MHz near the end. Host-presentation FPS varied with the scene;
  the sample does not isolate a single cause of low FPS or stutter.
- Single-gamepad input remains the only validated in-game input. Keyboard and
  mouse do not work as Android in-game controls. The 0.1.24 touch overlay is
  included but not yet validated in gameplay, and a clean-profile import,
  16 KiB page runtime and broad GPU compatibility have not been established.
- Push CI runs repository checks. The full patched-SDK build is restricted to
  pull requests and manual dispatch. Manual run
  [43132894300](https://github.com/gabrielbskk/mcla-recomp-rv/actions/runs/43132894300)
  on `ae92602` passed both the clean patched-SDK cross-build and the host
  syntax check with generated-header stand-ins. This does **not** build an APK
  from game data or verify gameplay; a successful push check alone is not
  proof of a fresh rebuild.

## Historical test state (2026-11-09)

- The tested phone is a Samsung Galaxy S23 with an Adreno 740 GPU, 8 GB RAM and
  256 GB storage. Older logs show `realme/RMX5070` because device spoofing was
  enabled; that string is not the physical device model.
- Menus, audio, fullscreen and a 3D road scene with HUD reached gameplay in
  earlier installed candidates. Touch was used only for launcher/menu
  navigation; it is not an in-game control method.
- The Android launcher is in English and provides **SELECT TURNIP (.ZIP)**,
  **USE SYSTEM DRIVER**, **SELECT SHADER PACK (.SHADERPACK)** and **PLAY**.
- Single-gamepad input is the only currently supported and validated in-game
  input method. Two-gamepad/two-player input is not supported at this time.
  Keyboard and mouse support is possible through Android/SDL but remains
  unverified. Touch controls are planned and are not implemented yet.
- At the time, system Vulkan was the recommended choice without a workaround.
  Both tested Turnip variants loaded but showed rear-light flashes. Later
  tests established the opt-in `syncdraw` workaround described above.
- FPS remains low and stutters occur. GPU thermal and power limits were seen
  during testing, so no sustained performance improvement has been established.
- A local APK `0.1.4` (code 4) with the restored 720-line default was built,
  signed with the existing compatible key, installed on 2026-11-09, and opened
  on the S23 with the system Vulkan driver and no shader pack. The log confirmed
  the default without a user override and a screenshot showed the 3D city
  intro. Driving and sustained FPS in this exact build are not yet verified.
- APK `0.1.6` (code 6) fixes the launch rotation: SDL requested `FULL_USER`
  after Android requested landscape, so the game could return to portrait when
  device rotation was locked. The game activity now keeps landscape while SDL
  initializes. The user confirmed that rotation works on the S23. This build
  uses the same native game library as `0.1.4`; its Android launcher/activity
  code changed.
- APK `0.1.7` (code 7) was installed on 2026-11-09 with the same compatible
  signature. After installation, the private `mcla.toml` was backed up and
  `mcla_fence_sleep_us = 50` was set for a future test. No gameplay or
  performance result exists for this setting yet; `0.1.6` remains the
  measured reference.

## Android implementation

The SDK source is ReXGlue v0.10.0 at commit
`f5337cdc947ff6d4c4196737e2c807a48f2a1fc2`. The active patch recipe is:

1. `patches/rexglue-android-arm64.patch`
2. `patches/rexglue-turnip-loader.patch`
3. `patches/rexglue-driver-cache-path.patch`
4. `patches/rexglue-vulkan-pipeline-background-work.patch`
5. `patches/rexglue-vulkan-occlusion-query-pipeline-wait.patch`
6. `patches/rexglue-android-present-filters.patch`
7. `patches/rexglue-android-sdl-event-pump.patch`
8. `patches/rexglue-pipeline-storage-layout.patch`
9. `patches/rexglue-android-present-stats.patch`
10. `patches/rexglue-vulkan-frame-start-sync.patch` (experimental; disabled by default)
11. `patches/rexglue-android-bionic-link.patch` (omit nonexistent Android `pthread`/`rt` libraries)

The seventh patch removes one redundant initial Android SDL event-maintenance
pump when the event queue is empty and the entry pump already ran. It does not
change subsequent wakeups or polling intervals. It was a code candidate when
first documented and is not a measured performance fix.

The first patch is a reviewed combined SDK/fork delta, not an Android-only
patch. The source strategy and future dedicated-fork option are documented in
[android-sdk-source.md](android-sdk-source.md).

The Java launcher imports the 13 extracted game files into app-private storage,
loads an optional user-selected ARM64 Vulkan driver through AdrenoTools, imports
an optional `545407F8.shaderpack` into `files/shaderpack/`, and exports private
logs to `Documents/MCLA/logs/` through MediaStore. Vulkan cache files are
separated by driver identity and cache UUID. These final cache and log-name
changes compiled but still require focused runtime validation.

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
APK assets. The launcher now imports a user-provided pack into
`files/shaderpack/`, where the native importer finds it. A later import on the
device added 1,721 pipeline descriptions; its FPS effect remains unmeasured
under matched conditions. 782 pipelines were created in about five minutes,
288/215/173 per minute right after loading. The GPU was held at 475-550 MHz of
719 MHz by thermal levels 3-4. Tracked in
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
changes target that burst. Their effect on this exact car-load event has not
been isolated in a matched test:

- `vulkan_async_skip_incomplete_frames_max_ms` defaults to 40 ms on Android
  (`src/mcla_app.cpp`), so a frame that used a pipeline still being created
  holds the last picture for at most about one 30 FPS interval instead of
  100 ms.
- The new `patches/rexglue-vulkan-pipeline-background-work.patch` sets the
  pipeline creation and storage-writing threads to a lower priority on
  Android, and defers the driver pipeline-cache save until the creation queue
  has drained and no pipeline was created for 2 seconds, so a save of tens of
  megabytes does not compete with a loading car.
- The experimental 644-line guest video mode repeatedly faulted while loading
  on the tested Galaxy S23, with or without the shader pack. Setting
  `mcla_render_height = 720` in the app's `mcla.toml` allowed the same installed
  APK to reach gameplay. The rebuilt `0.1.4` APK also reached the 3D city intro
  with 720 as its compiled default. The game's own 3D render targets
  were already observed at a fixed 1280x720 (`sub_8263D2A8`), so a lower
  guest video mode was not established as a 3D performance improvement.
- With the system driver and shader pack, the user reports a small FPS loss.
  The 2026-11-09 log showed 1,721 pipeline descriptions added, zero shaders
  rebuilt, and one 13 ms occlusion-query pipeline creation. The same log showed
  a 1280x720 4x MSAA tiled pass. Source comments identify this as the world
  behind the pause menu or GPS map, not necessarily the ordinary driving
  scene. These observations do not establish the pack as the cause of lower
  sustained FPS; a controlled comparison is still needed. The next performance
  experiment should profile the fixed 1280x720 scene and the present-to-screen
  step separately, while retaining the working 720-line video mode.
- During the `0.1.4` intro test, the phone's GPU maximum was capped at 295 MHz
  (`thermal_pwrlevel = 7`); sampled GPU load was roughly 33–40% at that cap.
  This is an intro measurement, not a driving benchmark. Avoid comparing FPS
  across runs with different thermal limits.
- In an `0.1.6` startup log without the shader pack, Vulkan loaded about
  19.7 MB of driver pipeline-cache data and created 512 cached graphics
  pipelines in roughly 0.35 seconds. No occlusion-query pipeline wait over
  the 10 ms logging threshold appeared in six short startup logs. These logs
  do not cover a controlled driving session or the car-load stutter; changing
  the pipeline wait defaults on this evidence would be premature.
- A later short `0.1.6` run without the pack (`mcla_016.log`) prepared 516
  pipelines from Vulkan storage at startup and created only three later;
  there were no logged occlusion-query waits. A prior run with the pack
  (`mcla_009.log`) created 410 pipelines over the run and logged one 13 ms
  occlusion-query creation. The two runs differ in scene and cache state, so
  they cannot establish the pack's effect on FPS. Use
  `scripts/analyze_android_pipeline_log.py` on a matched driving comparison.
- A 150-second driving session on `0.1.6`, system driver, no shader pack,
  confirmed the reported stutter. SurfaceFlinger reported 2,873 presented
  frames at 14.223 average FPS; its present-to-present histogram had p50 66 ms,
  p95 110 ms, 437 intervals over 100 ms and 46 at least 200 ms. Of 150 GPU
  samples, the maximum frequency was capped at 220 MHz for 44 and 295 MHz for
  75; the remaining samples were 348 or 615 MHz. The native log prepared 520
  pipelines from storage at startup, created 20 afterward, and reported no
  occlusion-query wait above its 10 ms threshold. One thread of the host and
  one guest thread each used roughly a CPU core in a point-in-time `top`
  snapshot. These data show that pipeline creation alone does not explain the
  driving stutter; thermal limits and CPU-side work are stronger candidates.
  The raw GPU samples, SurfaceFlinger dump and native log are kept locally in
  ignored `out/profiles/2026-11-09-stutter/`.
- `mcla_fence_sleep_us` is an experimental CPU-side option for the guest's GPU
  fence polling hook. The default 0 preserves the tested yield-only behavior;
  a small value such as 50 sleeps that many microseconds after every 64 polls.
  A 50-us setting was installed in an earlier test build but has not had a
  matched A/B measurement. Check both frame times and rendering correctness
  before considering a new default.

The remaining candidate, now instrumented but not yet changed: a pipeline
first needed by a draw inside an occlusion query is still created on the GPU
command thread (the SDK documented up to 400 ms for one such frame), and one
queued by an earlier draw is waited for up to 500 ms. The new
`patches/rexglue-vulkan-occlusion-query-pipeline-wait.patch` logs how long
each of those two paths takes (a warning for every 10 ms or more), and adds two
cvars so the behavior can be tested without a new build:
`vulkan_occlusion_query_pipeline_wait_ms` (default 500, the SDK's own value;
0 leaves the draw out right away) and `vulkan_occlusion_query_async_pipelines`
(default off; on creates those pipelines in the background too and leaves the
draws out until they are ready, which the query counts as hidden). Both paths
still need to be timed on the device before the defaults are changed.

The rear-light issue appeared on the tested Turnip variants. The later
`syncdraw` workaround and the artifact-free system-driver alternative are
summarized above. A wheel flicker was also reported on Turnip without that
switch and has not yet been reproduced on the system driver. Do not treat the
historical spoofed model string or a single screenshot as evidence about the
physical device or a transient artifact. Tracked in
[issue 2](https://github.com/gabrielbskk/mcla-recomp-rv/issues/2) and
[issue 3](https://github.com/gabrielbskk/mcla-recomp-rv/issues/3).

The picture itself is the guest image scaled to the screen by the presenter,
with a bilinear filter. The new `patches/rexglue-android-present-filters.patch`
compiles the presenter's spatial scaling filters (FidelityFX's Contrast
Adaptive Sharpening and FSR 1, whose shaders are in the SDK tree and need
neither its source nor its runtime) into the Android build, so
`present_effect = "cas"` or `"fsr"` can be tried in `files/mcla.toml`. They
are off by default: each is another pass over the whole picture, and the GPU
is the limit on the tested phone. The temporal upscaler (`fsr2`, `fsr3`) still
needs FidelityFX's runtime, which this build does not have, and is not
offered. Not yet measured on the device.

## Verification policy

Static repository checks and CI may run without a phone or game data. A device
test requires an explicit test round with a defined hypothesis, driver,
thermal state, scene and pass/fail observation. Compilation alone must not be
described as a runtime fix. No additional APK or gameplay test is implied by
this documentation update.
