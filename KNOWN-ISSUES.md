# Android known issues

- Turnip OneUI and Standard v27.0.0-20261107 produced intermittent white rear
  lights on the tested Galaxy S23; system Vulkan is the recommended driver.
- Sustained FPS is low and stutters remain. Thermal throttling prevented a
  controlled performance conclusion.
- Clean-profile folder import, final log filenames, per-driver cache behavior,
  two-gamepad/two-player input, long sessions and 16 KiB page-device runtime
  remain unverified.
- The launcher checks file presence and nonzero size, not hashes. A damaged
  extracted file may pass import.

See [android-status.md](docs/android-status.md) for evidence and verification
boundaries. The inherited Windows/PS5 issue catalogue is archived in
[`docs/fork-base/KNOWN-ISSUES.md`](docs/fork-base/KNOWN-ISSUES.md).
