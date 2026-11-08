#!/data/data/com.termux/files/usr/bin/sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
OUT="$ROOT/out/android-arm64"
SDK="$ROOT/out/android-sdk"
PLATFORM="$SDK/android-35/android.jar"
D8="$SDK/build-tools-35.0.1/android-15/lib/d8.jar"
SDL="$ROOT/third_party/rexglue-sdk/thirdparty/sdl3/android-project/app/src/main/java"
LIBS="$ROOT/out/build/mcla-android-arm64"
RUNTIME="$ROOT/third_party/rexglue-sdk/out/install/android-arm64/lib"
ADRENO="$ROOT/out/build/adrenotools-arm64"

if [ ! -f "$PLATFORM" ] || [ ! -f "$D8" ]; then
    mkdir -p "$SDK"
    python3 - "$SDK" <<'PY'
import pathlib, sys, urllib.request, zipfile
root = pathlib.Path(sys.argv[1])
downloads = [
    ("platform-35.zip", "https://dl.google.com/android/repository/platform-35_r02.zip",
     ("android-35/android.jar", "android-35/core-for-system-modules.jar")),
    ("build-tools-35.zip", "https://dl.google.com/android/repository/build-tools_r35.0.1_linux.zip",
     ("android-15/lib/d8.jar",)),
]
for archive_name, url, members in downloads:
    archive = root / archive_name
    if not archive.exists():
        print(f"Downloading {url}", flush=True)
        urllib.request.urlretrieve(url, archive)
    with zipfile.ZipFile(archive) as zf:
        for member in members:
            zf.extract(member, root)
PY
fi

for tool in javac java aapt2 apksigner zipalign keytool readelf; do
    command -v "$tool" >/dev/null || { echo "Missing Android packaging tool: $tool" >&2; exit 1; }
done
for lib in libmain.so; do
    [ -f "$LIBS/$lib" ] || { echo "Build $lib first" >&2; exit 1; }
done
[ -f "$RUNTIME/librexgpu-xenos.so" ] || { echo "Build and install librexgpu-xenos.so first" >&2; exit 1; }
for lib in "$ADRENO/libadrenotools.so" "$ADRENO/src/hook/libhook_impl.so" \
           "$ADRENO/src/hook/libmain_hook.so" \
           "$ADRENO/src/hook/libfile_redirect_hook.so" \
           "$ADRENO/src/hook/libgsl_alloc_hook.so"; do
    [ -f "$lib" ] || { echo "Build AdrenoTools first: missing $lib" >&2; exit 1; }
done

rm -rf "$OUT/stage"
mkdir -p "$OUT/stage/classes" "$OUT/stage/dex" "$OUT/stage/jni/arm64-v8a" "$OUT/stage/apk"

find "$SDL" -name '*.java' -print > "$OUT/stage/java-sources.txt"
printf '%s\n' "$ROOT/android/MclaActivity.java" >> "$OUT/stage/java-sources.txt"
printf '%s\n' "$ROOT/android/MclaLauncherActivity.java" >> "$OUT/stage/java-sources.txt"
printf '%s\n' "$ROOT/android/MclaLogExporter.java" >> "$OUT/stage/java-sources.txt"
javac -encoding UTF-8 --release 8 -classpath "$PLATFORM" \
    -d "$OUT/stage/classes" @"$OUT/stage/java-sources.txt"
python3 - "$D8" "$PLATFORM" "$OUT/stage/dex" "$OUT/stage/classes" <<'PY'
import pathlib, subprocess, sys
d8, platform, output, classes = sys.argv[1:]
files = sorted(str(path) for path in pathlib.Path(classes).rglob("*.class"))
subprocess.run(["java", "-cp", d8, "com.android.tools.r8.D8", "--release",
                "--min-api", "23", "--lib", platform, "--output", output, *files],
               check=True)
PY

aapt2 link -o "$OUT/stage/apk/base.apk" -I "$PLATFORM" \
    --manifest "$ROOT/android/AndroidManifest.xml" --min-sdk-version 23 --target-sdk-version 35

cp "$LIBS/libmain.so" "$OUT/stage/jni/arm64-v8a/"
cp "$RUNTIME/librexgpu-xenos.so" "$OUT/stage/jni/arm64-v8a/"
cp "$RUNTIME/libSDL3.so" "$RUNTIME/librexruntime.so" "$OUT/stage/jni/arm64-v8a/"
cp "${PREFIX:?Termux PREFIX is required}/lib/libc++_shared.so" "$OUT/stage/jni/arm64-v8a/"
cp "$ADRENO/libadrenotools.so" "$ADRENO/src/hook/"*.so "$OUT/stage/jni/arm64-v8a/"

python3 - "$OUT/stage/jni/arm64-v8a" <<'PY'
import pathlib, subprocess, sys
for library in sorted(pathlib.Path(sys.argv[1]).glob('*.so')):
    header = subprocess.run(['readelf', '-h', str(library)], check=True,
                            capture_output=True, text=True).stdout
    fields = {line.split(':', 1)[0].strip(): line.split(':', 1)[1].strip()
              for line in header.splitlines() if ':' in line}
    if fields.get('Class') != 'ELF64' or fields.get('Machine') != 'AArch64':
        raise SystemExit(f'{library.name}: expected an ELF64 AArch64 library')
    result = subprocess.run(['readelf', '-Wl', str(library)], check=True,
                            capture_output=True, text=True)
    alignments = [int(line.split()[-1], 0) for line in result.stdout.splitlines()
                  if line.lstrip().startswith('LOAD ')]
    if not alignments or min(alignments) < 16384:
        raise SystemExit(f'{library.name}: ELF LOAD segments are not 16 KiB aligned')
    print(f'{library.name}: ELF64 AArch64, LOAD alignment >= 16 KiB')
PY

python3 - "$OUT/stage/apk/base.apk" "$OUT/stage/dex/classes.dex" "$OUT/stage/jni" <<'PY'
import pathlib, sys, zipfile
apk, dex, jni = map(pathlib.Path, sys.argv[1:])
with zipfile.ZipFile(apk, "a", compression=zipfile.ZIP_DEFLATED, compresslevel=6) as zf:
    zf.write(dex, "classes.dex")
    for path in sorted(jni.rglob("*.so")):
        zf.write(path, "lib/arm64-v8a/" + path.name,
                 compress_type=zipfile.ZIP_STORED)
PY

KEYSTORE="$OUT/debug.keystore"
if [ ! -f "$KEYSTORE" ]; then
    keytool -genkeypair -keystore "$KEYSTORE" -storepass android -keypass android \
        -alias mcla-debug -keyalg RSA -keysize 2048 -validity 10000 \
        -dname "CN=MCLA Recomp Debug" >/dev/null 2>&1
fi
zipalign -P 16 -f 4 "$OUT/stage/apk/base.apk" "$OUT/stage/MCLA-aligned.apk"
apksigner sign --ks "$KEYSTORE" --ks-key-alias mcla-debug --ks-pass pass:android \
    --key-pass pass:android --out "$OUT/MCLA-arm64.apk" "$OUT/stage/MCLA-aligned.apk"
apksigner verify "$OUT/MCLA-arm64.apk"
zipalign -c -P 16 4 "$OUT/MCLA-arm64.apk"
echo "APK: $OUT/MCLA-arm64.apk"
DOWNLOADS_DIR=${MCLA_DOWNLOADS_DIR:-/storage/emulated/0/Download}
if [ -d "$DOWNLOADS_DIR" ]; then
    cp "$OUT/MCLA-arm64.apk" "$DOWNLOADS_DIR/MCLA-Recomp-arm64.apk"
    echo "Download APK: $DOWNLOADS_DIR/MCLA-Recomp-arm64.apk"
fi
