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

CONFIG="$HOME/.kiosco/kiosco.properties"

# Traduce el error de arranque que quedó en el log a qué hay que hacer
motivo_del_error() {
    local nuevo
    nuevo="$(tail -c +"$((INICIO_LOG + 1))" "$LOG" 2>/dev/null || true)"
    case "$nuevo" in
        *"using password: NO"*)
            echo "Falta la contraseña de la base: ponela en $CONFIG (mirá el README)." ;;
        *"Access denied"*)
            echo "MySQL rechazó la contraseña: tiene que ser la misma que pusiste en crear-base-mysql.sql. Revisala en $CONFIG." ;;
        *"Unknown database"*)
            echo "No existe la base kiosco: ejecutá scripts/crear-base-mysql.sql en MySQL Workbench." ;;
        *"Communications link failure"*)
            echo "No se pudo conectar con MySQL. ¿Está prendido el servicio de MySQL?" ;;
        *"Port 8080 was already in use"*)
            echo "El puerto 8080 lo está usando otro programa." ;;
        *)
            echo "Kiosco no pudo arrancar. El detalle está en $LOG" ;;
    esac
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
    elif [ -f "$CONFIG" ] && grep -q "CAMBIAR_ESTA_CONTRASEÑA" "$CONFIG"; then
        avisar "Todavía no pusiste la contraseña de la base en $CONFIG."
        exit 1
    fi
    INICIO_LOG="$(stat -c %s "$LOG" 2>/dev/null || echo 0)"
    nohup java -jar "$JAR" "${PERFIL[@]}" >> "$LOG" 2>&1 &
    PID=$!
    until curl -s -o /dev/null "$URL"; do
        if ! kill -0 "$PID" 2>/dev/null; then
            avisar "$(motivo_del_error)"
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
