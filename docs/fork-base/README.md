# Fork-base archive

This directory preserves material inherited from the Windows/PS5 base fork
that is not part of the active Android ARM64 workflow. It remains available for
provenance and comparison; it is not a build input for this repository.

- `FORK.md`, `KNOWN-ISSUES.md` and `fork-changes.md` describe the base fork.
- `windows/` contains Windows installation, build, settings and troubleshooting
  documentation.
- `patches/` and its README contain the inherited desktop/PS5 patch catalogue.
- The sibling `launcher/`, `ps5/`, `ci/` and `workflows/` directories contain
  the inherited Windows launcher, PS5 material, release helpers and Windows
  release workflow.

The active Android patch list is maintained in the repository's
[`patches/README.md`](../../patches/README.md). Archived files are retained for
reference and are not automatically tested or published by Android CI.
