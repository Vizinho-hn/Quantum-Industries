#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-25-openjdk-amd64}"
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-/mnt/dados/cache/quantum-gradle}"
mkdir -p build/cable-verification/world
printf 'eula=true\n' > build/cable-verification/world/eula.txt
cat > build/cable-verification/world/server.properties <<'EOF'
online-mode=false
server-ip=127.0.0.1
server-port=25579
max-tick-time=0
view-distance=2
simulation-distance=2
spawn-protection=0
level-type=minecraft:flat
generator-settings={"layers":[{"block":"minecraft:bedrock","height":1}],"biome":"minecraft:plains"}
EOF
printf 'PENDING\n' > build/cable-verification/result.txt
timeout --signal=TERM --kill-after=15s 300s bash ./gradlew --no-daemon --max-workers=2 \
    --init-script tests/cable.init.gradle runServer --console=plain > build/cable-verification/server.log 2>&1 || {
    tail -60 build/cable-verification/server.log
    exit 1
}
cat build/cable-verification/result.txt
grep -q '^PASS:' build/cable-verification/result.txt
