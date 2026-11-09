# Android known issues

- Turnip OneUI and Standard v27.0.0-20261107, and Turnip Mesa 27.0.0-devel
  (git-79cb04951e), produced intermittent white rear lights on the tested
  Galaxy S23; system Vulkan is the recommended driver. Tracked in
  [issue 2](https://github.com/gabrielbskk/mcla-recomp-rv/issues/2).
- Sustained FPS is low and stutters remain. The first seconds after the city
  loads are the worst: the Android build ships without the precompiled shader
  pack, so pipelines are translated on demand. Thermal throttling (GPU held at
  475-550 MHz of 719 MHz in the Turnip run, 295-348 MHz in the later system
  driver run) also limits the result. Tracked in
  [issue 1](https://github.com/gabrielbskk/mcla-recomp-rv/issues/1). Two
  mitigations are in the source but not yet measured on the device: the hold of
  the last picture while a pipeline is created is capped at 40 ms (about one
  30 FPS frame) instead of 100 ms, and pipeline compilation and driver
  pipeline-cache writes run below the game and render threads on Android. The
  driver pipeline-cache save also waits for a break in pipeline creation
  instead of writing tens of megabytes during a burst.
- The system driver removes the rear-light artifact but keeps a pipeline cache
  per driver: the first run after a switch started with 0 bytes of cache and
  created 782 pipelines in 13.6 s; the next run loaded a 15 MB cache and
  created 808 pipelines in 0.5 s. In the same session the system driver
  measured 13.4 FPS in gameplay, but the GPU was thermally limited to 348 MHz,
  so this is not a clean comparison against Turnip's 22-24 FPS.
- The aspect ratio can be taken from a transient portrait surface at startup
  (1080x2340), choosing a 1280x720 guest image that is pillarboxed on the
  19.5:9 display; another run detects 2340x1080 and chooses 1560x720. Tracked
  in [issue 4](https://github.com/gabrielbskk/mcla-recomp-rv/issues/4).
- Wheels flicker in motion on Turnip; not yet reproduced on the system driver.
  Tracked in [issue 3](https://github.com/gabrielbskk/mcla-recomp-rv/issues/3).
- Clean-profile folder import, final log filenames, per-driver cache behavior,
  two-gamepad/two-player input, long sessions and 16 KiB page-device runtime
  remain unverified.
- The launcher checks file presence and nonzero size, not hashes. A damaged
  extracted file may pass import.

See [android-status.md](docs/android-status.md) for evidence and verification
boundaries. The inherited Windows/PS5 issue catalogue is archived in
[`docs/fork-base/KNOWN-ISSUES.md`](docs/fork-base/KNOWN-ISSUES.md).
