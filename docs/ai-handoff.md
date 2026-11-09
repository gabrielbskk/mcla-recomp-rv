# AI handoff

This file is the public, safe entry point for an AI continuing the Android
port. Read it with `README.md`, `docs/android-status.md`,
`docs/android-performance-handoff.md`, `docs/android-build.md` and
`docs/android-testing.md`.

The private S23 and netbook handoffs stay outside Git because they contain
local paths, game data, signing keys and infrastructure details. On the
maintainer's Termux installation, read the latest
`~/MCLA-2026-11-09-SESSION-HANDOFF.md`, then `~/MCLA-TERMUX-S23-HANDOFF.md`,
`~/MCLA-PROJECT-LEDGER.md` and `~/MCLA-NETBOOK-OPERATIONS.md`. Older private
archive copies may exist; verify their dates and contents before relying on
them for recovery. The latest local session handoff is not assumed to be
synced elsewhere.

## Verified baseline

The target is Android ARM64 on a Samsung Galaxy S23 with an Adreno 740. Inspect
Git for the current commit instead of relying on a frozen hash here. The
measured working reference is the `0.1.6` APK: 720-line default, system
Vulkan driver, no shader pack in the long driving profile, and landscape
rotation confirmed by the user. The `0.1.7` APK was installed on 2026-11-09
but has no gameplay or performance result yet. It has an optional GPU fence
sleep, off by default in source; the private installed configuration was later
set to 50 microseconds for a future test. The earlier pre-FPS `0.1.2`
APK remains backed up; the FPS overlay was reverted after it appeared to
reduce performance. One generic wireless gamepad works. Two-player/two-gamepad
support is not verified. Touch is planned.

The APK does not contain game data. Use the user's legally obtained files and
the Android folder picker described in `docs/android-testing.md`. Never add
game assets, generated game code, APKs, shader packs or signing keys to Git.

## Current technical limits

Turnip driver variants have shown vehicle rear-light flashing in some tests.
The system driver was the preferred visual baseline. A 150-second driving
profile on `0.1.6` measured 14.223 average presented FPS and a 110 ms p95
frame interval while the GPU was usually capped at 220–295 MHz. See
`docs/android-performance-handoff.md` before changing pipeline behavior.
Frame generation is not enabled. A separate Android ARM64 FidelityFX Vulkan
runtime build succeeded,
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
