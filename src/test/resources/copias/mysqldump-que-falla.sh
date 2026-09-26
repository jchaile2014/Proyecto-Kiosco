#!/bin/sh
# Imita a mysqldump cuando la contraseña está mal.
echo "mysqldump: Got error: 1045: Access denied for user 'kiosco'@'localhost' (using password: YES)" >&2
exit 2
