#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
echo "Quantum Industries | Atualizar e Jogar"
if ! command -v git >/dev/null; then
  echo "Erro: instale o Git." >&2
  exit 1
fi
current="$(git branch --show-current)"
if [[ "$current" != "main" && "$current" != "feature/v0.1.1-conductivity-research" ]]; then
  echo "Branch atual: $current. Execute: git switch main" >&2
  exit 1
fi
git pull --ff-only
echo "Iniciando Minecraft no ambiente de desenvolvimento..."
bash ./gradlew runClient
