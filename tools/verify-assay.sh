#!/usr/bin/env bash
# Dedicated-server integration tests in an isolated disposable world.
set -euo pipefail
cd "$(dirname "$0")/.."
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-25-openjdk-amd64}"
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-/mnt/dados/cache/quantum-gradle}"
mkdir -p build/assay-verification/world
printf 'eula=true\n' > build/assay-verification/world/eula.txt
printf 'level-type=minecraft:flat\ngenerate-structures=false\nonline-mode=false\nserver-port=25576\nmax-tick-time=0\nview-distance=2\nsimulation-distance=2\n' > build/assay-verification/world/server.properties
rm -f build/assay-verification/result.txt
./gradlew --init-script tests/assay.init.gradle runServer
# Server shutdown can succeed even if the assertion failed; require explicit PASS.
python3 - <<'PY'
from pathlib import Path
result = Path('build/assay-verification/result.txt').read_text()
print(result, end='')
if not result.startswith('PASS:'):
    raise SystemExit(1)
PY
