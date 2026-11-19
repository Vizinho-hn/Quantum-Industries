#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
root="build/hydraulic-dynamo/runs/$(date +%Y%m%d-%H%M%S)"
mkdir -p "$root/world"
printf 'eula=true\n' > "$root/world/eula.txt"
cat > "$root/world/server.properties" <<'EOF'
online-mode=false
server-ip=127.0.0.1
server-port=25579
max-tick-time=0
view-distance=2
simulation-distance=2
pause-when-empty-seconds=0
EOF
for phase in write read; do
    timeout --signal=TERM --kill-after=15s 300s bash ./gradlew --no-daemon --max-workers=2 \
        "-PhydraulicRunDir=$root" --init-script tests/hydraulic-dynamo.init.gradle runServer --console=plain > "$root/$phase.log" 2>&1
    grep -q '^PASS:' "$root/result.txt" || { cat "$root/result.txt"; tail -50 "$root/$phase.log"; exit 1; }
    cat "$root/result.txt"
done
test -f "$root/complete.txt"
cat "$root/complete.txt"
