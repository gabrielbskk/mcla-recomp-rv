#!/usr/bin/env python3
"""Host policy tests; not an Android hardware input test."""
import argparse
from pathlib import Path
import os
import re
import subprocess
import tempfile


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--android-jar", required=True, type=Path)
    parser.add_argument("--sdk-source", type=Path)
    args = parser.parse_args()
    jar = args.android_jar.resolve(strict=True)
    root = Path(__file__).resolve().parents[2]
    with tempfile.TemporaryDirectory(prefix="mcla-input-policy-") as output:
        subprocess.run([
            "javac", "-encoding", "UTF-8", "--release", "8", "-classpath", str(jar),
            "-d", output, str(root / "android/MclaKeyboardMouse.java"),
            str(root / "scripts/ci/android-input/KeyboardMouseTest.java"),
        ], check=True)
        result = subprocess.check_output([
            "java", "-cp", os.pathsep.join([output, str(jar)]),
            "org.mcla.recomp.KeyboardMouseTest",
        ], text=True)
    print(result, end="")
    if args.sdk_source:
        sdk = args.sdk_source.resolve(strict=True)
        driver = (sdk / "src/input/mnk/mnk_input_driver.cpp").read_text()
        keys = (sdk / "src/ui/keybinds.cpp").read_text()
        for line in result.splitlines():
            if not line.startswith("BIND "):
                continue
            key, value = line[5:].split("=", 1)
            cvar = key[4:].lower()
            if not re.search(r"REXCVAR_DEFINE_\w+\(\s*" + re.escape(cvar) + r"\s*,", driver):
                raise RuntimeError("SDK cvar missing: " + cvar)
            if cvar.startswith("keybind_"):
                for token in filter(None, value.split(",")):
                    if '"' + token + '"' not in keys:
                        raise RuntimeError("SDK key name missing: " + token)
        print("PASS preset cvars and key names exist in SDK")


if __name__ == "__main__":
    main()
