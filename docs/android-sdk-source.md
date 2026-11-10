# Android SDK source strategy

The Android port uses ReXGlue v0.10.0 from commit `f5337cdc947ff6d4c4196737e2c807a48f2a1fc2`. The current `rexglue-android-arm64.patch` is a reviewed combined delta: it carries the fork's SDK changes together with the Android ARM64 host changes. It is 20 MB because it changes the SDK tree in many places. It must not be described as an Android-only patch.

## Reproducible source currently supported

Start from a clean checkout of the pinned upstream commit and apply the seven patches in this order:

```sh
git clone --recursive https://github.com/rexglue/rexglue-sdk.git third_party/rexglue-sdk
git -C third_party/rexglue-sdk checkout f5337cdc947ff6d4c4196737e2c807a48f2a1fc2
git -C third_party/rexglue-sdk submodule update --init --recursive
git -C third_party/rexglue-sdk apply patches/rexglue-android-arm64.patch
git -C third_party/rexglue-sdk apply patches/rexglue-turnip-loader.patch
git -C third_party/rexglue-sdk apply patches/rexglue-driver-cache-path.patch
git -C third_party/rexglue-sdk apply patches/rexglue-vulkan-pipeline-background-work.patch
git -C third_party/rexglue-sdk apply patches/rexglue-vulkan-occlusion-query-pipeline-wait.patch
git -C third_party/rexglue-sdk apply patches/rexglue-android-present-filters.patch
git -C third_party/rexglue-sdk apply patches/rexglue-android-sdl-event-pump.patch
```

The first patch includes the SDK changes needed by the current host fork and the Android port. The smaller patches add optional Turnip loading, driver-specific Vulkan cache paths, lower-priority pipeline compilation and cache writing on Android, the timing and the cvar bounds of the occlusion-query pipeline wait, the presenter's scaling filters, and a reduced initial SDL event pump on Android. Keep the SDK checkout clean before applying them; never apply a patch twice.

## Fixed SDK fork alternative

A future maintainer can replace the combined patch with a dedicated public SDK fork:

1. Fork `https://github.com/rexglue/rexglue-sdk`.
2. Check out `f5337cdc947ff6d4c4196737e2c807a48f2a1fc2`.
3. Apply the seven patches above, run `git diff --check`, and review the resulting file list for game data or generated output.
4. Commit the patched SDK, publish that commit in the fork, and pin its full commit SHA in the Android build documentation and CI.
5. Run `git apply --check` from a clean copy before accepting the fork as the new source.

The Android repository does not claim that such a separate SDK fork exists yet. Until a maintainer creates and reviews it, the pinned upstream commit plus the seven versioned patches above is the authoritative source recipe. A fork commit must not be invented in documentation.

## Patch validation status

The patch order and SHA values are tracked by the Android repository. This documentation change does not compile the SDK and does not prove that a fresh checkout links the host. The next offline verification should run `git apply --check` on a disposable clean SDK checkout; a complete compile remains a separate build action.
