#!/bin/sh
# Imita a mysqldump en los tests: revisa cómo lo llama la app y escribe un volcado de mentira.
case "$1" in
    --defaults-extra-file=*) ;;
    *) echo "la primera opción tiene que ser --defaults-extra-file" >&2; exit 4 ;;
esac
credenciales=""
resultado=""
base=""
for arg in "$@"; do
    case "$arg" in
        --defaults-extra-file=*) credenciales="${arg#*=}" ;;
        --result-file=*) resultado="${arg#*=}" ;;
        --*) ;;
        *) base="$arg" ;;
    esac
done
grep -q 'password="se\\"creto"' "$credenciales" || { echo "no llegó la contraseña" >&2; exit 3; }
printf -- '-- volcado de prueba de %s\n' "$base" > "$resultado"
