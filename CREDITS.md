# Credits and provenance

MCLA Recomp RV is an experimental Android ARM64 continuation of existing open-source work. The maintainer used AI assistance while developing and documenting this port. These credits identify the projects and their roles; they do not imply endorsement by their authors.

- [holdmysocks/mcla-recomp](https://github.com/holdmysocks/mcla-recomp): original static recompilation project and game-specific host code.
- [RenanQueiroz/mcla-recomp](https://github.com/RenanQueiroz/mcla-recomp): the fork used as the direct source base, including extensive graphics, performance, game-fix and launcher work. The base snapshot was commit `f656d24dfac102cc41942a457d885aedd5d3db1b`.
- [ReXGlue SDK](https://github.com/rexglue/rexglue-sdk) and its contributors: static recompilation runtime and Xbox 360 graphics/audio compatibility layer. The Android patch is based on SDK commit `f5337cdc947ff6d4c4196737e2c807a48f2a1fc2`.
- [SDL3](https://github.com/libsdl-org/SDL): Android activity, window, input and audio integration used by the port.
- [AdrenoTools](https://github.com/bylaws/libadrenotools) and `liblinkernsbypass`: optional loading of a user-selected Vulkan driver. Their BSD-2-Clause notice is included in [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md). No Turnip driver is part of the source repository or APK.
- [Black-Android](https://github.com/RanggaCasper/Black-Android): design reference for an Android native port. Its renderer was not copied into this project.
- [Def-Jam-Android-Native-Port](https://github.com/contatg/Def-Jam-Android-Native-Port): public documentation reference. Its private implementation was not used.
- Other authors and projects whose work is included in the base fork or patches are credited in [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md), source headers, and the base documentation.

The Android changes and investigation are recorded in [docs/android-handoff.md](docs/android-handoff.md) and [ANDROID-PORT-NOTES.md](ANDROID-PORT-NOTES.md). The repository is distributed under the existing [GPL-3.0-or-later license](LICENSE), with the separate third-party notices kept intact. Game data and original game rights belong to their respective owners; this project is not affiliated with them.
