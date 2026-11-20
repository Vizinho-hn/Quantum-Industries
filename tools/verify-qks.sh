#!/usr/bin/env bash
# Two fresh dedicated-server JVMs against one isolated world. Requires JDK 25.
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/qks-verification/runs
root=$(mktemp -d -p "$(realpath build/qks-verification/runs)" qks-XXXXXXXX)
phase=setup
fail() {
    status=$?
    if (( status != 0 )); then
        printf 'FAIL: QKS phase=%s (exit %s)\nEvidence: %s\n' "$phase" "$status" "$root" >&2
        if [[ -f "$root/result.txt" ]]; then cat "$root/result.txt" >&2; fi
        if [[ -f "$root/$phase.log" ]]; then tail -60 "$root/$phase.log" >&2; fi
    fi
}
trap fail EXIT
mkdir -p "$root/world"
printf 'eula=true\n' > "$root/world/eula.txt"
cat > "$root/world/server.properties" <<'PROPERTIES'
online-mode=false
server-ip=127.0.0.1
server-port=25583
max-tick-time=0
view-distance=2
simulation-distance=2
spawn-protection=0
pause-when-empty-seconds=0
level-type=minecraft:flat
generator-settings={"layers":[{"block":"minecraft:bedrock","height":1}],"biome":"minecraft:plains"}
PROPERTIES
for phase in write read; do
    # Remove stale success evidence before starting the next JVM.
    rm -f "$root/result.txt"
    setsid --wait timeout --signal=TERM --kill-after=15s 600s bash ./gradlew --no-daemon --max-workers=2 \
        "-PqksRunDir=$root" "-PqksPhase=$phase" --init-script tests/qks.init.gradle runServer --console=plain \
        > "$root/$phase.log" 2>&1
    grep -q "^PASS: $phase " "$root/result.txt"
    if [[ "$phase" == write ]]; then test -s "$root/restart-ready.txt"; fi
done
test -s "$root/complete.txt"
shopt -s nullglob
class_logs=("$root"/classes-*.log)
(( ${#class_logs[@]} == 2 ))
if grep -E '\[class,load\] (net\.minecraft\.client\.|br\.com\.quantumindustries\.client\.)' "${class_logs[@]}"; then
    printf 'FAIL: client class loaded on dedicated server\n' >&2
    exit 1
fi
cat "$root/result.txt" "$root/complete.txt"
printf 'PASS: dedicated-server class isolation (both JVMs)\nEvidence: %s\n' "$root"
