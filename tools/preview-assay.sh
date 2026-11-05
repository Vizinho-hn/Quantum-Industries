#!/usr/bin/env bash
# Capture the assay interface in the isolated preview world with bounded load.
set -euo pipefail
cd "$(dirname "$0")/.."
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-25-openjdk-amd64}"
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-/mnt/dados/cache/quantum-gradle}"
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -XX:ActiveProcessorCount=2"
export LP_NUM_THREADS=2
mkdir -p build/assay-preview
if [[ ! -f build/assay-preview/saves/AssayQA/level.dat ]]; then
    if [[ ! -f build/assay-verification/world/world/level.dat ]]; then
        echo 'Execute tools/verify-assay.sh uma vez para criar o mundo descartável.' >&2
        exit 1
    fi
    mkdir -p build/assay-preview/saves/AssayQA
    cp -a build/assay-verification/world/world/. build/assay-preview/saves/AssayQA/
fi
python3 - <<'PY'
from pathlib import Path

path = Path('build/assay-preview/options.txt')
options = dict(line.split(':', 1) for line in path.read_text().splitlines() if ':' in line) if path.exists() else {}
options.update({
    'renderDistance': '2', 'simulationDistance': '5', 'maxFps': '30',
    'enableVsync': 'false', 'guiScale': '2', 'mipmapLevels': '0',
    'particles': '0', 'entityShadows': 'false', 'renderClouds': '"false"',
    'menuBackgroundBlurriness': '0', 'soundCategory_master': '0.0',
    'onboardAccessibility': 'false', 'lang': 'pt_br', 'tutorialStep': 'none', 'pauseOnLostFocus': 'false',
})
path.write_text(''.join(f'{key}:{value}\n' for key, value in options.items()))
PY
preview_started=$(date +%s)
timeout --signal=TERM --kill-after=15s 240s xvfb-run -a -s '-screen 0 1100x760x24' \
    ./gradlew --offline --max-workers=2 --init-script tests/assay-preview.init.gradle runClient
python3 - "$preview_started" <<'PY'
from pathlib import Path
import sys

started = int(sys.argv[1])
for name in [f'v2-{i}-{kind}.png' for i in range(4) for kind in ('hud','world')] + ['v2-complete.png']:
    path = Path('build/assay-preview/screenshots') / name
    if not path.is_file() or path.stat().st_mtime < started:
        raise SystemExit(f'Captura ausente ou antiga: {path}')
print('PASS: nove capturas novas; conferir visualmente os estados da interface.')
PY
