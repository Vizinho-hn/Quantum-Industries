#!/usr/bin/env bash
# Two actual dedicated-server runs; no normal saves are read or modified.
set -euo pipefail
cd "$(dirname "$0")/.."
root="build/mechanical-verification/runs/$(date +%Y%m%d-%H%M%S)-linux"
mkdir -p "$root/world"
printf 'eula=true\n' > "$root/world/eula.txt"
cat > "$root/world/server.properties" <<'EOF'
online-mode=false
server-ip=127.0.0.1
server-port=25578
max-tick-time=0
view-distance=2
simulation-distance=2
spawn-protection=0
gamemode=survival
pause-when-empty-seconds=0
level-type=minecraft:flat
generator-settings={"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}],"biome":"minecraft:plains"}
EOF
for phase in write read; do
    timeout --signal=TERM --kill-after=15s 600s bash ./gradlew --no-daemon --max-workers=2 \
        "-PmechanicalRunDir=$root" --init-script tests/mechanical.init.gradle runServer --console=plain > "$root/$phase.log" 2>&1
    grep -q '^PASS:' "$root/result.txt" || { cat "$root/result.txt"; tail -60 "$root/$phase.log"; exit 1; }
    if [[ "$phase" == write ]]; then test -f "$root/restart-ready.txt"; fi
done
test -f "$root/complete.txt"
cat "$root/restart-ready.txt" "$root/complete.txt"
printf 'Evidence: %s\n' "$root"
