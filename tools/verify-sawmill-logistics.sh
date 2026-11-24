#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
root="build/sawmill-logistics/runs/$(date +%Y%m%d-%H%M%S)"
mkdir -p "$root/world"
printf 'eula=true\n' > "$root/world/eula.txt"
cat > "$root/world/server.properties" <<'PROPS'
online-mode=false
server-ip=127.0.0.1
server-port=25584
max-tick-time=0
view-distance=2
simulation-distance=2
pause-when-empty-seconds=0
PROPS
for phase in write read; do
    timeout --signal=TERM --kill-after=15s 300s bash ./gradlew --no-daemon --max-workers=2 \
        "-PsawmillRunDir=$root" --init-script tests/sawmill-logistics.init.gradle runServer --console=plain \
        > "$root/$phase.log" 2>&1
    grep -q '^PASS:' "$root/result.txt" || { cat "$root/result.txt"; tail -40 "$root/$phase.log"; exit 1; }
    cat "$root/result.txt"
done
test -s "$root/complete.txt"
if grep -Eq ' (net\.minecraft\.client\.|br\.com\.quantumindustries\.client\.)' "$root"/classes-*.log; then
    echo 'FAIL: client classes loaded on dedicated server'; exit 1
fi
cat "$root/complete.txt"
echo 'PASS: dedicated-server class isolation'
