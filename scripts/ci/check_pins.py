"""Offline repository checks for the Android ARM64 port.

This checker validates the fixed ReXGlue source revision, the ordered Android
patch recipe, workflow pinning, Python syntax, and the absence of game data or
build outputs in Git. It deliberately does not build the SDK, host, or APK.
"""
import hashlib
import pathlib
import re
import subprocess
import sys
import warnings

root = pathlib.Path(sys.argv[1] if len(sys.argv) > 1 else ".").resolve()
errors = []

sdk_commit = "f5337cdc947ff6d4c4196737e2c807a48f2a1fc2"
android_patches = [
    "rexglue-android-arm64.patch",
    "rexglue-turnip-loader.patch",
    "rexglue-driver-cache-path.patch",
    "rexglue-vulkan-pipeline-background-work.patch",
    "rexglue-vulkan-occlusion-query-pipeline-wait.patch",
]

build_doc = (root / "docs" / "android-build.md").read_text(encoding="utf-8", errors="replace")
source_doc = (root / "docs" / "android-sdk-source.md").read_text(encoding="utf-8", errors="replace")
if sdk_commit not in build_doc or sdk_commit not in source_doc:
    errors.append(f"Android docs do not pin ReXGlue SDK commit {sdk_commit}")

apply_lines = re.findall(r"git -C third_party/rexglue-sdk apply \"?\$PATCHES/([^\" ]+\.patch)", build_doc)
if apply_lines != android_patches:
    errors.append(f"docs/android-build.md patch order is {apply_lines!r}, expected {android_patches!r}")

patch_dir = root / "patches"
for name in android_patches:
    path = patch_dir / name
    if not path.is_file():
        errors.append(f"missing Android patch: patches/{name}")
for path in sorted(patch_dir.glob("*.patch")):
    if path.name not in android_patches:
        errors.append(f"non-Android patch remains in active patches/: {path.name}")

known_hashes = {
    "rexglue-android-arm64.patch": "a59701f759c084db1a0b36e5707aacf404cddb43a632f71b2d3e4082bc221299",
    "rexglue-turnip-loader.patch": "57b6794eb912fee5e9c82e2dd96809ca0e4c0c19901633ee6d316007c3a19bf1",
    "rexglue-driver-cache-path.patch": "1e6155c2b617ffc3e82b26472cc5586b20edf3e1773f354a781171aa1a13127e",
    "rexglue-vulkan-pipeline-background-work.patch": "4fd767bc374b994f8baaa11be2cbd189cf9214925a08e9d6f1097f8b5fcf60a2",
    "rexglue-vulkan-occlusion-query-pipeline-wait.patch": "c28b0408e1a926146afad75dd6a2b4f41b5d69be2c3688c0d52e242df385f3b6",
}
for name, expected in known_hashes.items():
    path = patch_dir / name
    if path.is_file() and hashlib.sha256(path.read_bytes()).hexdigest() != expected:
        errors.append(f"patches/{name}: SHA-256 changed; review the patch before updating its pin")

tracked = subprocess.run(
    ["git", "-C", str(root), "ls-files", "-z"], capture_output=True, check=True
).stdout.decode("utf-8").split("\0")
tracked = [path for path in tracked if path]
for path_name in tracked:
    path = root / path_name
    if re.search(r"\.(iso|xex|xexp|rpf|bik|god|apk|keystore|jks|p12|pfx|pem|shaderpack)$", path_name, re.I):
        errors.append(f"{path_name} is tracked: game data, build output, or signing material")
    if path_name.startswith(("game/", "out/", "third_party/", "generated/default/", "shaderpack/")):
        errors.append(f"{path_name} is tracked: generated data or game data")
    if path.is_file() and path.stat().st_size > 8_000_000 and path_name != "patches/rexglue-android-arm64.patch":
        errors.append(f"{path_name} is {path.stat().st_size / 1e6:.1f} MB: review large tracked files")

for workflow in sorted((root / ".github" / "workflows").glob("*.yml")):
    text = workflow.read_text(encoding="utf-8")
    for match in re.finditer(r"^\s*-?\s*uses:\s*(\S+)", text, re.M):
        reference = match.group(1)
        if not reference.startswith("./") and not re.fullmatch(
            r"[A-Za-z0-9_.-]+/[A-Za-z0-9_./-]+@[0-9a-f]{40}", reference
        ):
            errors.append(f"{workflow.name}: {reference} is not pinned by a full commit SHA")
    if re.search(r"^\s*pull_request_target\s*:", text, re.M):
        errors.append(f"{workflow.name}: pull_request_target is not allowed")
    if not re.search(r"^permissions:\s*\n\s+contents:\s*read\s*$", text, re.M):
        errors.append(f"{workflow.name}: top-level permissions are not contents: read")
    if re.search(r"secrets\.(?!GITHUB_TOKEN)", text):
        errors.append(f"{workflow.name}: uses a secret other than GITHUB_TOKEN")

checked_python = 0
for path_name in tracked:
    if not path_name.endswith(".py"):
        continue
    checked_python += 1
    try:
        with warnings.catch_warnings():
            warnings.simplefilter("error")
            compile((root / path_name).read_text(encoding="utf-8"), path_name, "exec")
    except (SyntaxError, Warning) as error:
        errors.append(f"{path_name}: {error}")

for error in errors:
    print("ERROR:", error)
print(
    f"check_pins: {checked_python} Python sources compile; "
    f"{len(android_patches)} Android patches checked; {len(tracked)} tracked files checked; "
    f"{len(errors)} problem(s)"
)
sys.exit(1 if errors else 0)
