#!/usr/bin/env bash
# Instala Kiosco para tu usuario: copia la app a ~/.kiosco/app y crea el ícono en el
# escritorio y en el menú de aplicaciones. Para actualizar a una versión nueva, compilá y
# volvé a correrlo: tus datos (en MySQL) y tus copias de seguridad no se tocan.
#
#   mvn clean package
#   ./scripts/instalar.sh
set -euo pipefail

REPO="$(cd "$(dirname "$0")/.." && pwd)"
DESTINO="$HOME/.kiosco/app"

if [ ! -f "$REPO/target/kiosco.jar" ]; then
    echo "Primero compilá la app con: mvn clean package" >&2
    exit 1
fi

# Si hay una versión anterior abierta, se cierra para que la próxima vez arranque la nueva
if pgrep -f "^java .*$DESTINO/kiosco.jar" >/dev/null; then
    echo "Cerrando la versión que estaba abierta…"
    pkill -f "^java .*$DESTINO/kiosco.jar" || true
    sleep 2
fi

mkdir -p "$DESTINO"
cp "$REPO/target/kiosco.jar" "$DESTINO/kiosco.jar"
cp "$REPO/kiosco.sh" "$DESTINO/kiosco.sh"
cp "$REPO/src/main/resources/static/img/icono.svg" "$DESTINO/icono.svg"
chmod +x "$DESTINO/kiosco.sh"

ENTRADA="[Desktop Entry]
Type=Application
Name=Kiosco
Comment=Caja, pedidos y fiados del kiosco
Exec=\"$DESTINO/kiosco.sh\"
Icon=$DESTINO/icono.svg
Terminal=false
Categories=Office;Finance;"

MENU="$HOME/.local/share/applications"
mkdir -p "$MENU"
printf '%s\n' "$ENTRADA" > "$MENU/kiosco.desktop"
echo "Agregado al menú de aplicaciones."

ESCRITORIO="$(xdg-user-dir DESKTOP 2>/dev/null || true)"
if [ -n "$ESCRITORIO" ] && [ -d "$ESCRITORIO" ] && [ "$ESCRITORIO" != "$HOME" ]; then
    printf '%s\n' "$ENTRADA" > "$ESCRITORIO/kiosco.desktop"
    chmod +x "$ESCRITORIO/kiosco.desktop"
    # GNOME pide marcar el acceso como confiable para poder abrirlo con doble clic
    gio set "$ESCRITORIO/kiosco.desktop" metadata::trusted true 2>/dev/null || true
    echo "Ícono creado en $ESCRITORIO."
fi

echo "Listo: abrí Kiosco desde el ícono."
if [ ! -f "$HOME/.kiosco/kiosco.properties" ] && [ -z "${DB_PASSWORD:-}" ]; then
    echo "Ojo: todavía no configuraste la base. Mirá la sección \"Usarla con MySQL\" del README." >&2
fi
