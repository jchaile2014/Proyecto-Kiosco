CREATE TABLE movimiento_caja (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    fecha         DATE          NOT NULL,
    registrado_en DATETIME      NOT NULL,
    tipo          VARCHAR(20)   NOT NULL,
    monto         DECIMAL(12,2) NOT NULL,
    descripcion   VARCHAR(200),
    categoria     VARCHAR(60)
);

CREATE INDEX idx_movimiento_caja_fecha ON movimiento_caja (fecha);

CREATE TABLE cierre_diario (
    fecha         DATE          PRIMARY KEY,
    ventas        DECIMAL(12,2) NOT NULL,
    cobros_fiado  DECIMAL(12,2) NOT NULL,
    pagos_pedidos DECIMAL(12,2) NOT NULL,
    gastos        DECIMAL(12,2) NOT NULL,
    balance       DECIMAL(12,2) NOT NULL,
    cerrado_en    DATETIME      NOT NULL
);
