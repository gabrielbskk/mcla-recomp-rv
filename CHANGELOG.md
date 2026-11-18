# Changelog

Public build notes for the Android ARM64 port. Entries describe what changed for
players and community testers; full source code and SDK patches live in
[gabrielbskk/mcla-recomp-source](https://github.com/gabrielbskk/mcla-recomp-source).

Versioning follows the APK `versionName` (`0.1.<build>`); the build number is
the APK `versionCode`.

> **Snapdragon / Adreno only.** Mali, Immortalis and PanVK devices are not
> supported. Found a bug? Ping **Discord `gabrielbskk`** — fastest way to reach
> the maintainer; include phone model, SoC, Android version, driver and APK
> version, plus the exported log from `Documents/MCLA/logs/MCLA-diagnostic.txt`.
>
> **Active development has resumed**, focusing on Snapdragon / Adreno performance,
> audio stability, touch controls, and Turnip / Vulkan rendering refinements.

## 0.1.64 — Launcher & Xbox 360 % Pedals Redesign, Snapdragon FPS Profiles, RedMagic/S23 Audio Fix, and Log Recorder Repair *(Experimental)*

> **Experimental build (`v0.1.64` / `versionCode 64`).** Active community test
> release addressing Galaxy S23 gameplay drops, RedMagic 11 Air audio cuts,
> Qualcomm system Vulkan overhead, log export failures, and a full Launcher +
> Touch HUD overhaul.

- **Fixed diagnostic log recording & export (`Documents/MCLA/logs/`).**
  - Restored live background log export every 15 seconds during gameplay plus
    automatic flushes on pause, resume, exit, and launcher open.
  - Self-heals if `MCLA-diagnostic.txt` or `mcla-current.txt` was previously
    deleted or moved in Android `MediaStore`, writes an app-external mirror
    under `Android/data/org.mcla.recomp/files/logs/`, and adds an **Export
    Diagnostic Log Now** button in the launcher.
- **Snapdragon / Galaxy S23 Performance Profiles + Empty Streets mode.**
  - Added selectable launcher performance presets:
    - **S23 Balanced (Recommended):** disables expensive Xbox 360 sun/race/foliage
      shadow cascades, motion blur, depth of field, and fullscreen blur while
      keeping fast blob car shadows, half-rate reflections, and tuned LA city /
      traffic LODs — dramatically reducing draw-call stalls under Turnip
      `TU_DEBUG=syncdraw`.
    - **Max FPS / Low Draw-Call:** aggressive geometry, impostor, and ambient
      density reduction for heavy Turnip `syncdraw` scenes and 6–8 GB RAM phones.
    - **Console Visuals:** leaves original Xbox 360 visual settings / `mcla.toml`
      untouched.
  - Added **Empty Streets** toggle (`mcla_empty_streets`): removes ambient street
    traffic, pedestrians, and parked cars for maximum free-roam / race FPS while
    keeping race opponent AI active.
- **Qualcomm System Vulkan `VKDBGUTILWARN003` storm eliminated (all Snapdragon devices).**
  - Fixed an issue found in multi-device community logs (notably Asus Zenfone 9
    and Snapdragon system-driver runs) where `VK_EXT_debug_utils` and
    `VkDebugUtilsMessengerEXT` were attached even without validation, triggering
    1,000+ internal `VKDBGUTILWARN003` driver callbacks per second across GPU
    threads and saturating log I/O.
- **Audio Anti-Cut Engine for Galaxy S23 & RedMagic 11 Air (Snapdragon 8 Elite).**
  - Implemented Android/Linux host thread priority elevation (`setpriority`) for
    the **Audio Worker** and **XMA Decoder** threads so heavy PowerPC and shader
    compilation threads no longer starve audio mixing.
  - Made the SDL3 realtime audio callback lock-free during 6ch → 2ch downmixing
    and `SDL_PutAudioStreamData` writes, preventing `SubmitFrame` contention.
  - Added selectable audio profiles (**S23 Adaptive 16–28 frames**, **RedMagic 11
    Air / 8 Elite Safe 20–32 frames with 1024-sample AAudio buffer**
    \[auto-selected on RedMagic/nubia devices\], and **Low-Latency 12–24**).
- **Redesigned Midnight Racing Launcher, App Icon & Authentic Xbox 360 Controls with `%` Pedals.**
  - Added the official **MCLA Recomp Android** adaptive & legacy app icon.
  - Redesigned the Android Launcher into a Midnight LA dashboard with top and
    bottom **LAUNCH** buttons, live configuration summary chips, dark-themed
    selectors, and full-screen (`auto` aspect) vs `16:9` toggle at 720p.
  - Rebuilt the touch overlay in authentic **Xbox 360 controller style** (glossy
    `A`/`B`/`X`/`Y` jewel buttons, concave 4-dot `LS`/`RS` thumbsticks, circular
    D-pad dish, sculpted `LB`/`RB` bumpers) with **vertical analog `GAS` (`RT`)
    and `BRK` (`LT`) racing pedals** displaying live **`0%–100%` pressure**
    (slide higher on the pedal for stronger `%` force, lower for gentler
    throttle), plus an upgraded HUD Editor with pedals toggle, size, and opacity
    sliders.
  - Disabled background SDL3 accelerometer-joystick polling and synthetic touch
    mouse events during 240 Hz multitouch driving.

## 0.1.63 — full-screen restoration, Turnip syncdraw stability, and S23 performance fixes *(Experimental)*

Experimental follow-up build addressing Galaxy S23 test feedback and diagnostic
telemetry from experimental build `0.1.62`. **Superseded by `0.1.64`.**

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
  by default and removed aggressive ADPF CPU boosting that triggered thermal
  throttling during extended drives.
- **Retained from `0.1.62`:** persistent on-disk `VkPipelineCache`
  (`vulkan_pipeline_cache_file`), native Android `TextView` FPS / frametime HUD
  with zero Vulkan render-pass cost, 2-channel stereo audio output default, and
  zero-allocation frame-timing statistics.

## 0.1.62 — experimental Snapdragon optimization test build *(Experimental)*

Experimental test build used to profile Snapdragon 8 Gen 2 / Adreno 740 bottlenecks
on Galaxy S23. **Superseded by `0.1.63` / `0.1.64`.**

- **Added (kept in `0.1.63+`):** persistent disk `VkPipelineCache`, native Android
  `TextView` FPS / frametime / `1% low` overlay (replacing the ImGui Vulkan pass
  on Android), 2-channel stereo output default (`audio_android_force_stereo`),
  and lock-free / zero-allocation frame-timing stats.
- **Experimental changes (reverted or refined in `0.1.63`):** tested `SurfaceHolder`
  fixed-size DPU scaling, Turnip with `TU_DEBUG` cleared by default, ADPF CPU hint
  sessions, and `texture_load_direct_unscaled = false` + 16 KiB write-watch pages.

## 0.1.61 — automatic log export and launcher cleanup

Normal variant, no frame generation.

- **Logs export themselves.** Low-priority worker writes
  `Documents/MCLA/logs/MCLA-diagnostic.txt` shared across sessions with session
  UUID, ISO date, device metadata, and an 8 MiB history cap.
- **Launcher:** controls moved above graphics/advanced options; shaders grouped
  apart from diagnostics.

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
