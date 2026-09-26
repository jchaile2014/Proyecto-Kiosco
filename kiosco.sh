#!/usr/bin/env bash
# Abre Kiosco en una ventana propia. Si la app no está corriendo, primero la arranca.
#
#   ./kiosco.sh         usa tu base MySQL
#   ./kiosco.sh demo    usa datos de ejemplo, sin MySQL
set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
JAR="$DIR/target/kiosco.jar"
URL="http://localhost:8080"
LOG="$HOME/.kiosco/kiosco.log"

if [ ! -f "$JAR" ]; then
    echo "No encuentro $JAR. Compilá primero con: mvn clean package" >&2
    exit 1
fi

if ! curl -s -o /dev/null "$URL"; then
    mkdir -p "$HOME/.kiosco"
    PERFIL=()
    if [ "${1:-}" = "demo" ]; then
        PERFIL=(--spring.profiles.active=demo)
    fi
    nohup java -jar "$JAR" "${PERFIL[@]}" >> "$LOG" 2>&1 &
    PID=$!
    until curl -s -o /dev/null "$URL"; do
        if ! kill -0 "$PID" 2>/dev/null; then
            echo "Kiosco no pudo arrancar. Mirá el detalle en $LOG" >&2
            exit 1
        fi
        sleep 1
    done
fi

if command -v google-chrome >/dev/null; then
    google-chrome --app="$URL" >/dev/null 2>&1 &
else
    xdg-open "$URL"
fi
