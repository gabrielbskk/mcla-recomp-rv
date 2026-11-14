# Android compatibility and performance

Results apply only to the stated builds and devices, not every phone with the
same GPU. Other than the S23, the entries below are **tester reports**.

| Device | Result | Evidence / limitations |
| --- | --- | --- |
| Galaxy S23 · Snapdragon 8 Gen 2 · Adreno 740 · 8 GB | Maintainer reports good results with Snapdragon v0.1.45. | Earlier builds reached menus, audio and driving; no controlled v0.1.45 FPS benchmark. |
| Zenfone 9 · Snapdragon 8+ Gen 1 · Adreno 730 · 6 GB | Older v0.1.24/25 builds ran, but the tester reported freezes and lag. | [Issue #5](https://github.com/gabrielbskk/mcla-recomp-rv/issues/5): estimated 5–15 FPS with system Vulkan, not a measured benchmark; the displayed 60 FPS appeared inaccurate. No v0.1.45 retest. |
| Galaxy Z Fold 5 · Snapdragon 8 Gen 2 · Adreno 740 · 12 GB | No confirmed gameplay. | [Issue #6](https://github.com/gabrielbskk/mcla-recomp-rv/issues/6): v0.1.24 reached a file-I/O error; cause and v0.1.45 behavior unverified. |
| Mali / Immortalis | Unsupported; diagnostic APK withdrawn. | Tested Mali-G52 stock driver lacked a required feature; tested PanVK failed swapchain creation. [Support discontinued](android-mali-driver.md). |
| RedMagic 11 Air · Snapdragon 8 Elite | Stereo output confirmed in diagnostic logs; tester reported audio cuts. | Gameplay compatibility and FPS are unverified. The RedMagic diagnostic APK is withheld while cuts are investigated. |

## Measured performance: Galaxy S23, earlier builds

- A 150-second v0.1.6 drive using system Vulkan without a shader pack averaged
  **14.223 presented FPS**, with a 110 ms p95 frame interval.
- A separate Turnip 27 run measured **22–24 presented FPS**, with different
  settings and thermal limits. It is not a controlled driver comparison.
- The positive v0.1.45 S23 result is qualitative; no FPS can be inferred.
  Thermal limits, scene, cache state and shader compilation affect results.

Full measurements and caveats: [Android status](android-status.md),
[performance handoff](android-performance-handoff.md), and
[A740 driver investigation](turnip-a740-rendering-artifacts.md).

## Requirements and uncertainty

- ARM64 Android; the APK declares **Android 6.0 / API 23** as its installation
  minimum, not a verified gameplay minimum. Android 10+ supports the documented
  shared-log export.
- A compatible Vulkan driver, including `vertexPipelineStoresAndAtomics` and
  working Android swapchain support. The Vulkan version number alone is not a
  compatibility guarantee. Optional FSR requires Vulkan 1.3; this is not an
  established minimum for the base game.
- Your own legally obtained Xbox 360 game files. Allow approximately **6.2 GB**
  free for the private imported copy, plus the extracted source, caches and OS
  space. About **13 GB free** is a cautious starting estimate when source files
  are on the same device, not a measured hard minimum.
- No reliable RAM or SoC minimum has been established. The S23 has 8 GB; a
  tester with 6 GB reported slow operation on older builds, while a tester with
  12 GB encountered a file-I/O error. More RAM alone does not ensure compatibility.
  Runtime behavior on 16 KiB page-size devices remains unverified.
