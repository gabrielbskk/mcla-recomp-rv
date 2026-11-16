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
> > **This is the final public release.** Development is paused indefinitely.
> > I'm stepping away from recompilation work and AI-related projects due to
> > internal circumstances. The private repository will remain but no further
> > public builds are planned. No ETA for any future work.
> >
> > The current build (v0.1.61) is stable for daily use on supported devices.
> > Frame generation (LSFG 2×) was experimentally integrated but remains
> > unpublished and incomplete.

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
