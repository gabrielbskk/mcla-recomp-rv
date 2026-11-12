# Android known issues

- Audio on some devices is silent until another app opens the audio mixer
  (for example while screen recording). The current Android build defaults to
  the normal mixer path and requests audio focus; `mcla_audio_low_latency`
  restores the old low-latency stream. This has no measured latency result
  yet.

- Only one gamepad is validated for in-game controls. Keyboard and mouse do
  not work as in-game controls on Android. v0.1.24 includes an experimental
  Xbox-style touch overlay, but it has not been validated in a driving
  session; treat it as unsupported until it is tested.

- Turnip OneUI and Standard v27.0.0-20261107, and Turnip Mesa 27.0.0-devel
  (git-79cb04951e), produced intermittent white rear lights on the tested
  Galaxy S23. On the latter build, `TU_DEBUG=syncdraw` removed the observed
  rear-light and wheel artifacts, but does not fix the underlying issue and
  may cost performance. System Vulkan also avoids the observed artifacts, at
  lower performance in the tested runs. See the
  [test matrix](docs/turnip-a740-rendering-artifacts.md). Tracked in
  [issue 2](https://github.com/gabrielbskk/mcla-recomp-rv/issues/2).
- Sustained FPS is low and stutters remain. The first seconds after the city
  loads are the worst: the Android build ships without the precompiled shader
  pack, so pipelines are translated on demand. The launcher can now import a
  user-provided `545407F8.shaderpack` into `files/shaderpack/`, but that path
  is not yet measured on the device. Thermal throttling (GPU held at 475-550 MHz
  of 719 MHz in the Turnip run, 295-348 MHz in the later system driver run)
  also limits the result. Tracked in
  [issue 1](https://github.com/gabrielbskk/mcla-recomp-rv/issues/1). Two
  mitigations are in the source but not yet measured on the device: the hold of
  the last picture while a pipeline is created is capped at 40 ms (about one
  30 FPS frame) instead of 100 ms, pipeline compilation and driver
  pipeline-cache writes run below the game and render threads on Android, and
  the driver pipeline-cache save waits for a break in pipeline creation
  instead of writing tens of megabytes during a burst. The experimental
  644-line guest video mode caused repeated guest access violations during
  loading on the tested Galaxy S23. Forcing 720 lines allowed gameplay to
  start; the Android default is restored to 720 in source, pending a rebuild.
- A draw inside an occlusion query still waits for the pipeline it needs: one
  created right away can take up to 400 ms in the driver, and one queued by an
  earlier draw is waited for up to 500 ms. The source now logs both times and
  bounds them with `vulkan_occlusion_query_pipeline_wait_ms` and
  `vulkan_occlusion_query_async_pipelines`; the defaults are unchanged and
  still need a device measurement.
- The system driver removes the rear-light artifact but keeps a pipeline cache
  per driver: the first run after a switch started with 0 bytes of cache and
  created 782 pipelines in 13.6 s; the next run loaded a 15 MB cache and
  created 808 pipelines in 0.5 s. In the same session the system driver
  measured 13.4 FPS in gameplay, but the GPU was thermally limited to 348 MHz,
  so this is not a clean comparison against Turnip's 22-24 FPS.
- The aspect ratio can be taken from a transient portrait surface at startup
  (1080x2340), choosing a 1280x720 guest image that is pillarboxed on the
  19.5:9 display; another run detects 2340x1080 and chooses 1560x720. The
  source now takes the landscape orientation from that transient surface and
  derives the guest video mode width from the aspect ratio; not yet measured.
  Tracked in
  [issue 4](https://github.com/gabrielbskk/mcla-recomp-rv/issues/4).
- Wheels flicker in motion on Turnip without `syncdraw`; the workaround
  removed the flicker in the tested sessions. Not reproduced on system Vulkan.
  Tracked in [issue 3](https://github.com/gabrielbskk/mcla-recomp-rv/issues/3).
- Clean-profile folder import, final log filenames, per-driver cache behavior,
  two-gamepad/two-player input, long sessions and 16 KiB page-device runtime
  remain unverified.
- The launcher checks file presence and nonzero size, not hashes. A damaged
  extracted file may pass import.

See [android-status.md](docs/android-status.md) for evidence and verification
boundaries. The inherited Windows/PS5 issue catalogue is archived in
[`docs/fork-base/KNOWN-ISSUES.md`](docs/fork-base/KNOWN-ISSUES.md).
