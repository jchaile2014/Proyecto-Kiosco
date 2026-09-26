#!/usr/bin/env bash
# Abre Kiosco en una ventana propia. Si la app no está corriendo, primero la arranca.
#
#   ./kiosco.sh         usa tu base MySQL
#   ./kiosco.sh demo    usa datos de ejemplo, sin MySQL
#
# Sirve tanto desde el repositorio (usa target/kiosco.jar) como instalado con
# scripts/instalar.sh (usa el kiosco.jar que está al lado).
set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
URL="http://localhost:8080"
LOG="$HOME/.kiosco/kiosco.log"

if [ -f "$DIR/kiosco.jar" ]; then
    JAR="$DIR/kiosco.jar"
else
    JAR="$DIR/target/kiosco.jar"
fi

# Abierto desde el ícono no hay terminal: el aviso va como notificación del escritorio
avisar() {
    echo "$1" >&2
    if command -v notify-send >/dev/null; then
        notify-send --icon=dialog-error "Kiosco" "$1"
    fi
}

if [ ! -f "$JAR" ]; then
    avisar "No encuentro $JAR. Compilá primero con: mvn clean package"
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
            avisar "Kiosco no pudo arrancar. ¿Ya creaste la base en MySQL? El detalle está en $LOG"
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
