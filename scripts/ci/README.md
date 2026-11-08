# Android CI helpers

These scripts support the repository checks and the Android ARM64 workflow. No
script here needs the user's game files, an APK, a signing key, or a connected
phone.

| Script | Purpose |
| --- | --- |
| `check_pins.py` | Validates the pinned SDK revision, Android patch order and hashes, workflow permissions, tracked files, and Python syntax. |
| `host_stub.py` | Creates compile-only stand-ins for headers normally generated from the user's game. |
| `host_check.py` | Runs the C++ commands from `compile_commands.json` with `-fsyntax-only`. |
| `host_compile_android.sh` | Configures the host against an installed Android SDK and runs the stub-based syntax check. |

The Windows launcher and release helpers inherited from the base fork are kept
under [`archive/fork-base/ci/`](../../archive/fork-base/ci/) for reference and
are not part of this Android workflow.
