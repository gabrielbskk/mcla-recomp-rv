# Android ARM64 build handoff

For current evidence, open issues and verification boundaries, read [android-status.md](android-status.md) first. The SDK source choices and combined patch limitation are in [android-sdk-source.md](android-sdk-source.md). Record future candidates in [android-build-journal.md](android-build-journal.md). This page is a source recipe; it does not generate a build during documentation review.

This port is written in **C++23** (plus Java for the Android launcher). The APK contains only ARM64 native libraries and Java launcher code. It does not include the game's files, Turnip binaries, or the local signing key. The current device build is experimental but has reached gameplay on a real Adreno 740 phone.

## Current working tree

Build on Termux with `clang`, CMake, Ninja, Python 3, Java, `aapt2`, `apksigner`, `zipalign`, `readelf`, and enough free space for generated code. The SDK and AdrenoTools checkouts under `third_party/` are ignored by Git. The current SDK was built from `f5337cdc947ff6d4c4196737e2c807a48f2a1fc2` (ReXGlue v0.10.0); AdrenoTools is pinned by `scripts/build_adrenotools_android.sh`.

From the repository root, after the SDK patch or C++ code changes:

```sh
cmake --build third_party/rexglue-sdk/out/build/android-arm64 -j 4
cmake --install third_party/rexglue-sdk/out/build/android-arm64
scripts/build_adrenotools_android.sh
third_party/rexglue-sdk/out/install/android-arm64/bin/rexglue codegen mcla_manifest.toml
cmake --build out/build/mcla-android-arm64 --target mcla -j 4
scripts/build_android_apk.sh
```

`scripts/build_android_apk.sh` checks every native library for ELF64 AArch64 and 16 KiB `LOAD` alignment, aligns and signs the APK, verifies its signature, and copies it to `Download/MCLA-Recomp-arm64.apk` on this Termux device. It also keeps `out/android-arm64/MCLA-arm64.apk`. `MCLA_DOWNLOADS_DIR` overrides the export folder. Installing with `pm install -r` preserves the private game files, saves, driver selection, and cache.

The packager takes `libmain.so` from `out/build/mcla-android-arm64/` and `librexgpu-xenos.so`, `librexruntime.so`, and `libSDL3.so` from the installed SDK. Reinstall the SDK after changing its source so the APK cannot silently use an older GPU plugin copied into the main build directory.

## Signing and versioning

The packaging script keeps the debug signing key outside the repository by default at `$PREFIX/share/mcla-recomp/android-debug.keystore` in Termux. Set `MCLA_KEYSTORE_PATH` to choose another fixed path. For compatibility with the first local APK, an existing `out/android-arm64/debug.keystore` is used when no external key exists and `MCLA_KEYSTORE_PATH` is unset; future builds should migrate to the external path. Set `MCLA_KEY_ALIAS`, `MCLA_KEYSTORE_PASSWORD`, and `MCLA_KEY_PASSWORD` when using a non-default key.

Without `MCLA_VERSION_CODE`, the script increments a counter outside the repository at `$PREFIX/share/mcla-recomp/version-code`. The default version name is `0.1.<versionCode>`. `MCLA_VERSION_CODE`, `MCLA_VERSION_NAME`, and `MCLA_VERSION_STATE_FILE` override this behavior for a reproducible or release build. The counter is written only after packaging, signature verification and ZIP alignment succeed.

Keep approved builds outside the repository: after testing an APK in game, copy it to `Download/MCLA-build-backups/` using a filename that includes the commit and version, then record its SHA-256 hash. The packager's `Download/MCLA-Recomp-arm64.apk` is replaced by each build; the backup folder is not. The rollback build `MCLA-arm64-pre-fps-925ae4b-v0.1.2.apk` is saved there as the pre-counter baseline. It was installed successfully, but its gameplay behavior still needs confirmation after the rebuild. Preserve the signing keystore too: an APK signed with a different key cannot update the installed package without removing its app data.

## Recreate the native build directories

Use the exact SDK commit above. Clone the SDK and its submodules if the checkout does not exist. Apply these patches in order to a clean SDK checkout:

```sh
git clone --recursive https://github.com/rexglue/rexglue-sdk.git third_party/rexglue-sdk
git -C third_party/rexglue-sdk checkout f5337cdc947ff6d4c4196737e2c807a48f2a1fc2
git -C third_party/rexglue-sdk submodule update --init --recursive
PATCHES="$PWD/patches"
git -C third_party/rexglue-sdk apply "$PATCHES/rexglue-android-arm64.patch"
git -C third_party/rexglue-sdk apply "$PATCHES/rexglue-turnip-loader.patch"
git -C third_party/rexglue-sdk apply "$PATCHES/rexglue-driver-cache-path.patch"
git -C third_party/rexglue-sdk apply "$PATCHES/rexglue-vulkan-pipeline-background-work.patch"
git -C third_party/rexglue-sdk apply "$PATCHES/rexglue-vulkan-occlusion-query-pipeline-wait.patch"
git -C third_party/rexglue-sdk apply "$PATCHES/rexglue-android-present-filters.patch"
```

If the SDK checkout already exists, start with a clean copy at that commit instead of cloning over it. Do not apply a patch twice. The first patch contains the fork's SDK changes as well as Android ARM64 support; the later ones add optional Turnip loading, per-driver Vulkan cache files, lower-priority pipeline compilation and cache writing on Android, the timing and the cvar bounds of the occlusion-query pipeline wait, and the presenter's scaling filters on Android.

Configure and build the SDK:

```sh
SDK="$PWD/third_party/rexglue-sdk"
cmake -S "$SDK" -B "$SDK/out/build/android-arm64" -G Ninja \
  -DANDROID=ON -DCMAKE_BUILD_TYPE=Release \
  -DREXGLUE_USE_VULKAN=ON -DREXGLUE_USE_D3D12=OFF \
  -DREXGLUE_ENABLE_FSR=OFF \
  -DCMAKE_INSTALL_PREFIX="$SDK/out/install/android-arm64"
cmake --build "$SDK/out/build/android-arm64" -j 4
cmake --install "$SDK/out/build/android-arm64"
scripts/build_adrenotools_android.sh
```

Extract the user's own Xbox 360 game image with `python3 scripts/extract_game.py /path/to/disc.iso`; this creates 13 files in `game/`. Keep the image and extracted data outside Git. Then:

```sh
"$SDK/out/install/android-arm64/bin/rexglue" codegen mcla_manifest.toml
cmake -S . -B out/build/mcla-android-arm64 -G Ninja \
  -DANDROID=TRUE -DCMAKE_BUILD_TYPE=Release \
  -DCMAKE_PREFIX_PATH="$SDK/out/install/android-arm64"
cmake --build out/build/mcla-android-arm64 --target mcla -j 4
scripts/build_android_apk.sh
```

The first C++ build can take a long time because codegen produces many large translation units. Ninja resumes completed objects after an interruption. Avoid compiling on the same phone during a gameplay performance comparison: it consumes CPU and raises the temperature.

## Runtime and known limits

- The launcher text is English. **SELECT TURNIP (.ZIP)** imports an AdrenoTools-compatible ARM64 driver package into app-private storage; **USE SYSTEM DRIVER** returns to Android Vulkan; **PLAY** starts the game. The system driver is the safe default. The Turnip 8G2-OneUI and Standard v27.0.0-20261107 packages both produced white rear-light flashes on the Samsung Galaxy S23 used for testing, while the system driver did not. Older runtime logs report `realme/RMX5070` because device spoofing was enabled. See [android-history.md](android-history.md) for the historical findings.
- The launcher imports the 13 extracted game files through Android's folder picker into `files/game/`. The exact user steps are in `docs/android-testing.md`. The full clean-install import has not been verified end to end on a separate empty app profile.
- Logs remain privately in `files/logs/*.log` and are exported to `Documents/MCLA/logs/*.txt` through MediaStore on Android 10+. The export runs on launcher open, during gameplay, and when the game pauses or closes. The Vulkan pipeline cache is under `files/mcla/cache/shaders/local/` and now has a separate file for each driver identity.
- The APK's libraries are built for 16 KiB page alignment, but runtime behavior on an actual 16 KiB page device has not been tested. The current test phone uses 4 KiB pages.
