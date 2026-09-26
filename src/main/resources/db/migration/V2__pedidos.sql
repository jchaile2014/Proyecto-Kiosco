CREATE TABLE proveedor (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre           VARCHAR(80) NOT NULL,
    telefono         VARCHAR(40),
    dias_preventista VARCHAR(80) NOT NULL,
    activo           BOOLEAN     NOT NULL
);

-- Lo que hay que acordarse de pedirle a cada proveedor la próxima vez
CREATE TABLE nota_pedido (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    proveedor_id BIGINT       NOT NULL,
    texto        VARCHAR(120) NOT NULL,
    creada_en    DATETIME     NOT NULL,
    CONSTRAINT fk_nota_pedido_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedor (id)
);

CREATE TABLE pedido (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    proveedor_id   BIGINT      NOT NULL,
    fecha_pedido   DATE        NOT NULL,
    detalle        VARCHAR(1000),
    fecha_estimada DATE,
    monto_estimado DECIMAL(12,2),
    estado         VARCHAR(20) NOT NULL,
    fecha_llegada  DATE,
    monto_boleta   DECIMAL(12,2),
    CONSTRAINT fk_pedido_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedor (id)
);

CREATE INDEX idx_pedido_estado ON pedido (estado);

-- El pago de la boleta queda en el libro de caja apuntando a su pedido
ALTER TABLE movimiento_caja ADD COLUMN pedido_id BIGINT;
ALTER TABLE movimiento_caja ADD CONSTRAINT fk_movimiento_caja_pedido FOREIGN KEY (pedido_id) REFERENCES pedido (id);
