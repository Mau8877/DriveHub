#!/bin/sh
# Pre-commit de Asistente IA (FastAPI): corre ruff si está instalado.
# Sin ruff en esta máquina, avisa y no bloquea el commit: el job
# `asistente-ia` del CI corre ruff igual.
cd projects/backend/asistente-ia || exit 1
for py in python python3; do
  if command -v "$py" >/dev/null 2>&1 && "$py" -m ruff --version >/dev/null 2>&1; then
    exec "$py" -m ruff check .
  fi
done
echo "AVISO: ruff no está instalado (pip install -r projects/backend/asistente-ia/requirements-dev.txt)."
echo "       Se saltea el lint de Python en este commit; el CI lo valida igual."
