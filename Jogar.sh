#!/usr/bin/env bash
# Launch the regular development client; no preview world or FPS override.
set -euo pipefail
cd -- "$(dirname -- "$(readlink -f -- "$0")")"
project_dir="$PWD"
run_dir="$project_dir/run"
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
# PIDs of the java processes running this project's client.
client_pids() {
    python3 - "$project_dir" <<'PY'
from pathlib import Path
import sys
project = sys.argv[1].encode() + b'/build/classes'
for p in Path('/proc').glob('[0-9]*/cmdline'):
    try:
        args = p.read_bytes().split(b'\0')
        if args and Path(args[0].decode()).name == 'java' and any(project in arg for arg in args):
            print(p.parent.name)
    except (OSError, UnicodeError):
        continue
PY
}

# A client whose log records a finished shutdown ("Stopping!") after its last
# startup ("Sound engine started") never left the process: the JDK 25 render
# thread stalls in teardown and keeps holding the instance lock forever. Such a
# process is reported as open by any naive liveness check, so the launcher must
# identify and reap it before trusting the lock.
client_is_wedged() {
    [ -n "$(client_pids)" ] || return 1
    python3 - "$run_dir/logs/latest.log" <<'PY'
from pathlib import Path
import sys
try:
    text = Path(sys.argv[1]).read_text(errors='replace')
except OSError:
    raise SystemExit(1)
stop = text.rfind('Stopping!')
raise SystemExit(0 if stop > text.rfind('Sound engine started') else 1)
PY
}

if client_is_wedged; then
    echo 'Um cliente anterior ficou preso no encerramento; liberando a instância.' >&2
    kill $(client_pids) 2>/dev/null || true
    for _ in $(seq 1 20); do
        if [ -z "$(client_pids)" ]; then
            break
        fi
        sleep 1
    done
    remaining="$(client_pids)"
    if [ -n "$remaining" ]; then
        kill -9 $remaining 2>/dev/null || true
        sleep 1
    fi
    if [ -n "$(client_pids)" ]; then
        echo 'Não foi possível encerrar o cliente preso; finalize o processo manualmente.' >&2
        exit 1
    fi
fi

# Also detect clients started directly with Gradle before this launcher existed.
if [ -n "$(client_pids)" ]; then
    echo 'O cliente deste projeto já está aberto. Feche-o antes de abrir outra instância.'
    exit 0
fi

mkdir -p build
exec 9>build/client-launch.lock
if ! flock -n 9; then
    echo 'O Minecraft já está sendo iniciado por este atalho.'
    exit 0
fi
./gradlew --max-workers=2 runClient --console=plain
