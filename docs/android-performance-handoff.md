# Android performance handoff — 2026-11-09

Read this after [AI handoff](ai-handoff.md) and [Android status](android-status.md).
This page records what the S23 runs proved, what they did not prove, and the
next experiments. The raw device logs and samples are private and ignored by
Git. Do not interpret compilation or an APK install as a gameplay fix.

## Tested configuration and user observations

The device is a Samsung Galaxy S23, 8 GB RAM, 256 GB storage, Adreno 740.
The user confirmed that one gamepad works and that the `0.1.6` launch rotation
works. With the system Vulkan driver, the user saw no rear-light flash that
appeared with tested Turnip variants. The user reports low FPS and intermittent
stutters. They also report that using the shader pack with the system driver
costs a little FPS; this is an observation, not yet a controlled comparison.

The experimental 644-line guest video mode caused repeated guest access
violations during loading, with and without the shader pack. The same APK
reached the road after `mcla_render_height = 720` was set. The Android source
default is now 720. The `0.1.4` APK was installed and reached the 3D city
intro without a user override. The `0.1.6` APK added the verified landscape
fix. It is the installed build and the working reference for performance
experiments. Version numbers in the APK are now correct; an earlier manifest
hardcoded `0.1.0` despite the packaging script's higher version output.

`0.1.7` is an **installed experimental APK** as of 2026-11-09. It adds
`mcla_fence_sleep_us`, default 0, so its new CPU-wait behavior is off until
selected in `files/mcla.toml`. It compiled, packaged and installed with the
compatible signature; no gameplay or performance result exists for it yet.
Keep the backed-up and measured `0.1.6` as the reference. The installed
`mcla.toml` did not contain `mcla_fence_sleep_us` at installation, so the
experimental sleep was off.

## Long driving profile on `0.1.6`

The user drove for a 150-second capture with the system driver and **no shader
pack**. The game was visibly on the road; the session was long enough to
capture the reported stutter. SurfaceFlinger's MCLA SurfaceView reported:

| Measure | Result |
| --- | ---: |
| Presented frames | 2,873 |
| Average presented FPS | 14.223 |
| Present-to-present p50 | about 66 ms |
| Present-to-present p95 | about 110 ms |
| Intervals over 100 ms | 437 |
| Intervals at least 200 ms | 46 |
| Longest populated histogram bin | 800 ms |

The histogram is binned, so the percentiles are approximate. It covers
startup and driving; it does not identify the exact frame or game event that
caused each pause.

Of 150 one-second KGSL samples, the maximum permitted GPU clock was 220 MHz
in 44 samples, 295 MHz in 75, 348 MHz in 9 and 615 MHz in 22. The phone
therefore spent 119 of 150 samples capped at 295 MHz or less. A point-in-time
`top -H` sample showed the SDL thread around 94% CPU and one guest thread
around 91% CPU, with other game and GPU threads also active. That snapshot
cannot identify the hot functions. The app had about 26 MiB in swap at that
point; total system swap use alone does not prove paging caused the stutter.

The native log loaded about 19.7 MB of Vulkan driver cache and prepared 520
graphics pipelines from storage at startup in about 0.35 seconds. Only 20
pipelines were created later in this run, and the occlusion-query patch logged
no wait or creation at or above its 10 ms threshold. Pipeline creation may
still explain particular earlier car-load pauses, but it does **not** explain
the bulk of the measured long-session stutter by itself. The thermal limit and
CPU activity are stronger leads; their causal shares remain unmeasured.

Earlier, a run with the shader pack added 1,721 pipeline descriptions, rebuilt
zero shaders, created 410 graphics pipelines over that run and logged one
13 ms occlusion-query pipeline creation. The two runs used different scenes and
cache states. Their counts must not be compared as a pack FPS benchmark.

## Evidence and code entry points

Private raw evidence on the S23 is in the ignored
`out/profiles/2026-11-09-stutter/` directory: `gpu.csv`, the MCLA native log
and the full SurfaceFlinger timestats dump. Do not commit those files; the
SurfaceFlinger dump can name other apps. Native logs also live in the app's
private `files/logs/` and are exported to `Documents/MCLA/logs/` by the app.
The live analysis script is `scripts/analyze_android_pipeline_log.py`.

- `src/render_perf.cpp` and `config/render_perf.toml`: the guest GPU fence
  polling hook. Existing `mcla_fence_yield = true` yields every 64 polls but
  can still consume CPU. The new `mcla_fence_sleep_us` is an optional sleep at
  that point, default 0, range 0–1000 microseconds. A small value such as 50
  is a hypothesis to test, not a confirmed optimization.
- `src/frame_timing.cpp`: frame cap and host timing. The Android build targets
  30 FPS and disables vsync by default. Its last-deadline spin is short; no
  profiling has established it as the main CPU cost.
- `third_party/rexglue-sdk/src/graphics/vulkan/pipeline_cache.cpp`: shader
  storage preloading and pipeline creation. The tested warm cache loaded in
  under half a second; avoid changing its thread count based on startup logs
  alone.
- `third_party/rexglue-sdk/src/graphics/vulkan/command_processor.cpp`: async
  placeholder presentation and occlusion-query waits. The Android incomplete
  frame hold is capped at 40 ms. Reducing the occlusion wait or allowing
  async occlusion-query pipelines can hide objects and change query results;
  neither is justified by the long driving profile.
- `src/shader_prepare.cpp`: optional shader-pack import. It is not required
  for gameplay. Keep pack and driver as separate test variables.
- `third_party/rexglue-sdk/src/ui/presenter.cpp`: optional `present_log` CSV
  instrumentation. An older, pre-current-build CSV exists locally, but its
  numbers cannot be used as a current presenter cost measurement.

## Instructions for the next AI

1. Inspect the current Git status, installed APK version and APK hashes before
   changing or testing anything. Treat the `0.1.6` APK as the working reference
   and `0.1.7` as an experiment. Preserve the compatible signing key and app
   data. Never commit game data, generated code, APKs, shader packs or keys.
2. Do not ask the user to repeat the long drive immediately; they said they
   had tested enough. Analyze the preserved raw profile and source first.
3. If a device test is welcome later, compare `mcla_fence_sleep_us = 0` and
   `50` on the experimental build, with the same driver, no shader pack, same
   saved scene and similar GPU thermal limit. Capture SurfaceFlinger
   present-to-present histograms, KGSL clock caps, per-thread CPU and native
   logs. Check visuals and input as well as FPS and p95/p99 stalls.
4. If sleep reduces CPU use but adds frame latency or artifacts, keep 0. If it
   helps, test the smallest effective value before proposing a new default.
   Do not mark a speedup from a single hot or cold run.
5. If the wait hook has little effect, profile CPU hot functions in a driving
   session (for example with Android `simpleperf`, if available) and correlate
   the result with thermal limits. A 90% CPU thread sample is a lead, not a
   function profile.
6. Only then compare shader pack on/off under matched conditions. Its possible
   startup or car-load benefit is distinct from sustained FPS. Avoid changing
   Vulkan pipeline defaults based solely on the 13 ms warning from one run.
7. Keep 720 lines as the default. Do not retry 644 as a broad FPS shortcut:
   it failed to load on this device and the game's own 3D targets stayed at
   1280×720 in the earlier investigation.

Verification for documentation/source changes: `git diff --check`,
`python3 -I scripts/ci/check_pins.py`, and
`python3 -I scripts/ci/check_markdown_links.py`, run from the repository root.
Build success, installation, road gameplay and a measured performance gain are
different levels of evidence; state which one was reached.
