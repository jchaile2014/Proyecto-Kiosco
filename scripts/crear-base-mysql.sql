-- Crea la base y un usuario propio para Kiosco. Se ejecuta UNA sola vez,
-- en MySQL Workbench conectado como root.
--
-- Antes de ejecutarlo, reemplazá CAMBIAR_ESTA_CONTRASEÑA por una contraseña tuya
-- y anotala: después va en ~/.kiosco/kiosco.properties (ver kiosco.properties.ejemplo).
-- Las tablas no hace falta crearlas: la app las crea sola la primera vez que arranca.

CREATE DATABASE IF NOT EXISTS kiosco CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'kiosco'@'localhost' IDENTIFIED BY 'CAMBIAR_ESTA_CONTRASEÑA';

GRANT ALL PRIVILEGES ON kiosco.* TO 'kiosco'@'localhost';
