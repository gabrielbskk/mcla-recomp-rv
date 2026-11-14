# Mali support discontinued

As of 2026-11-14 this Android project targets Snapdragon / Adreno. The Mali
diagnostic APK has been withdrawn from distribution and the Mali-specific
launcher/importer and feature-relaxation build path have been retired.
There is no supported Mali/Immortalis build or PanVK integration roadmap.

The tested stock driver lacked a Vulkan feature required by the current Xenos
renderer, and the tested PanVK build failed Android swapchain creation. A
correct solution would require significant renderer/feature-fallback and
Android driver/WSI integration work, followed by device validation. It is not
a matter of selecting a different ZIP or adjusting performance variables.

This is a project-scope and maintenance decision. The evidence does **not**
prove Mali support is impossible in principle or that the entire recompilation
project must be rewritten. No experimental APK established correct gameplay.

The evidence below is retained for historical reference only. Its descriptions
of a separate launcher, importer, and proposed tests are not current support
instructions. No downloaded driver can turn the supported Adreno build into a
validated Mali build. Do not uninstall or clear app data to switch variants.

## Archived Mali-G52 investigation

The Galaxy A06 / Helio G85 / Mali-G52 MC2 tester reported failure both with
the stock Arm Vulkan driver and with the tested Mesa PanVK 26.3.0-devel build.
Neither path is validated for gameplay. Do not ship or advertise Mali support.

## Separate failures

- Stock Arm driver: creates a 1600x720 swapchain and records presentations,
  but does **not** expose `vertexPipelineStoresAndAtomics`. The local diagnostic
  runtime relaxes the device gate; `graphics/vulkan/command_processor.cpp`
  still rejects vertex shader memory export draws without this feature.
  Presentations and frame-rate counters do not establish correct gameplay.
- PanVK: reports `vertexPipelineStoresAndAtomics` but the tested Android build
  logs `VK_ERROR_INVALID_EXTERNAL_HANDLE` in `panvk_android.c:182`, then fails
  to create a swapchain. This is a distinct Android WSI / gralloc external
  buffer integration issue, not evidence that the shader feature is missing.
  The `crash trace: ... guest functions mapped` line does not by itself prove
  a process crash.

## Driver integration boundaries

The Vulkan loader now accepts only two selected library basenames: Turnip's
`libvulkan_freedreno.so` and PanVK's `libvulkan_panfrost.so`. The Mali importer
also accepts the old PanVK ZIP layout with a `libvulkan_freedreno.so` filename,
but stages it under its PanVK name. An ELF architecture check alone cannot
prove the library is PanVK or that its transitive dependencies are available.
The system Vulkan driver remains the default. The Mali launcher does not show
Turnip debug options and does not parse or apply `TU_DEBUG`; it is not a
PanVK option. The builds have different native runtimes and must stay separate.

Mesa documents PanVK Vulkan 1.3 support for Bifrost G52, but this is not a
claim that a particular Android user-space package works with a particular
vendor kernel, gralloc, or Android loader. Mesa's Android documentation
describes a PanVK NDK build with `-Dvulkan-drivers=panfrost`,
`-Dallow-fallback-for=libdrm`, `-Dmesa-clc=system`, and
`-Dprecomp-compiler=system` (after preparing the offline compiler). Building
one is not a substitute for checking its platform dependencies and WSI on
the target device. Android documents `VK_ANDROID_native_buffer` and gralloc
as the bridge used to allocate and import swapchain images; this is the first
layer to investigate for the observed external-handle error.

## Validation when a tester returns

1. Keep signed Snapdragon and Mali diagnostic APKs and their runtime libraries
   distinct; never install a test by uninstalling the existing app (that loses
   private data). The current manifests share a package ID, so an install of
   one variant replaces the other; a second package ID requires an explicit
   data-migration decision.
2. Reproduce with the exact PanVK binary and record its build ID, dependencies,
   loader messages, device name, surface capabilities and swapchain result.
   Compare a minimal Android Vulkan swapchain program on the same driver to
   distinguish driver integration from presenter settings. Change one
   variable per run; do not change occlusion-query behavior without timing.
3. If PanVK WSI works, check shader output, missing draws, device loss,
   gameplay stability and frame times. Separately, a stock-driver solution
   requires a correct vertex-memory-export fallback (potentially a compute
   dispatch with synchronization), not merely disabling the feature gate.

References:
- https://docs.mesa3d.org/drivers/panfrost.html
- https://docs.mesa3d.org/android.html
- https://source.android.com/docs/core/graphics/implement-vulkan
