<p align="center"><img src="docs/assets/mcla-rv-banner.svg" alt="MCLA Recomp RV — Android ARM64 experimental port" width="100%"></p>

# MCLA Recomp RV

An experimental native Android ARM64 port of *Midnight Club: Los Angeles Complete Edition* for Xbox 360. The project uses C++23, SDL3 and Vulkan through the ReXGlue runtime; it is not an x86 emulator or a Linux compatibility layer.

## Status

The port reaches menus, audio and 3D gameplay on a Samsung Galaxy S23 (Adreno 740, 8 GB RAM, 256 GB storage). On this device the system Vulkan driver avoids the tested Turnip rendering artifacts, but is slower. The tested Turnip 27 build is playable without the rear-light and wheel flashes when `TU_DEBUG=syncdraw` is set before launch; without that workaround, the artifacts remain. Sustained FPS is limited and stutters remain, especially as the phone heats up. See the [Turnip test matrix](docs/turnip-a740-rendering-artifacts.md), [Android status](docs/android-status.md) and [known issues](KNOWN-ISSUES.md).

Android release APKs are debug-signed and include a native library generated from the game's executable. Unlike release APK assets, the source repository never contains game data, generated game sources, saves, keys, driver packages or shader packs. This build has only been tested on a limited set of devices; it is not a general Android compatibility guarantee.

## Build

The reproducible source recipe is in [docs/android-build.md](docs/android-build.md). It pins ReXGlue SDK commit `f5337cdc947ff6d4c4196737e2c807a48f2a1fc2` and applies the Android patches in `patches/`. The Android CI builds the patched SDK and performs a host syntax check without game files. This repository does not provide the game data.

## Install and import data

Provide your own legally obtained Xbox 360 game data, extract the 13 required files, and import the extracted folder through the Android document picker. The detailed steps and optional Turnip-driver guidance are in [docs/android-testing.md](docs/android-testing.md). No broad storage permission is required for the picker. Logs are exported to `Documents/MCLA/logs/` when Android supports MediaStore.

## Limitations

- Only ARM64 Android is supported; 16 KiB page-device runtime and clean-profile folder import remain unverified.
- The system Vulkan driver avoids the observed Turnip artifacts; Turnip 27 needs the diagnostic `TU_DEBUG=syncdraw` workaround on the tested device.
- Gamepad is the only officially supported input method at the moment. Keyboard
  and mouse may work through Android/SDL but are not verified; touch input is
  planned for a future update.
- The launcher checks file presence and nonzero size, not content hashes.
- No game image, game assets, saves, signing keys, generated game code or third-party driver is distributed by this source repository.

## Credits and license

This work builds on [holdmysocks/mcla-recomp](https://github.com/holdmysocks/mcla-recomp), the direct [RenanQueiroz fork](https://github.com/RenanQueiroz/mcla-recomp), ReXGlue, SDL3 and AdrenoTools. [CREDITS.md](CREDITS.md) records provenance and the AI assistance used during development; [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md) contains required notices. The project is licensed under [GPL-3.0-or-later](LICENSE) and is not affiliated with the game rights holders.

The original game and its data remain the property of their respective owners. Do not upload game files, generated code or private artifacts to this repository.
