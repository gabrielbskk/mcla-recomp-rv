# Android performance handoff — 2026-11-09

Read this after [AI handoff](ai-handoff.md) and [Android status](android-status.md).
This page records what the S23 runs proved, what they did not prove, and the
next experiments. The raw device logs and samples are private and ignored by
Git. Do not interpret compilation or an APK install as a gameplay fix.

## Tested configuration and user observations

### 0.1.9 candidate implementation (2026-11-11)

The user now requests an FPS overlay enabled by default plus periodic log
summaries. This supersedes the earlier no-overlay preference. Android options
`mcla_fps_overlay` and `mcla_fps_log` default to true. The overlay samples
successful host presentations every 500 ms, and the log reports the recent
two-second window every five seconds (FPS, p50, p95 and sample count).
The SDK retains at most 256 timestamps. These are host-present completion
intervals, not guest swaps, UI callback counts or SurfaceFlinger scanout;
repeated host presentations of the same guest image may be counted. No
continuous repaint is requested solely for the counter. The extra rendering
and instrumentation cost has not been measured.

The shader-pack investigation found incompatible packed Vulkan pipeline
records: the imported lists use 68 bytes, while this Android compiler's native
records use 66. All 407 native FBO records and all 474 pack-format FSI records
in the inspected files passed XXH3 validation at their respective strides.
Simply changing the pack parser's stride would invalidate its input; the
candidate instead exposes the native SDK record size and skips importing
incompatible pipeline lists after validating them. Native storage reading,
retirement and stamp counts use the native stride. Compatible shader import
continues and native pipelines can still be learned during gameplay. No cache
deletion is necessary for the valid native FBO file inspected in this session.
This avoids the repeated import/rejection cycle without claiming a portable
bitfield conversion. Full cross-compiler pipeline conversion remains future work.

Earlier conversational conclusions were too broad: the loaded driver Vulkan
cache is distinct from rejected shareable pipeline descriptions, and a failed
first record does not establish that every record is independently corrupt.
Neither invalid texture-fetch warnings nor YUV fallback messages identify the
headlight/wheel draws. No texture fix or gain from enabling invalid fetch
constants is established; that option stays unchanged. Likewise, absence of
artifacts on the system driver points to a driver/backend interaction, not
proof that the driver alone is at fault. Local driver builds are deferred.

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
fix and is kept as the fallback reference. Version numbers in the APK are now
correct; an earlier manifest
hardcoded `0.1.0` despite the packaging script's higher version output.

`0.1.7` is the **installed and tested APK** as of the 2026-11-09 night
session. It adds `mcla_fence_sleep_us`, default 0. A Turnip 26.3 run with 50
microseconds and a Turnip 27.0 run with 0 were captured, but the setting was
not compared under matched conditions, so neither value is a proven
optimization. The user approved the final configuration below and closed this
test phase. Keep the backed-up `0.1.6` APK as a fallback reference.

The approved configuration is `0.1.7`, Turnip Mesa 27.0.0-devel
(`git-79cb04951e`), the imported `545407F8.shaderpack`, 720 lines,
`mcla_fps = 60`, `vsync = false` and `mcla_fence_sleep_us = 0`. The user
reports that the pack makes the game "a bit hotter and more stable" rather
than clearly faster, and that the rear-light and wheel artifacts do not occur
with the system driver.

A Poco X7 Pro (`Mali-G720 MC7`) cannot run the current build: the driver lacks
`vertexPipelineStoresAndAtomics`, the graphics provider is not created, and
presentation fails with `C0000001`. This is a capability limit of the Vulkan
emulation on that driver, not a game-data problem.

## Long driving profile on `0.1.6`

### Live S23 Turnip run, 2026-11-09

The user asked for a live capture and drove in the city before closing the game.
The installed experimental build used Turnip Mesa 26.3.0-devel
(`git-84673c2731`), no shader pack, `mcla_fps = 60`, `vsync = true`,
`mcla_fence_sleep_us = 50`, and the user's existing reduced-detail settings
(including shadows, foliage shadows, motion blur and MSAA off). No APK or
game setting was changed for this capture.

SurfaceFlinger TimeStats was reset after the city loaded. In its next 111
seconds, the MCLA SurfaceView presented 1,782 frames at 15.929 average FPS;
the binned present-to-present p50 was about 58 ms and p95 about 118 ms.
There were 215 intervals over 100 ms and 21 of at least 200 ms. The separate
180-second GPU sample also included loading: the GPU maximum clock was capped
at 220 MHz in 133 samples and 348 MHz in 47 samples. A point-in-time `top -H`
sample showed SDLThread at about 84% CPU and two guest threads around 62% and
46%; these are snapshots, not per-frame attribution.

The native log loaded 0 bytes of `VkPipelineCache` data, then prepared 497
graphics pipelines from Vulkan storage and created 23 more later in the
captured portion. It recorded no access violation, fatal error, or logged
occlusion-query pipeline wait above the threshold. The cache was cold for this
driver and the GPU was capped for much of the run, so this capture cannot
measure the benefit of any Xendroid-Plus idea or establish which caused the
long pauses. Private raw data and the exact config snapshot are under
`out/profiles/2026-11-09-live-turnip/` and must not be committed.

The Xenia Canary MCLA Complete Edition patch also listed by Xendroid-Plus
offers foliage-shadow, MSAA and motion-blur switches. Equivalent configurable
hooks already exist in this port, and all three were off in this capture.
An A/B comparison of warmed runs on the same device, driver, scene and
settings is needed before changing more Vulkan code.

## Earlier driving profile on `0.1.6`

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

## Shader pack run on `0.1.7` with Turnip 27.0

The user imported `545407F8.shaderpack` through the launcher and drove during
a 150-second capture. The run used the approved configuration above. The
capture started at the launcher, so it includes loading and the pack's first
preparation work; it is not a matched A/B against the no-pack profiles.

The import rebuilt 320 of 320 shaders in 2,867 ms from 2,056 preset programs
and 38 vertex layouts, then added 320 shaders and 5,876 pipeline descriptions
to app-private shareable storage. The native log loaded 13,341,469 bytes of
`VkPipelineCache`; the analyzer counted 375 pipeline creations over the run,
with the busiest second creating 87 pipelines and loading 16 textures. There
was no access violation, fatal error, or logged occlusion-query wait of 10 ms
or more.

SurfaceFlinger's MCLA SurfaceView reported:

| Measure | Result |
| --- | ---: |
| Presented frames | 3,259 |
| Average presented FPS | 20.882 |
| Present-to-present p50 | about 42 ms |
| Present-to-present p95 | about 102 ms |
| Present-to-present p99 | about 134 ms |
| Intervals over 100 ms | 182 |
| Intervals at least 200 ms | 19 |

Of 150 one-second KGSL samples, the maximum permitted GPU clock was 220 MHz
in 70, 295 MHz in 16, 348 MHz in 50 and 615 MHz in 14; the median `gpu_load`
was 30. Comparing 20.882 FPS with the earlier 15.929 FPS Turnip run or the
14.223 FPS system-driver run is tempting but not valid as attribution: this
capture used a newer Turnip, `vsync = false`, a warm cache and a different
scene window, and it includes loading. The earlier runs used other
combinations. The pack also cannot raise a GPU-bound frame rate; its
measurable effect is removing runtime shader compilation.

The user's assessment is that the pack mainly adds heat and stability, not a
large speedup. Keep it optional and do not present it as a general FPS fix.
The GPU clock caps are an important constraint, but these runs do not establish
the relative CPU and GPU contribution to the stalls. If stutter returns, the next useful
step is function-level CPU profiling during an authorized drive
(`simpleperf`), not another pipeline-default change.

## Evidence and code entry points

### CPU capture and latest handoff (2026-11-09 evening)

The user authorized CPU profiling, drove, and then reported they had tested
as much as they could. Do not ask for another drive just to repeat this capture.
The successful retry used `simpleperf record -p 14899 -f 99
--call-graph dwarf,8192 --post-unwind=yes --cpu-percent 5 --duration 90`.
The resulting private file is `out/profiles/2026-11-09-simpleperf/perf.data`
(6,919,920 bytes); the original is `MCLA-simpleperf-retry.data` in Downloads.
An early report attempt while recording was still active said the file was
invalid. After recording and post-unwinding finished, `simpleperf report`
successfully read 33,449 samples. Earlier assistant statements that the retry
had failed were premature. Neither simpleperf nor the game was still running
when the completed file was inspected.

The report covers all sampled process threads, not just SDLThread:

| Thread | Share of sampled CPU cycles |
| --- | ---: |
| SDLThread | 28.52% |
| XThread9C3A7140 | 22.56% |
| XThread88F71140 | 12.19% |
| GPU Commands (F | 7.25% |
| Audio Worker (F | 2.11% |

`__kernel_clock_gettime` on SDLThread accounts for 15.76% of all sampled
cycles, or 55.23% when the report is filtered to SDLThread. The filtered report
contains 8,596 samples. Recovered stacks include
`SDLWindowedAppContext::RunMainMessageLoop`, `SDL_WaitEventTimeoutNS`,
`SDL_PumpEventsInternal`, `SDL_UpdateJoysticks` and `HIDAPI_UpdateDevices`.
These are CPU-cycle shares, not frame-time shares or a demonstrated FPS gain.
Some VDSO samples lack useful caller attribution; their cost must not all be
assigned to controller polling. Guest threads also have significant cost.

Capture limitations: recording started near launch, without a reliable marker
separating loading from driving. The attempted synchronized sensor collector
failed because its output directory was wrong. There is no valid temperature
and `thermal_pwrlevel` time series for this profile. SurfaceFlinger statistics
read later include other windows and are not a matched driving measurement.
Preserve the raw CPU profile and do not claim a controlled A/B result.

### Intended code work and current stopping point

The user explicitly requested a code change after the capture. The proposed
target was unnecessary repeated work in the Android SDL event/controller path.
Only source inspection happened before the user interrupted to request this
documentation: no runtime code change, new APK, installation or performance
fix resulted from that investigation.

In the following implementation session, a narrow Android SDL patch removed
the duplicate first event-maintenance pump when the entry pump already ran
and the queue is empty. A pending sentinel and later wakeups still pump.
`SDL3-shared` compiled and linked; no APK was generated or installed, and no
performance improvement has been measured. The approved 0.1.7 stays intact.

2026-11-10 follow-up: the SDL-only candidate was packaged as `0.1.8`
(versionCode 8) and installed with `pm install -r` using the same signing
certificate as `0.1.7`. SHA-256 of the candidate APK:
`d29eb05cbcc350ca0898d1c63f824fe82b4bd69611a148e58f88b6dfcc82a99f`.
The installed version and retained `mcla_fence_sleep_us = 0`, `mcla_fps = 60`,
`vsync = false`, selected Turnip driver and imported pack were verified.
No gameplay, controller, lifecycle or frame-time test of `0.1.8` has occurred.
`0.1.7` remains the last user-approved gameplay build and its APK is preserved
for rollback; do not present `0.1.8` as a performance improvement.

Continue by reading the complete Android branch of `SDL_WaitEventTimeoutNS`
and its Android wait implementation, then tracing the runtime's wakeup events
and controller-update scheduling. The runtime currently calls `SDL_WaitEvent`
in its main message loop. Event maintenance calls `SDL_UpdateJoysticks`, which
calls HIDAPI device updates. Merely appearing in the stack does not prove
excessive polling. Do not infer Android behavior from the non-Android polling
fallback in the same SDL source file.

Use the saved call graphs to identify a concrete redundant wakeup or busy-wait
before choosing the edit. Implement one isolated, reversible change in that
path if confirmed, preserving gamepad input, vibration, hotplug and Android
lifecycle handling. No polling interval has been selected. Do not disable
HIDAPI or add an arbitrary sleep solely from the current sample percentages.
Record SDK/SDL changes reproducibly in the repository's patch/build recipe;
`third_party` points to a shared SDK checkout, so a local edit alone is not a
reproducible delivery. Keep the approved 0.1.7 APK and signing key available.
Any eventual device comparison must use the same driver, pack, scene and
settings and collect thermal data alongside CPU and frame timings.

### Additional external-device evidence

Downloads now also contains a Mali-G52 MC2 user's log. It fails on the same
missing `vertexPipelineStoresAndAtomics` capability as all three Poco X7 Pro
Mali-G720 MC7 logs, before graphics-provider creation. These logs establish a
driver/backend capability rejection, not corrupt game data or measured
performance. The exact G52 phone model and installed APK identity are unverified.
PanVK alternatives were researched, but no driver was installed or integrated
for these users. The user explicitly deferred Mali driver work; it is not the
current objective.

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
   changing or testing anything. `0.1.7` is the approved build; keep `0.1.6`
   as the fallback. Preserve the compatible signing key and app data. Never
   commit game data, generated code, APKs, shader packs or keys.
2. Do not ask the user to repeat the long drive immediately; they said they
   had tested enough. Analyze the preserved raw profile and source first.
3. If a device test is welcome later, compare `mcla_fence_sleep_us = 0` and
   `50` with the same driver, pack state, `vsync` value, saved scene and
   similar GPU thermal limit. The existing runs are not matched. Capture
   SurfaceFlinger present-to-present histograms, KGSL clock caps, per-thread
   CPU and native logs. Check visuals and input as well as FPS and p95/p99
   stalls.
4. If sleep reduces CPU use but adds frame latency or artifacts, keep 0. If it
   helps, test the smallest effective value before proposing a new default.
   Do not mark a speedup from a single hot or cold run.
5. If the wait hook has little effect, profile CPU hot functions in a driving
   session (for example with Android `simpleperf`, if available) and correlate
   the result with thermal limits. A 90% CPU thread sample is a lead, not a
   function profile.
6. Only then compare shader pack on/off under matched conditions. Its possible
   startup or car-load benefit is distinct from sustained FPS. The existing
   pack run is not a clean A/B. Avoid changing Vulkan pipeline defaults based
   solely on the 13 ms warning from one run.
7. Keep 720 lines as the default. Do not retry 644 as a broad FPS shortcut:
   it failed to load on this device and the game's own 3D targets stayed at
   1280×720 in the earlier investigation.

Verification for documentation/source changes: `git diff --check`,
`python3 -I scripts/ci/check_pins.py`, and
`python3 -I scripts/ci/check_markdown_links.py`, run from the repository root.
Build success, installation, road gameplay and a measured performance gain are
different levels of evidence; state which one was reached.
