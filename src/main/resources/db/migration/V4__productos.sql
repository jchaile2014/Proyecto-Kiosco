CREATE TABLE producto (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo         VARCHAR(40)   NOT NULL,
    nombre         VARCHAR(100)  NOT NULL,
    precio         DECIMAL(12,2) NOT NULL,
    controla_stock BOOLEAN       NOT NULL,
    stock          INT           NOT NULL,
    stock_minimo   INT,
    activo         BOOLEAN       NOT NULL
);

CREATE INDEX idx_producto_codigo ON producto (codigo);

-- Cada cambio de stock: ventas, mercadería que entró y correcciones.
-- Si fue una venta, apunta a su movimiento de caja para poder devolver el stock si se borra.
CREATE TABLE movimiento_stock (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    producto_id        BIGINT      NOT NULL,
    registrado_en      DATETIME    NOT NULL,
    cantidad           INT         NOT NULL,
    motivo             VARCHAR(20) NOT NULL,
    movimiento_caja_id BIGINT,
    CONSTRAINT fk_movimiento_stock_producto FOREIGN KEY (producto_id) REFERENCES producto (id),
    CONSTRAINT fk_movimiento_stock_caja FOREIGN KEY (movimiento_caja_id) REFERENCES movimiento_caja (id)
);
