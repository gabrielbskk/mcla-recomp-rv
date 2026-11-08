#!/data/data/com.termux/files/usr/bin/sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
DEST="$ROOT/out/logs/android-$(date -u +%Y%m%dT%H%M%SZ)"
mkdir -p "$DEST"

LATEST=$(su -c 'ls -t /data/user/0/org.mcla.recomp/files/logs/mcla_*.log' | head -1)
case "$LATEST" in
    /data/user/0/org.mcla.recomp/files/logs/mcla_*.log) ;;
    *) echo "Log do MCLA não encontrado" >&2; exit 1 ;;
esac
su -c "cat '$LATEST'" > "$DEST/mcla.log"

PID=$(su -c 'pidof org.mcla.recomp' | awk '{print $1}')
case "$PID" in
    ''|*[!0-9]*) su -c 'logcat -d -t 2000' > "$DEST/logcat.txt" ;;
    *) su -c "logcat -d --pid=$PID" > "$DEST/logcat.txt" ;;
esac

su -c 'for name in gpuclk max_gpuclk gpu_busy_percentage; do
    path="/sys/class/kgsl/kgsl-3d0/$name"
    if [ -r "$path" ]; then printf "%s=" "$name"; cat "$path"; fi
done
for zone in /sys/class/thermal/thermal_zone*; do
    case "$(cat "$zone/type" 2>/dev/null)" in
        gpuss-0|gpuss-1|battery) printf "%s=" "$(cat "$zone/type")"; cat "$zone/temp" 2>/dev/null ;;
    esac
done' > "$DEST/device.txt"

PRESENT='/data/user/0/org.mcla.recomp/files/present-profile.csv'
if su -c "test -f '$PRESENT'"; then
    su -c "cat '$PRESENT'" > "$DEST/present-profile.csv"
fi

echo "Logs: $DEST"
