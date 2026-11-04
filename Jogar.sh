#!/usr/bin/env bash
# Launch the regular development client; no preview world or FPS override.
set -euo pipefail
cd -- "$(dirname -- "$(readlink -f -- "$0")")"
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-25-openjdk-amd64}"
if [[ ! -x "$JAVA_HOME/bin/java" ]]; then
    echo 'JDK 25 não encontrado. Configure JAVA_HOME para sua instalação do Java 25.' >&2
    exit 1
fi
if ! "$JAVA_HOME/bin/java" -version 2>&1 | head -1 | grep -Eq 'version "25([.\"]|$)'; then
    echo 'Este projeto precisa do JDK 25. Confira JAVA_HOME.' >&2
    exit 1
fi
if [[ -z "${GRADLE_USER_HOME:-}" ]]; then
    if [[ -d /mnt/dados ]]; then
        export GRADLE_USER_HOME=/mnt/dados/cache/quantum-gradle
    else
        export GRADLE_USER_HOME="$PWD/.local/gradle"
    fi
fi
if [[ "${1:-}" == --check ]]; then
    [[ -x ./gradlew ]]
    printf 'Pronto: Java 25, Gradle Wrapper e cache em %s\n' "$GRADLE_USER_HOME"
    exit 0
fi
mkdir -p build
exec 9>build/client-launch.lock
if ! flock -n 9; then
    echo 'O Minecraft já está sendo iniciado por este atalho.'
    exit 0
fi
# Also detect clients started directly with Gradle before this launcher existed.
if python3 - "$PWD" <<'PY'
from pathlib import Path
import sys
project = sys.argv[1].encode() + b'/build/classes'
for p in Path('/proc').glob('[0-9]*/cmdline'):
    try:
        args = p.read_bytes().split(b'\0')
        if args and Path(args[0].decode()).name == 'java' and any(project in arg for arg in args):
            raise SystemExit(0)
    except (OSError, UnicodeError):
        continue
raise SystemExit(1)
PY
then
    echo 'O cliente deste projeto já está aberto. Feche-o antes de abrir outra instância.'
    exit 0
fi
./gradlew --max-workers=2 runClient --console=plain
