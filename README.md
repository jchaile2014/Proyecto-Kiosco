# Kiosco

Sistema de gestión para un kiosco/almacén de barrio, hecho en **Java 21** con interfaz de escritorio **Swing** y persistencia con **JPA/Hibernate** sobre **PostgreSQL**.

Permite administrar el stock de productos, clientes, repartidores, compras a proveedores, ventas y cuentas fiadas (crédito a clientes), llevando el control de stock de forma automática en cada operación.

## Funcionalidades

- **Productos**: alta, búsqueda por código, listado y baja lógica (no se borran físicamente, se marcan como inactivos).
- **Clientes** y **Repartidores**: alta, listado y baja lógica.
- **Compras**: registro de compras a proveedores, que suman stock automáticamente al producto correspondiente.
- **Ventas**: registro de ventas con control de stock disponible, que descuentan stock automáticamente.
- **Fiado**: registro de cuentas a crédito por cliente, con cálculo del total adeudado.

## Stack técnico

| Capa            | Tecnología                          |
|-----------------|--------------------------------------|
| UI              | Java Swing                          |
| Persistencia    | Jakarta Persistence (JPA) + Hibernate 6 |
| Base de datos   | PostgreSQL                          |
| Build           | Maven                               |

## Arquitectura

El proyecto sigue una separación simple en tres capas:

```
src/main/java/org/kiosco/
├── igu/            # Ventanas Swing (formularios y menú principal)
├── logica/         # Entidades JPA (Producto, Cliente, Venta, Compra, Fiado, ...)
└── persistencia/    # DAOs y manejo de la conexión (EntityManager)
```

## Puesta en marcha

### Requisitos

- JDK 21+
- Maven 3.8+
- PostgreSQL corriendo localmente (o accesible por red)

### 1. Crear la base de datos

```bash
createdb kiosco
```

Hibernate crea/actualiza las tablas automáticamente (`hibernate.hbm2ddl.auto=update`) al levantar la aplicación.

### 2. Configurar la conexión

La configuración por defecto (`src/main/resources/META-INF/persistence.xml`) apunta a `localhost:5432/kiosco` con usuario `postgres` y sin contraseña. Para no versionar credenciales, las siguientes variables de entorno sobreescriben esos valores si están definidas:

| Variable       | Ejemplo                                    |
|----------------|---------------------------------------------|
| `DB_URL`       | `jdbc:postgresql://localhost:5432/kiosco`   |
| `DB_USER`      | `postgres`                                  |
| `DB_PASSWORD`  | `mi_password`                               |

```bash
export DB_URL=jdbc:postgresql://localhost:5432/kiosco
export DB_USER=postgres
export DB_PASSWORD=mi_password
```

### 3. Compilar y ejecutar

```bash
mvn clean package
java -jar target/kiosco-1.0-SNAPSHOT.jar
```

O directamente desde el IDE, ejecutando `org.kiosco.Main`.

## Posibles mejoras futuras

- Tests automatizados (unitarios para DAOs con una base en memoria/testcontainers).
- Reportes de ventas/compras por período.
- Autenticación de usuarios.
- Migrar la UI a JavaFX o a una API REST + frontend web.
