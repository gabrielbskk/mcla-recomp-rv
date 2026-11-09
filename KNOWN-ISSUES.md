# Android known issues

- Turnip OneUI and Standard v27.0.0-20261107, and Turnip Mesa 27.0.0-devel
  (git-79cb04951e), produced intermittent white rear lights on the tested
  Galaxy S23; system Vulkan is the recommended driver. Tracked in
  [issue 2](https://github.com/gabrielbskk/mcla-recomp-rv/issues/2).
- Sustained FPS is low and stutters remain. The first seconds after the city
  loads are the worst: the Android build ships without the precompiled shader
  pack, so pipelines are translated on demand. Thermal throttling (GPU held at
  475-550 MHz of 719 MHz) also limits the result. Tracked in
  [issue 1](https://github.com/gabrielbskk/mcla-recomp-rv/issues/1).
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
