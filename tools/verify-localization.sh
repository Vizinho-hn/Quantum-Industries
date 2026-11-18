#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-25-openjdk-amd64}"
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-/mnt/dados/cache/quantum-gradle}"
mkdir -p build/localization-verification/world
printf 'eula=true\n' > build/localization-verification/world/eula.txt
cat > build/localization-verification/world/server.properties <<'EOF'
online-mode=false
server-ip=127.0.0.1
server-port=25580
max-tick-time=0
view-distance=2
simulation-distance=2
spawn-protection=0
level-type=minecraft:flat
generator-settings={"layers":[{"block":"minecraft:bedrock","height":1}],"biome":"minecraft:plains"}
EOF
printf 'PENDING\n' > build/localization-verification/result.txt
timeout --signal=TERM --kill-after=15s 300s bash ./gradlew --no-daemon --max-workers=2 \
    --init-script tests/localization.init.gradle runServer --console=plain > build/localization-verification/server.log 2>&1 || {
    tail -60 build/localization-verification/server.log
    exit 1
}
cat build/localization-verification/result.txt
grep -q '^PASS:' build/localization-verification/result.txt
