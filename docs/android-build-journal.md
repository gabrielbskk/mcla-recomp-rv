# Android build journal

Record each candidate here when code changes. Link to the relevant `ANDROID-PORT-NOTES.md` investigation and update [android-handoff.md](android-handoff.md) whenever the current state changes. Building an APK and testing it on the phone are separate milestones. Do not mark a change as fixed merely because it compiles.

## Baseline B0 — 2026-11-08, final build before pause

| Field | Record |
| --- | --- |
| Intent | Deliver one last ARM64 APK, then pause direct tests and document the port. |
| Code | English launcher; optional Turnip selection; MediaStore log exporter with `.txt` naming; Vulkan binary cache keyed per driver; Android reflection interval kept at 0. See [handoff](android-handoff.md) for file-level changes. |
| Build | Main target `mcla` completed 112/112 and linked `libmain.so`; `scripts/build_android_apk.sh` completed. |
| Static checks | Ten ELF64 AArch64 libraries with 16 KiB aligned `LOAD` segments; APK ZIP alignment and signature verified; `git diff --check` passed. |
| Artifact | `out/android-arm64/MCLA-arm64.apk` and `/storage/emulated/0/Download/MCLA-Recomp-arm64.apk`, both SHA-256 `94b5fc1ac47556efa790b858099ed96226fff9f15c5826de44133854a519de3e`, 105 MiB. |
| Device test of this exact APK | None, as requested. Previous installed versions reached gameplay and were tested with stock Vulkan and two Turnip variants. |
| Known open behavior | Turnip rear-light white flash; lower/uneven FPS and stutters on stock Vulkan; clean-profile folder import, final log names, per-driver cache, gamepads, and 16 KiB page runtime not verified. |
| Next gate | Ask the user before beginning the next build round. Review code and prepare a focused candidate/test plan first. |

## Entry template for the next candidate

Copy these fields for each candidate. Fill in “not run” when a check or device test did not occur, and keep raw captures in ignored `out/` directories with a stable filename.

| Field | Record |
| --- | --- |
| Candidate/date |  |
| Authorized scope |  |
| Problem and evidence |  |
| Code files and rationale |  |
| Expected observable change |  |
| Offline review/build checks |  |
| APK path, size, SHA-256 |  |
| Planned on-device test (if needed) |  |
| Actual device, driver, thermal state |  |
| Logs/captures |  |
| Result, limits, next action |  |

The release candidate should have one or a few related changes and a focused pass/fail condition. A slow or hot run alone cannot establish a regression. Record the GPU clock cap, approximate temperature, scene and duration whenever comparing FPS. When the goal is only to check packaging or Java compilation, no gameplay run is necessary.
