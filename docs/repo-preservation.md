# Repository preservation

This repository is maintained as a source and documentation project. At the
handoff commit `36010d0`, it contains 498 tracked files and no tracked
worktree changes. The local `game` and `third_party` links used for development
are outside Git and point to private data.

The `.gitignore` entries for `/game` and `/third_party` deliberately omit the
trailing slash so that the local symlinks are ignored as well as real
directories. A trailing slash matches directories only, which allowed
`git add -A` to stage the symlinks and publish machine-local paths.

## Top-level map

- `.github/`: CI and dependency automation.
- `android/`: Android manifest, launcher, import and log export code.
- `archive/`: inherited fork-base Windows/PS5 material kept as reference.
- `config/`: platform and project configuration.
- `docs/`: public Android build, status, testing, history and handoff docs.
- `generated/`: tracked generation templates; game output stays local.
- `patches/`: reviewed SDK patch series and documentation.
- `scripts/`: build and offline CI checks.
- `src/`: native C++ application and platform integration.
- Root CMake, manifest, metadata, license, notices and issue files: project
  entry points and build metadata.

## Preservation contract

Future changes must keep the existing file tree, path case, line endings,
comments and public language unless the task explicitly changes them. Inspect
the exact diff for every changed file. Do not use destructive Git commands,
whole-tree formatters, mass cleanup or code generation to hide unrelated work.
Keep game data, generated artifacts, APKs, shader packs, native libraries and
signing keys outside Git.

Before and after a task, run:

```sh
git status --short
git diff --check
git diff --stat
python3 -I scripts/ci/check_pins.py
python3 -I scripts/ci/check_markdown_links.py
```

Stage named files only and use one focused English commit per task. The
private handoff contains the full path/mode/blob manifest, local symlink map,
backup location and recovery procedure. Start with `docs/ai-handoff.md` and
request the private handoff when local build data is required.
