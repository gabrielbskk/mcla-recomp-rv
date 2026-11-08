# Android port history

This is a concise record of decisions and findings that still matter. The
former chronological `ANDROID-PORT-NOTES.md` was consolidated here so the
repository no longer carries local device paths, private storage details or
step-by-step test chatter.

## Baseline and native target

The project started from the RenanQueiroz MCLA recompilation fork and ReXGlue
v0.10.0. The Android target uses ARM64 C++, SDL3 for activity/window/input and
Vulkan for rendering. It does not run the Linux build through proot, Box64 or
an emulator. Android needed its own CMake selection, SDL surface integration,
Android storage paths, shared-memory initialization and an AArch64 fiber
backend because Bionic does not provide the Linux `ucontext` path.

The Android SDK recipe is pinned to commit
`f5337cdc947ff6d4c4196737e2c807a48f2a1fc2`. The large Android patch preserves
the reviewed SDK/fork delta; two smaller patches add optional Turnip loading
and driver-specific Vulkan cache paths.

## Runtime milestones

- The first successful ARM64 package initialized SDL, Android Vulkan, the
  Xenos plugin, shared memory, audio and the guest function map.
- Later candidates reached menus, fullscreen, audio and a rendered road scene
  with HUD on a Samsung Galaxy S23 using an Adreno 740. The launcher was then
  changed to English and gained **SELECT TURNIP (.ZIP)**, **USE SYSTEM DRIVER**
  and **PLAY**.
- The user supplied extracted game files locally. They are never part of the
  source repository, CI or public documentation artifacts.
- The original low-end reflection interval caused brief black vehicle flashes;
  keeping Android environment reflections updated every frame removed that
  artifact in the user's test. This is separate from the later rear-light
  issue.

## Vulkan and performance findings

The warm Vulkan cache reduced pipeline-storage preparation from about 15.8
seconds to 0.47 seconds in a controlled pair of runs. That result concerns
startup preparation only and does not prove higher sustained FPS. The phone
was observed under thermal/power limits, and short presentation samples showed
low and uneven frame rates.

Turnip OneUI and Standard v27.0.0-20261107 both loaded and rendered, but both
showed the same intermittent white rear-light flash. The system Vulkan driver
removed that artifact in the user's comparison, although performance felt a
little lower and stutters remained. The system driver is therefore the current
recommended default while the Turnip issue is investigated.

Older logs identify the runtime as `realme/RMX5070`; device spoofing was active
and the physical phone was a Galaxy S23. That historical string must not be
used as device evidence. The tested phone uses 4 KiB pages; 16 KiB page-device
runtime is still unverified.

## Launcher, data and logs

The launcher imports 13 extracted files through the Android document picker
into app-private storage and does not require broad storage permission. It
checks presence and nonzero size, not hashes. A selected Turnip ZIP is native
code and must come from a trusted source. Logs are kept privately and exported
to `Documents/MCLA/logs/` through MediaStore. The final `.txt` naming change,
clean-profile import, per-driver cache behavior and long-session stability
still need focused runtime validation.

Gamepad is the only supported and validated in-game input method at present.
Keyboard and mouse may work through Android/SDL but were not verified. Touch
controls are planned for a future update and are not implemented.

## Distribution and verification decisions

The final local APK was packaged and statically verified for ARM64 ELF and 16
KiB `LOAD` alignment, APK signature and ZIP alignment, but it was not installed
or played after the final cache and log-export changes. The public release
asset still contains generated native game code; the preferred future release
should distribute source and a build recipe only, subject to an explicit
release decision. No additional build or device test is implied by this
history rewrite.
