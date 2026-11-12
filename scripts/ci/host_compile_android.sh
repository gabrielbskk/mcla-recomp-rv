#!/bin/sh
set -eu

# Configure the host sources against an installed Android SDK and compile them
# with syntax-only commands using generated-header stand-ins. This does not
# invoke ReXGlue codegen and does not require the game files.
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
SDK_PREFIX=${1:?usage: host_compile_android.sh <installed SDK prefix> [work directory]}
WORK=${2:-${RUNNER_TEMP:-/tmp}/mcla-host-check}
BUILD="$WORK/build"

rm -rf "$WORK"
mkdir -p "$WORK"
python3 -I "$ROOT/scripts/ci/host_stub.py" "$ROOT/src" "$WORK"
if [ -n "${MCLA_HOST_CHECK_NDK:-}" ]; then
    # Check against the same Android libc++ headers as the patched SDK, not
    # the runner's host libstdc++ (which has different chrono declarations).
    set -- \
        "-DCMAKE_TOOLCHAIN_FILE=$MCLA_HOST_CHECK_NDK/build/cmake/android.toolchain.cmake" \
        -DANDROID_ABI=arm64-v8a \
        -DANDROID_PLATFORM=android-35
else
    set --
fi
cmake -S "$ROOT" -B "$BUILD" -G Ninja \
    -DCMAKE_BUILD_TYPE=Release \
    -DCMAKE_PREFIX_PATH="$SDK_PREFIX" \
    -DCMAKE_EXPORT_COMPILE_COMMANDS=ON \
    -DCMAKE_CXX_FLAGS="-I$WORK" \
    "$@"
python3 -I "$ROOT/scripts/ci/host_check.py" "$BUILD" -j "$(getconf _NPROCESSORS_ONLN 2>/dev/null || echo 2)"
