#!/data/data/com.termux/files/usr/bin/sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
SOURCE="$ROOT/third_party/libadrenotools"
BUILD="$ROOT/out/build/adrenotools-arm64"
REVISION=8fae8ce254dfc1344527e05301e43f37dea2df80
SUBMODULE_REVISION=aa3975893d83ef1bc84c321ec60c65fbf1287887

if [ ! -d "$SOURCE/.git" ]; then
    git clone https://github.com/bylaws/libadrenotools.git "$SOURCE"
    git -C "$SOURCE" checkout "$REVISION"
fi
if [ "$(git -C "$SOURCE" rev-parse HEAD)" != "$REVISION" ]; then
    echo "Expected libadrenotools commit $REVISION in $SOURCE" >&2
    exit 1
fi
git -C "$SOURCE" submodule update --init --recursive
if [ "$(git -C "$SOURCE/lib/linkernsbypass" rev-parse HEAD)" != "$SUBMODULE_REVISION" ]; then
    echo "Unexpected liblinkernsbypass revision" >&2
    exit 1
fi

cmake -S "$SOURCE" -B "$BUILD" -G Ninja \
    -DCMAKE_ANDROID_ARCH_ABI=arm64-v8a \
    -DCMAKE_BUILD_TYPE=Release -DBUILD_SHARED_LIBS=ON \
    -DCMAKE_POSITION_INDEPENDENT_CODE=ON \
    -DCMAKE_SHARED_LINKER_FLAGS=-Wl,-z,max-page-size=16384
cmake --build "$BUILD" -j 4
