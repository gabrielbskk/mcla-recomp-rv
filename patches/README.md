# Android SDK patches

The active recipe targets ReXGlue v0.10.0 at commit
`f5337cdc947ff6d4c4196737e2c807a48f2a1fc2`. Apply these patches in order to a
clean checkout:

1. `rexglue-android-arm64.patch` — the reviewed SDK/fork Android ARM64 delta.
2. `rexglue-turnip-loader.patch` — optional user-selected Turnip loading.
3. `rexglue-driver-cache-path.patch` — Vulkan cache separation by driver.

The first patch is a combined source delta and is intentionally pinned by
SHA-256 in `scripts/ci/check_pins.py`; it is not described as Android-only.
See [android-build.md](../docs/android-build.md) and
[android-sdk-source.md](../docs/android-sdk-source.md) for the full recipe.

The inherited Windows/PS5 patch catalogue remains in
[`docs/fork-base/patches/README.md`](../docs/fork-base/patches/README.md) for
historical provenance. Those patches are not part of the active Android CI or
build recipe.
