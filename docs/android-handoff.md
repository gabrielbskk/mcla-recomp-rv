# Android ARM64 port: complete handoff (2026-11-08)

This is the current entry point for continuing the Android work. `ANDROID-PORT-NOTES.md` contains the chronological investigation, measurements, and earlier hypotheses; `docs/android-build.md` has the reproducible commands; `docs/android-testing.md` explains installation and game-data import; `docs/android-build-journal.md` records each build candidate. Earlier notes describe the state *at that time*. This document records what is true at the final build and distinguishes code that compiled from behavior observed on the phone.

## Scope and current state

- Goal: compile the Xbox 360 PowerPC code into host C++23 and run it as Android ARM64 native code, using SDL3 and Vulkan. The ReXGlue runtime still translates console services and graphics calls; this is not an emulator running an x86/Linux executable.
- Device tested: realme RMX5070, Snapdragon 8 Gen 2 / Adreno 740, Android 16. It has 4 KiB memory pages. The APK contains only `arm64-v8a` libraries with 16 KiB aligned ELF `LOAD` segments; execution on a 16 KiB page device remains untested.
- Reached gameplay, menus, audio initialization, a road scene with HUD, and fullscreen on this phone. The user played and checked the vehicle artifacts. The Android folder picker has not yet been verified end to end on an empty app profile. Gamepad behavior, longer play sessions, and a formal FPS comparison remain unverified.
- Final development APK: `out/android-arm64/MCLA-arm64.apk`, copied to `/storage/emulated/0/Download/MCLA-Recomp-arm64.apk`. Both are 105 MiB and have SHA-256 `94b5fc1ac47556efa790b858099ed96226fff9f15c5826de44133854a519de3e`. It is signed with a local debug keystore, not a release key. No game files or external Turnip binary are bundled.
- The final APK was **built and statically verified, but not installed or run**. The most recent gameplay observation predates its per-driver Vulkan cache and final log-export filename changes. Do not present those changes as runtime-validated.
- The user explicitly asked to stop repeated direct tests on the phone. Next work should emphasize code review, offline build checks, documentation after each change, and a small, planned on-device validation only after the user authorizes the next build round.

## What was changed

| Area | Implementation and reason | Verification |
| --- | --- | --- |
| ReXGlue SDK | `patches/rexglue-android-arm64.patch` on SDK commit `f5337cdc947ff6d4c4196737e2c807a48f2a1fc2`: Android CMake target, SDL/Android surface, AArch64 fiber context instead of unavailable Bionic `ucontext`, libc++ chrono compatibility, Android storage/shared memory paths, and Vulkan/runtime adaptation. | SDK and game compiled; plugin loaded and rendered on device. Patch applicability was checked on a clean SDK tree. |
| Main game | `CMakeLists.txt` builds `libmain.so` and exports `SDL_main`; `src/mcla_app.cpp` uses Android Vulkan cache default; `src/perf_options.cpp` keeps environment reflections updated each frame on Android. | Main library linked; game reached gameplay. The reflection change was confirmed visually by the user. |
| Android host | `android/MclaActivity.java` starts SDL, stages `librexgpu-xenos.so` into app-private storage, uses immersive fullscreen, and requests sustained performance mode only when supported. | Fullscreen gameplay captured; sustained-mode effect on FPS was not isolated. |
| Launcher and data | `android/MclaLauncherActivity.java` imports 13 files from a selected extracted folder into `files/game/`, with progress and `.partial` cleanup. English UI has **Select game folder**, **Select Turnip (.zip)**, **Use system driver**, and **Play**. No broad storage permission is needed for the Android document picker. | Existing private game files detected and matched the extracted source. Folder-picker import on an empty app profile remains unverified. Launcher English UI captured. |
| Optional Turnip | `patches/rexglue-turnip-loader.patch`, `scripts/build_adrenotools_android.sh`, and launcher ZIP import use AdrenoTools. Selected driver lives under `files/gpu-drivers/`; system Vulkan remains available in the launcher. | Both OneUI and Standard Turnip loaded and rendered; both had white rear-light flashes on this device. |
| Pipeline cache | Android enables `vulkan_pipeline_cache_file` by default. `patches/rexglue-driver-cache-path.patch` keys the binary cache by title, vendor, device, driver version, and Vulkan cache UUID; validates old cache headers before migration. | Earlier single-driver cache gave a 15.8 s to 0.47 s pipeline-storage startup interval on successive runs. New per-driver path compiled but was not run. This does not prove sustained FPS gains. |
| Logs | `android/MclaLogExporter.java` exports private `mcla_*.log` to shared `Documents/MCLA/logs/*.txt` via MediaStore on Android 10+. It runs from the launcher and during pause/close and timed gameplay. | Earlier version exported 18 intact logs, but Android gave them `.log.txt` names. Final `.txt` naming and migration compiled, but were not run. |
| APK packaging | `scripts/build_android_apk.sh` compiles Java, packages DEX and ten ARM64 libraries, verifies architecture and 16 KiB ELF alignment, aligns/signs/verifies the APK, and copies it to Downloads. | Final script completed; copied APK hash matches. |

The SDK patch order on a clean checkout is: `rexglue-android-arm64.patch`, `rexglue-turnip-loader.patch`, then `rexglue-driver-cache-path.patch`. The SDK checkout and generated game code are ignored by Git. The Android additions in this working tree have not been committed; preserve the modified and untracked files before resetting or cloning anew.

## Chronological record and evidence

All local `out/` captures are ignored by Git and may disappear if the worktree is cleaned. The game image, extracted game data, saves, and debug signing key are also local and should not be committed or shared as source artifacts.

| Stage | Observation and evidence | Conclusion / remaining limit |
| --- | --- | --- |
| Game input | The user's own multipart ZIP arrived in 27 volumes; `.016.jpg` contained ordinary ZIP data. Combined locally, checked with `unzip -t`, and extracted to 13 files. Codegen found Title ID `545407F8`, Media ID `5940C9DB`. | Input was valid; original volumes were not modified. |
| First Android launch | `mcla_004.log` recorded `VK_KHR_android_surface`, Adreno 740, swapchain, 30,094 mapped guest functions, shader storage and 48 kHz audio. Initial screen was black during shader preparation. | Fixed `/proc/self/exe` storage path, Xenos plugin placement, and Android shared-memory initialization. |
| Rendered game | `mcla_005.log` showed graphics pipelines and menu option changes; `out/android-arm64/screen-current.png` shows a street/car/menu. | Gameplay renderer and input to menus ran on ARM64. Gamepad mapping was not established. |
| Fullscreen | `out/android-arm64/screen-fullscreen.png`, `screen-fullscreen-2.png`, `screen-fullscreen-3.png`; `mcla_008.log` loaded 676 graphics pipelines. | The temporary black loading screen did not persist in that run; its earlier cause was not isolated. |
| Performance baseline | `mcla_009.log` spent 26.4 s from shader storage initialization to 805 pipelines from storage. `out/android-arm64/present-profile.csv` had about 12 presentations/s over a 10 s sample, with irregular frame times. `simpleperf` saw about 3.29 CPUs occupied and ~1.3 GiB RSS. | GPU was near 94–99% busy; thermal/power limit reduced max GPU clock from 719 MHz to 295/220 MHz. No reliable sustained-FPS improvement was established. |
| Shader cache | `mcla_013.log`: 822 pipelines, 15.8 s measured interval, 18,276,386-byte Vulkan binary cache written. `mcla_014.log`: same 822 pipelines, 0.47 s interval with warm cache. `out/android-arm64/screen-cache-warm.png` shows gameplay. | Warm cache greatly reduced startup pipeline preparation on this pair of runs. A short SurfaceFlinger sample still showed ~13.1 presentations/s under a 348 MHz GPU cap. |
| Vehicle paint | User saw brief black flashes on vehicles with `mcla_env_map_interval=4`. Restoring every-frame reflection updates removed the issue according to the user. `out/android-arm64/screen-reflection-retest.png` shows a rendered frame. | Keep interval 0 on Android. A still image cannot by itself prove a transient flash is gone. |
| Turnip loading | OneUI ZIP imported; `mcla_016.log` identified `Turnip Adreno (TM) 740`, Vulkan 1.4.367 and `driverID: MesaTurnip`; `out/android-arm64/mcla-turnip-test2.png` rendered. Standard variant also loaded. | Optional driver selection works, but visual compatibility varies. |
| Rear lights | OneUI screenshots `rear-081226.png`, `rear-081229.png`, `rear-081232.png` and `mcla-rear-test.mp4` show dark-to-white flashes at 0 mph (`mcla_017.log`). Standard screenshots `rear-standard-082217.png`, `rear-standard-082221.png`, `rear-standard-082223.png` show the same (`mcla_019.log`). | Changing between those two Turnip variants did not fix the artifact. |
| System driver comparison | `mcla_020.log` identifies `Adreno (TM) 740`, Vulkan 1.3.128. `out/android-arm64/mcla-system-rear-gameplay.mp4` shows normal rear lights while driving/braking; user confirmed no flashing but slight slowdown and stutters. | System Vulkan is the current recommended choice on this phone. Thermal conditions and cold cache prevent a fair FPS comparison. |
| Final package | `out/android-arm64/full-rebuild-final.log`: 112/112 and `libmain.so` linked. `scripts/build_android_apk.sh` verified ten ELF64 AArch64 libraries, 16 KiB alignment, APK signature and ZIP alignment. Hashes of `out/` and Downloads copies matched. | Packaging succeeded. This final APK was not installed or played. |

Some older sections in `ANDROID-PORT-NOTES.md` say the system-driver comparison or Standard Turnip test was still pending. They are historical checkpoints; the later rows above supersede those pending statements.

## Current local layout

- Source: `~/mcla-recomp/`; Android Java in `android/`, SDK patches in `patches/`, packaging and log scripts in `scripts/`.
- SDK working checkout: `third_party/rexglue-sdk/`; installed libraries: `third_party/rexglue-sdk/out/install/android-arm64/lib/`. Build directories: `third_party/rexglue-sdk/out/build/android-arm64/`, `out/build/mcla-android-arm64/`, `out/build/adrenotools-arm64/`.
- Final APK: `out/android-arm64/MCLA-arm64.apk` and `/storage/emulated/0/Download/MCLA-Recomp-arm64.apk`; local build/capture evidence under `out/android-arm64/`.
- Extracted user's game input: `game/` for codegen and `/storage/emulated/0/Download/MCLA-extraido/` for the Android picker. Installed copy: `/data/user/0/org.mcla.recomp/files/game/`. The app keeps private logs in `files/logs/`, shader caches in `files/mcla/cache/shaders/`, driver selection in `files/gpu-drivers/` and exports logs to `Documents/MCLA/logs/` on supported Android.
- The system driver is currently selected. A prior Turnip selection marker was backed up as `selected.txt.turnip-backup`; do not silently restore it. `pm install -r` preserves app data, whereas clearing app data removes the imported game, saves, caches and selection.

## Open questions and next work

1. Review Vulkan pipeline and shader work offline, especially the cost of pipeline creation and cache invalidation, before proposing more optimization. Record the code path, expected effect, and a specific measurement that would distinguish improvement from thermal throttling.
2. Review launcher importer and log exporter. On the next authorized device test, verify the clean-profile folder-picker import and the `.txt` log names; do not claim success based on Java compilation alone.
3. Verify the new per-driver Vulkan cache on the next authorized run using the system driver first. Record cold/warm cache bytes and timings. Avoid switching drivers repeatedly merely to collect data.
4. Investigate rear-light flashes with Turnip as a separate compatibility issue. Keep the system driver available and selected by default until a candidate fix survives a short, focused comparison. Do not conflate the rear-light issue with the earlier black vehicle-reflection flash.
5. Optimize sustained performance only with comparable temperature, GPU clock, scene, and duration. Cache startup timings and short frame samples cannot establish a stable FPS change. Controls, long-session stability, the 16 KiB page runtime and full clean-install import are also open.

## Build and test cadence agreed with the user

For the next round, first review and edit code, run compilation and static packaging checks, and update this handoff and the chronological notes with each meaningful change. Build candidates can be created without launching them. When a candidate has a clear hypothesis and a short test plan, ask the user before initiating that build round or any direct phone test. The final build before this pause has already been delivered; no additional build or gameplay run is authorized by this documentation request alone.
