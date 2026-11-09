# Android frame generation feasibility

Status: the standalone Android ARM64 FidelityFX Vulkan runtime builds; no
frame-generation APK or device test has succeeded yet.

The patched ReXGlue SDK contains a Vulkan frame generation path and an FSR 3.1
runtime recipe. The installed Android SDK was configured with
`REXGLUE_ENABLE_FSR=OFF`, so the current APK cannot use that path.

The existing runtime loads `libamd_fidelityfx_vk.so` outside Windows. A Windows
`.dll` cannot be loaded directly by an Android ARM64 process; its source and
license may still be useful if it corresponds to the same FidelityFX version.
The particular user DLL has not been supplied for inspection.

The SDK recipe expects prebuilt Vulkan shader permutations outside Windows.
An isolated configuration using the pinned FidelityFX source and those shaders
successfully built `libamd_fidelityfx_vk.so` on Android ARM64. The resulting
library is an AArch64 ELF shared object, is approximately 1.3 MiB, has 16 KiB
load alignment, exports the five ffx-api entry points used by ReXGlue and links
against Android's `libvulkan.so`. This proves build feasibility, but not device
compatibility or usefulness in this title.

Enabling FSR must remain a separate experiment: rebuild the ReXGlue SDK with
FSR enabled, package the runtime, verify the Vulkan device features and memory
cost, then measure native game FPS, displayed FPS, frame pacing, visual
artifacts, latency and temperature on the same scene. Generated frames do not
reduce the time spent computing a real game frame. At the currently observed
low native frame rate, added latency and interpolation artifacts may outweigh
the apparent smoothness. The baseline overlay showed 16.9 native FPS in one
captured driving scene; this is too low to expect a good frame-generation
experience without first improving native rendering performance.
