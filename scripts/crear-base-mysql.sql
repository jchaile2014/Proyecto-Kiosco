-- Crea la base y el usuario de Kiosco. Se ejecuta en MySQL Workbench conectado como root.
--
-- Antes de ejecutarlo, reemplazá CAMBIAR_ESTA_CONTRASEÑA por una contraseña tuya (sin comillas)
-- y poné esa misma en ~/.kiosco/kiosco.properties. Ejecutalo sin guardar el archivo, así tu
-- contraseña no queda escrita en la carpeta del proyecto.
--
-- Se puede volver a ejecutar cuando quieras: sirve también para cambiar la contraseña si te la
-- olvidaste. Los datos no se tocan: están en la base, no en el usuario.
-- Las tablas no hace falta crearlas: la app las crea sola la primera vez que arranca.

CREATE DATABASE IF NOT EXISTS kiosco CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

DROP USER IF EXISTS 'kiosco'@'localhost';
CREATE USER 'kiosco'@'localhost' IDENTIFIED BY 'CAMBIAR_ESTA_CONTRASEÑA';

GRANT ALL PRIVILEGES ON kiosco.* TO 'kiosco'@'localhost';
