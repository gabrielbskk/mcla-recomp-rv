# Changelog

Public build notes for the Android ARM64 port. Entries describe what changed for
players; internal engineering detail stays in the private source repository.

Versioning follows the APK `versionName` (`0.1.<build>`); the build number is
the APK `versionCode`.

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
