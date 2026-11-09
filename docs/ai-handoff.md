# AI handoff

This file is the public, safe entry point for an AI continuing the Android
port. Read it with `README.md`, `docs/android-status.md`,
`docs/android-build.md` and `docs/android-testing.md`.

The private S23 and netbook handoffs stay outside Git because they contain
local paths, game data, signing keys and infrastructure details. On the
maintainer's Termux installation, read `~/MCLA-TERMUX-S23-HANDOFF.md`,
`~/MCLA-PROJECT-LEDGER.md` and `~/MCLA-NETBOOK-OPERATIONS.md`. The private
netbook copy keeps the same documents under the dated backup root.

## Verified baseline

The target is Android ARM64 on a Samsung Galaxy S23 with an Adreno 740. The
public repository is at commit `36010d0`. The current baseline is the pre-FPS
APK `0.1.2`/versionCode `2`; the FPS overlay was reverted after it appeared to
reduce performance. One generic wireless gamepad works. Two-player/two-gamepad
support is not verified. Touch is planned.

The APK does not contain game data. Use the user's legally obtained files and
the Android folder picker described in `docs/android-testing.md`. Never add
game assets, generated game code, APKs, shader packs or signing keys to Git.

## Current technical limits

Turnip driver variants have shown vehicle rear-light flashing in some tests.
The system driver was the preferred visual baseline. Native performance is
low, around 16–17 FPS in the captured driving scene. Frame generation is not
enabled. A separate Android ARM64 FidelityFX Vulkan runtime build succeeded,
but no frame-generation APK or device test exists; a Windows DLL is not a
drop-in Android library.

## Working rules

- Preserve the compatible signing key when creating an update APK.
- Back up every gameplay-approved APK outside Git and record its SHA-256.
- Test changes on the S23 before claiming a fix.
- Keep public documentation in English and avoid local machine paths.
- Run `git diff --check`, `python3 -I scripts/ci/check_pins.py` and
  `python3 -I scripts/ci/check_markdown_links.py` before a source commit.
- Ask for or locate the private netbook backup before rebuilding missing game
  data or generated outputs.
