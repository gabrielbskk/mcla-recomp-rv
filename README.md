<p align="center">
  <img src="docs/assets/mcla-recomp-project-cover.png" alt="Midnight Club Los Angeles Complete Edition: Android recompilation" width="480">
</p>

<h1 align="center">MCLA RECOMP <sub>RV</sub></h1>
<p align="center"><strong>LOS ANGELES, RECOMPILED.</strong><br>
An unofficial Android ARM64 port of <em>Midnight Club: Los Angeles Complete Edition</em> for Xbox 360.</p>

<p align="center">
  <a href="https://github.com/gabrielbskk/mcla-recomp-rv/releases/tag/stable-snap"><strong>DOWNLOAD</strong></a>
  &nbsp; · &nbsp; <a href="docs/android-testing.md">INSTALL GUIDE</a>
  &nbsp; · &nbsp; <a href="docs/android-compatibility.md">COMPATIBILITY</a>
  &nbsp; · &nbsp; <a href="docs/android-build.md">BUILD</a>
</p>

<p align="center">Discord: <strong>gabrielbskk</strong></p>

> **Early Android port.** Compatibility and performance vary. You need your
> own legally obtained Xbox 360 game files; they are not included.

## Choose a build

Both APKs are available in the [same release](https://github.com/gabrielbskk/mcla-recomp-rv/releases/tag/stable-snap):

| APK | Intended device | Status |
| --- | --- | --- |
| **stable-snap.apk** · v0.1.45 | Snapdragon / Adreno | Maintainer reports good results on Galaxy S23; broader compatibility and controlled FPS are unverified. |
| **mali-experimental.apk** · v0.1.23 | Mali diagnostic | **Experimental only:** no validated gameplay on the tested Mali-G52. |

The RedMagic / 8 Elite diagnostic is withheld while reported audio cuts are
investigated. See [release details and hashes](docs/android-release.md).

## Compatibility at a glance

| Device | Result | Source |
| --- | --- | --- |
| Galaxy S23 · Snapdragon 8 Gen 2 · 8 GB | Current v0.1.45 reportedly performed well; no controlled FPS measurement. | Maintainer test |
| Zenfone 9 · Snapdragon 8+ Gen 1 · 6 GB | Older builds ran with reported freezes and lag; 5–15 FPS estimated, not benchmarked. | [Tester · #5](https://github.com/gabrielbskk/mcla-recomp-rv/issues/5) |
| Galaxy Z Fold 5 · Snapdragon 8 Gen 2 · 12 GB | v0.1.24 reached a file-I/O error; no confirmed gameplay or v0.1.45 retest. | [Tester · #6](https://github.com/gabrielbskk/mcla-recomp-rv/issues/6) |
| Galaxy A06 · Helio G85 · Mali-G52 | Stock driver lacks a required Vulkan feature; tested PanVK failed swapchain creation. | Tester report |
| RedMagic 11 Air · Snapdragon 8 Elite | Stereo audio logged, but cuts reported; gameplay and FPS unverified. | Tester report |

Older S23 runs measured 14.223 presented FPS on system Vulkan and 22–24 FPS
in a separate Turnip run. They used different settings and thermal conditions:
**they are not a controlled comparison or v0.1.45 benchmarks**. For evidence,
device details and likely requirements, see [compatibility and performance](docs/android-compatibility.md).

## Get started

1. Install the appropriate APK from [Releases](https://github.com/gabrielbskk/mcla-recomp-rv/releases/tag/stable-snap).
2. Extract the 13 files from your own game copy and select their folder in the
   launcher. Allow about **6.2 GB** for the imported copy, plus source files
   and cache space. See the [installation guide](docs/android-testing.md).
3. Start with the system Vulkan driver. Optionally import a **trusted** Turnip
   ZIP for Adreno or PanVK ZIP for Mali; imported drivers run native code.
4. Adjust visual settings in the game's own **Options / Display** menu. The
   launcher's driver selector is separate. Touch controls and their editor are
   experimental; one gamepad is the validated input method.

Both APKs share a package ID and replace each other. For compatible updates use
`pm install -r`; **do not uninstall or clear data** if you want to keep your
game files and saves. Downgrades can be rejected by Android. Shared logs are
exported to `Documents/MCLA/logs/` on Android 10+; reopen the launcher after
a crash if necessary.

## Documentation

- [Compatibility, performance and likely requirements](docs/android-compatibility.md)
- [Installation and testing](docs/android-testing.md) · [Known issues](KNOWN-ISSUES.md)
- [Release notes and hashes](docs/android-release.md) · [Mali investigation](docs/android-mali-driver.md)
- [Build from source](docs/android-build.md) · [Android status](docs/android-status.md)
- [Security policy](SECURITY.md) · [Credits](CREDITS.md) · [Third-party notices](THIRD-PARTY-NOTICES.md)

This project uses static recompilation, C++23, SDL3, Vulkan and ReXGlue; it is
not an x86 emulator or an official release. APKs are debug-signed and contain a
native library generated from the game executable, but no extracted game
assets, saves, shader pack, driver ZIP or signing key. The source repository
does not contain game data or generated game code. See the [security policy](SECURITY.md)
before importing third-party drivers or sharing logs.

Built on [holdmysocks/mcla-recomp](https://github.com/holdmysocks/mcla-recomp),
the [RenanQueiroz fork](https://github.com/RenanQueiroz/mcla-recomp), ReXGlue,
SDL3 and AdrenoTools. This unofficial project is not affiliated with the game's
rights holders. Source code is licensed under [GPL-3.0-or-later](LICENSE).
