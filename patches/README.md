# Android SDK patches

The active recipe targets ReXGlue v0.10.0 at commit
`f5337cdc947ff6d4c4196737e2c807a48f2a1fc2`. Apply these patches in order to a
clean checkout:

1. `rexglue-android-arm64.patch` — the reviewed SDK/fork Android ARM64 delta.
2. `rexglue-turnip-loader.patch` — optional user-selected Turnip loading.
3. `rexglue-driver-cache-path.patch` — Vulkan cache separation by driver.
4. `rexglue-vulkan-pipeline-background-work.patch` — on Android, run pipeline
   compilation and pipeline-cache writes below the game and render threads, and
   keep the driver pipeline-cache save out of a burst of new pipelines (for
   example while a car loads).
5. `rexglue-vulkan-occlusion-query-pipeline-wait.patch` — time the pipeline
   creation and the pipeline wait of the draws inside an occlusion query, and
   let both be bounded by a cvar (`vulkan_occlusion_query_pipeline_wait_ms`,
   `vulkan_occlusion_query_async_pipelines`) so their cost can be measured on
   the device before the behavior is changed.
6. `rexglue-android-present-filters.patch` — compile the presenter's spatial
   scaling filters (FidelityFX's Contrast Adaptive Sharpening and FSR 1, whose
   shaders are in the SDK tree and need neither its source nor its runtime)
   into the Android build, so `present_effect = cas` or `fsr` can be tried
   there. The temporal upscaler still needs the runtime and is not offered.
7. `rexglue-android-sdl-event-pump.patch` — avoid a second joystick/HIDAPI
   maintenance pump before the first Android event wait when the entry pump
   has already run. Later wakeups and lifecycle events retain the SDL path.
8. `rexglue-pipeline-storage-layout.patch` — expose the native Vulkan pipeline
   record size so importers can reject incompatible packed-bitfield layouts.
9. `rexglue-android-present-stats.patch` — retain the last 256 successful
   Android host-presentation timestamps for optional FPS and timing summaries.

The first patch is a combined source delta and is intentionally pinned by
SHA-256 in `scripts/ci/check_pins.py`; it is not described as Android-only.
See [android-build.md](../docs/android-build.md) and
[android-sdk-source.md](../docs/android-sdk-source.md) for the full recipe.

The inherited Windows/PS5 patch catalogue remains in
[`docs/fork-base/patches/README.md`](../docs/fork-base/patches/README.md) for
historical provenance. Those patches are not part of the active Android CI or
build recipe.
