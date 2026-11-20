#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
root="build/hydraulic-inspection/runs/$(date +%Y%m%d-%H%M%S)"
mkdir -p "$root/world"
printf 'eula=true\n' > "$root/world/eula.txt"
cat > "$root/world/server.properties" <<'PROPS'
online-mode=false
server-ip=127.0.0.1
server-port=25582
max-tick-time=0
view-distance=2
simulation-distance=2
pause-when-empty-seconds=0
PROPS
timeout --signal=TERM --kill-after=15s 300s bash ./gradlew --no-daemon --max-workers=2 \
    "-PhydraulicInspectionRunDir=$root" --init-script tests/hydraulic-inspection.init.gradle \
    runServer --console=plain > "$root/server.log" 2>&1
if ! grep -q '^PASS:' "$root/result.txt"; then
    cat "$root/result.txt"; tail -40 "$root/server.log"; exit 1
fi
if grep -Eq ' (net\.minecraft\.client\.|br\.com\.quantumindustries\.client\.)' "$root"/classes-*.log; then
    echo 'FAIL: client classes loaded on dedicated server'; exit 1
fi
cat "$root/result.txt"
echo 'PASS: dedicated-server client-class isolation'
