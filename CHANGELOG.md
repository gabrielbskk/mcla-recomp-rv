# Changelog

Public build notes for the Android ARM64 port. Entries describe what changed for
players; internal engineering detail stays in the private source repository.

Versioning follows the APK `versionName` (`0.1.<build>`); the build number is
the APK `versionCode`.

> **Snapdragon / Adreno only.** Mali, Immortalis and PanVK devices are not
> supported. Found a bug? Ping **Discord `gabrielbskk`** — fastest way to reach
> the maintainer; include phone model, SoC, Android version, driver and APK
> version, plus the log from `Documents/MCLA/logs/MCLA-diagnostic.txt`.
>
> **Active development has resumed**, focusing on Snapdragon / Adreno performance,
> thermal stability, and Turnip / Vulkan rendering refinements.

## 0.1.63 — full-screen restoration, Turnip syncdraw stability, and S23 performance fixes *(Experimental)*

Experimental follow-up build addressing Galaxy S23 test feedback and diagnostic
telemetry from experimental build `0.1.62`. Still under active device validation.

- **Full-screen presentation restored.** Removed `SurfaceHolder` hardware scaler
  overrides so SDL3 / Vulkan presents across the full native display panel while
  keeping internal Xenos 3D rendering at 720p (`mcla_render_height = 720`).
- **Turnip `syncdraw` enabled by default again.** Restores `TU_DEBUG=syncdraw`
  whenever a custom Turnip driver is selected, eliminating flickering wheels and
  headlights on Adreno Turnip drivers while keeping `"No switch"` available in the
  launcher for opt-in testing.
- **Fixed `0.1.62` GPU & memory regressions.** Restored single-pass compute
  decode for unscaled 2D EDRAM-resolved textures (`texture_load_direct_unscaled = true`)
  and restored 256 KiB shared-memory write-watch pages
  (`shared_memory_invalidation_pages_log2 = 6`) to avoid excess staging copies and
  `SIGSEGV` / `mprotect` overhead.
- **Mobile-friendly graphics defaults without overriding `mcla.toml`.** Defaults
  anisotropic filtering to `off` (`anisotropic_override = 0`, replacing the
  desktop 16× default) and MSAA to `off` on Android while letting all in-game
  Pause Menu (`mcla.toml`) choices persist cleanly across launches.
- **Thermal stability on Galaxy S23.** Enabled Android Sustained Performance Mode
  by default (toggleable in Launcher → `03 Extras`) and removed aggressive ADPF
  CPU boosting that triggered thermal throttling during extended drives.
- **Retained from `0.1.62`:** persistent on-disk `VkPipelineCache`
  (`vulkan_pipeline_cache_file`), native Android `TextView` FPS / frametime HUD
  with zero Vulkan render-pass cost, 2-channel stereo audio output default, and
  zero-allocation frame-timing statistics.

## 0.1.62 — experimental Snapdragon optimization test build *(Experimental)*

Experimental test build used to profile Snapdragon 8 Gen 2 / Adreno 740 bottlenecks
on Galaxy S23. **Superseded by `0.1.63`.**

- **Added (kept in `0.1.63`):** persistent disk `VkPipelineCache`, native Android
  `TextView` FPS / frametime / `1% low` overlay (replacing the ImGui Vulkan pass
  on Android), 2-channel stereo output default (`audio_android_force_stereo`),
  and lock-free / zero-allocation frame-timing stats.
- **Experimental changes (reverted or refined in `0.1.63`):** tested `SurfaceHolder`
  fixed-size DPU scaling, Turnip with `TU_DEBUG` cleared by default, ADPF CPU hint
  sessions, launcher `REX_MCLA_*` environment overrides, and `texture_load_direct_unscaled = false`
  + 16 KiB write-watch pages. Device logs (`mcla-current.txt`) showed reduced
  aspect coverage, wheel/headlight flicker on Turnip without `syncdraw`, zero
  direct texture loads, and thermal throttling after sustained boost — all
  resolved in `0.1.63`.

## 0.1.61 — automatic log export and launcher cleanup

Normal variant, no frame generation. No native changes: every native library is
byte-for-byte identical to v0.1.60; only the manifest, the DEX and the signature
metadata differ.

- **Logs export themselves.** The manual button is gone. A low-priority worker
  writes one document, `Documents/MCLA/logs/MCLA-diagnostic.txt`, shared across
  sessions: first pass after 5 s, then every 30 s, plus a pass when returning to
  the launcher and when the game pauses or exits.
- Batches are tagged with a random session UUID, ISO date **with time zone**,
  monotonic time, version, device and selected settings, so a resume can be told
  apart from a new session and the day boundary is visible.
- Incremental append with a persisted cursor per device/inode follows log
  rotation instead of re-copying history; up to 64 KiB per source per cycle.
- **8 MiB cap:** on overflow it keeps roughly the newest 4 MiB from a line
  boundary and writes `HISTORY TRIMMED`.
- No foreground service, no wake lock, no new permissions, no per-frame I/O.
- **Launcher:** manual log button removed and the export path/limit explained in
  place, with the last known export status; controls moved above the
  graphics/advanced options; shaders grouped apart from diagnostics.
- Unchanged: game-file import, driver and shaderpack selection, touch/gamepad
  controls, `syncdraw`, shader pack still disabled, NPC/traffic corrections.
- Known limits: Android 10+ required; not a transactional journal, so a killed
  process can repeat a batch or leave partial text; not a backup of saves; gaps
  possible if sources rotate faster than the collection cycle; the CPU/I/O/GC
  cost of the 4 MiB compaction is **not measured** — no FPS or stutter claim.

## Unreleased

- **Frame generation (LSFG 2×) — experimental, not published yet.** An optional
  2× frame generator is being integrated into the Vulkan presenter. It is off by
  default, requires a compatible device and is still under device testing. No
  quality, latency or power claim is made for it.
- Present-mode handling now prefers `MAILBOX` before `FIFO` when the frame pacer
  shares the graphics queue, and a safety watchdog disables frame generation if
  its connection keeps going outdated.

## 0.1.45 — stable-snap

- Snapdragon / Adreno build published as a long-lived snapshot.
- Mali / Immortalis and PanVK support discontinued; diagnostic APK withdrawn.
- RedMagic / 8 Elite diagnostic withheld pending the audio-cut investigation.

## 0.1.24 — historical

- Earlier public build, kept as an unpublished historical release for reference.

---

Release assets, hashes and per-build caveats live in
[Releases](https://github.com/gabrielbskk/mcla-recomp-rv/releases) and
[release notes](docs/android-release.md).
