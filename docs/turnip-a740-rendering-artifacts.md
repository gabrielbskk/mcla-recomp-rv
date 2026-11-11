# Turnip A740: intermittent rear-light and wheel artifacts in MCLA recomp

## Environment and symptoms

- Samsung Galaxy S23, Adreno 740; selected Turnip 27 driver reports
  `Mesa 27.0.0-devel (git-79cb04951e)` in the game log.
- MCLA Android recomp APK 0.1.12–0.1.15, Vulkan backend, warm native shader
  cache, no precompiled shader pack. Anti-aliasing is off and the requested
  frame-rate cap is 60 FPS. The game files are user-supplied, not part of this
  repository or this report.
- In the same driving scene, rear lights intermittently flash white or change
  color and wheels flicker. These observations require on-device visual
  confirmation: the host-presentation log alone cannot detect the artifacts.

## Reproduction and comparisons

1. Select the Turnip driver on the tested device, disable the shader pack,
   retain the native shader/pipeline caches and use the same driving scene.
2. With no `files/driver.env`, launch and observe the rear lights and wheels.
3. Stop the app, put `TU_DEBUG=syncdraw` in the app-private `files/driver.env`,
   then relaunch without changing other settings. Both artifacts disappear in
   the tested session. `driver.env` is an app-side diagnostic switch that sets
   the environment variable before Vulkan initialization, not a Mesa config
   file. Remove it and restart to return to the control.
4. For the recomp-only experiments, leave `driver.env` absent, enable just the
   named Vulkan option in `files/mcla.toml`, and restart between runs. The
   experimental options are disabled by default in the published patch.

The FPS figures below are medians of logged `host presentation (recent 2s)`
samples over each entire session, **not** display scanout or controlled
same-temperature driving-only benchmarks. Loading and scene transitions may
affect the medians. Visual results are the user's on-device observations.

| Session | Single diagnostic condition relative to the tested defaults | Median host FPS | Visual result |
| --- | --- | ---: | --- |
| 033 | Baseline, no driver debug switch | 29.9 | Artifacts present |
| 039 | `TU_DEBUG=noubwc` | 27.6 | Artifacts present |
| 040 | `TU_DEBUG=syncdraw`, threaded submission on | 28.8 | Both artifacts absent |
| 042 | `TU_DEBUG=nolrz` | 33.2 | Artifacts present |
| 043 | Wait at frame start, zero older submissions in flight | 29.5 | Artifacts present |
| 044 | Threaded submission off, no `syncdraw` | 21.8 | Artifacts present |
| 047 | Threaded submission off plus `syncdraw` | 21.1 | Both artifacts absent |
| 048 | Wait before each submission; primary-buffer-end submission on | 15.3 | Both artifacts absent |
| 049 | As 048, but primary-buffer-end submission off | 18.6 | Artifacts present |
| 051 | Wait only after eligible primary-buffer-end submissions | 22.5 | Artifacts present |

Session 043 waited about 61 times per two seconds (862 ms total) without an
FPS loss or a visual fix. Session 048 waited about 105 times per two seconds
(1.4 s total) and removed the artifacts at substantial performance cost.
Session 051 waited about 40 times per two seconds (1.39 s total) but did not
remove the artifacts. The `vulkan_threaded_submission_split_draws = 1` test
stalled loading while repeatedly presenting the same image: its apparent very
high FPS is invalid. Async shader compilation off, other AA/MSAA settings,
`depth_float24_convert_in_pixel_shader`, and `fsi` also failed to remove the
artifacts in earlier tests. The tested system Vulkan driver did not show the
artifacts, at lower performance.

## Interpretation and next diagnostic step

The evidence is **consistent with** an ordering or visibility problem in the
Turnip A740 path involving work within a frame: waiting once at frame start
does not fix it, while `syncdraw` does. The submission-boundary results make
command-buffer dependency handling a useful place to investigate, but do not
prove a specific missing Vulkan barrier or a Mesa defect: the recomp's
recording and submission behavior may also be involved. In particular, the
selective primary-buffer-end wait failing means a simple wait at those
boundaries is not a working fix. No occlusion-query behavior was changed for
these experiments.

A useful driver-side follow-up is a minimal Vulkan capture or trace of the
affected draws and their read/write dependencies, checked against Vulkan
synchronization validation and replayed on both the affected Turnip build and
the system driver. This report does not distribute game data or a capture.
The currently usable on-device workaround is `TU_DEBUG=syncdraw` with threaded
submission on. Recomp-only fence waits remain opt-in diagnostics, not a
performance-safe fix.

## Pending repeatability check

Two additional runs of the same driving section with the workaround active
are pending. Record their log session IDs, median driving-only FPS, temperature
or thermal state, and separate visual observations for lights and wheels.
These are repeat runs, not an A/B comparison; a same-day A/B would additionally
need a control run with `driver.env` absent, changing no other setting. Do not
treat the historical session medians as those new results.
