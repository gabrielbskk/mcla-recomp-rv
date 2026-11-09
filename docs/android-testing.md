# Testing the Android ARM64 build

This experimental build uses the extracted files from the user's own copy of
*Midnight Club: Los Angeles Complete Edition* for Xbox 360 (Title ID
`545407F8`). The APK contains the ARM64 program; game data remains outside it.

## Installation without root

1. Install the ARM64 APK produced by `scripts/build_android_apk.sh`.
2. Extract the game files into a folder accessible to Android's document
   picker, such as `Download/MCLA-extracted/`. Select that subfolder rather
   than the `Download` root, which cannot be granted to an app on some Android
   versions.
3. Open **MCLA Recomp**, tap **Select game folder**, and choose the extracted
   folder. Wait for the copy to finish; it needs about 6.2 GB plus free space
   for the source and private copy.
4. After import, tap **Play**. Later launches show the driver controls and
   **Play** before starting the game.

The document picker grants access only to the selected folder; broad storage
permission is not required. Confirm the selection when Android asks. If access
is denied, choose the folder again. On Android 11 or newer, select the
`MCLA-extracted` subfolder instead of the `Download` root.

The 13 files must be directly inside the selected folder: `default.xex`, four
`xarchive_*.rpf` files, six `.bik` videos, `nxeart` and
`root_directory_padding.pad`. The launcher checks presence and nonzero size,
including after an interrupted copy, but does not verify hashes. A closed `.zip`
or `.iso` is not accepted by the picker.

## Direct copy with root

The private destination is:

```text
/data/user/0/org.mcla.recomp/files/game/
```

`/data/data/org.mcla.recomp/files/game/` points to the same location on the
tested device. Install and open the APK once, then copy the **contents** of the
extracted folder into `game/` with read permission for the app UID. A normal
file manager cannot access `/data/user/0` without root, so use the document
picker instead when root is unavailable.

Do not clear app data if you want to keep the imported game, settings and
saves. Updating with `pm install -r` preserves app data. Private logs are in
`files/logs/`; shader caches are under `files/mcla/cache/shaders/`.

On Android 10 and newer, logs are also exported to
`Documents/MCLA/logs/` through MediaStore when the launcher opens, during
gameplay, and when the game pauses or closes. Shared copies use `.txt` names,
while private originals retain `.log` names. The folder is created by Android
without broad storage permission. If the game closes before an export, reopen
the launcher to export the private log. The final filename migration still
needs runtime validation. See [Android shared storage](https://developer.android.com/training/data-storage/shared/media).

The development device had a prepared folder under
`/storage/emulated/0/Download/MCLA-extracted/`; the files matched the local
extraction byte for byte. Existing private-copy detection was tested, but a
clean-profile picker import remains unverified.

## Input support

Single-gamepad input is the only supported and validated in-game input method
at present. Two-gamepad/two-player input is not supported at this time.
Keyboard and mouse input may work through Android/SDL, but this port has not
verified it. Touch controls are planned for a future update and are not
available yet.

## Optional Vulkan driver

The default is the system Vulkan driver. In the launcher, tap **SELECT TURNIP
(.ZIP)**, choose a trusted ARM64 package containing
`libvulkan_freedreno.so`, wait for the import message, and tap **PLAY**. The
driver is copied to `files/gpu-drivers/`. **USE SYSTEM DRIVER** disables the
selected Turnip path without deleting the original ZIP.

The selected ZIP contains native code that runs inside the app. The importer
checks ELF ARM64 format and size limits, but that does not make an untrusted
driver safe. Import packages only from sources you trust.

Turnip compatibility depends on the Adreno GPU, Android version, kernel and
package version. An incompatible combination can cause a black screen, Vulkan
failure or worse performance. If the custom driver fails to open, the loader
falls back to the system driver. Check `files/logs/mcla_*.log` for
`AdrenoTools opened Vulkan for selected Turnip`, the actual `Vulkan device`
line, or `AdrenoTools rejected selected driver`.

The system driver is recommended on the tested Samsung Galaxy S23. Turnip
8G2-OneUI and Standard v27.0.0-20261107 both produced rear-light flashes in
the user's tests; system Vulkan removed the artifact, with somewhat lower
performance. The spoofed `realme/RMX5070` string in old logs is not the phone
model. Thermal throttling was observed, so FPS comparisons require similar
temperature and cache conditions.

## Optional shader pack

The Android build does not ship the precompiled shader pack. If you have
`545407F8.shaderpack` from the same graphics-code version, tap **SELECT SHADER
PACK (.SHADERPACK)** in the launcher and choose the file. It is copied to
`files/shaderpack/545407F8.shaderpack`; the game adds the shaders and pipelines
it lacks at the next start. The pack holds no game data, only fingerprints and
pipeline descriptions, and is optional. A pack made for another game or
version is rejected with a `shader pack:` line in `files/logs/mcla_*.log`.

This import path has not yet been measured on the device. Compare the pipeline
creation burst and the car-load hold with and without the pack, one change at a
time.

## Rebuilding the APK

No build is performed by this documentation page. The source recipe is in
[android-build.md](android-build.md), and CI builds the patched SDK and host
stub without game data. The APK includes AdrenoTools and its hooks, but no
third-party Turnip driver.
