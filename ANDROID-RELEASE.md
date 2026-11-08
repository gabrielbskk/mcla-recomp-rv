# Android ARM64 development APK

This release contains the **same APK** delivered locally on 2026-11-08, not a newer rebuild. It is an experimental, debug-signed build for ARM64 Android. The game data is **not** in the APK. Users must provide and import the 13 extracted files from their own copy of *Midnight Club: Los Angeles Complete Edition* for Xbox 360 (Title ID `545407F8`). See [Android testing and import](docs/android-testing.md).

Expected asset name: `MCLA-Recomp-arm64.apk` (about 105 MiB). SHA-256: `94b5fc1ac47556efa790b858099ed96226fff9f15c5826de44133854a519de3e`.

The package contains `libmain.so`, which is generated from the game's executable. Unlike the source repository, the APK therefore contains generated game code. The APK can be downloaded, extracted, copied and redistributed by anyone with access to a public release; its checksum and signature identify this exact build but do not prevent copying or impersonation elsewhere. It contains no ISO, extracted game assets, save files, Turnip driver ZIP, personal credentials, or private debug signing key.

The last installed candidates reached gameplay on a realme RMX5070 / Adreno 740. The **exact release APK was not installed or played** after the final per-driver Vulkan cache and log-export name changes. It passed build, ELF64 AArch64 and 16 KiB LOAD-alignment checks, ZIP alignment and APK signature checks. A device with 16 KiB memory pages has not been tested.

Known limits:

- Use the system Vulkan driver on the tested phone. Turnip OneUI and Standard v27.0.0-20261107 both caused rear lights to flash white. The system driver removed the flash in the user's test, with somewhat lower/uneven performance.
- Sustained FPS remains low and stutters occur; GPU thermal/power limits were observed during tests. A controlled performance comparison has not been completed.
- Empty-profile game-folder import, final `.txt` log naming, the new per-driver cache, gamepads, long-session stability and 16 KiB page runtime remain unverified.
- The launcher checks that the 13 game files exist and are nonempty, but does not verify their hashes. A damaged file may pass import. An imported Turnip ZIP contains native code; use only a trusted source.

Source provenance and AI assistance are explained in [CREDITS.md](CREDITS.md) and [README.md](README.md). Technical evidence and the exact build steps are in [docs/android-handoff.md](docs/android-handoff.md) and [docs/android-build.md](docs/android-build.md).
