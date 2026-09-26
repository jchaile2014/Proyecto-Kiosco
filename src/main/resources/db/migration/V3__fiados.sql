CREATE TABLE cliente (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre   VARCHAR(80) NOT NULL,
    telefono VARCHAR(40),
    activo   BOOLEAN     NOT NULL
);

-- Cada renglón de la libreta: lo que se llevó, los pagos y los intereses.
-- Cuando la cuenta llega a cero se completa saldado_en y los renglones pasan al historial.
CREATE TABLE movimiento_fiado (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id         BIGINT        NOT NULL,
    fecha              DATE          NOT NULL,
    registrado_en      DATETIME      NOT NULL,
    tipo               VARCHAR(20)   NOT NULL,
    descripcion        VARCHAR(200),
    monto              DECIMAL(12,2) NOT NULL,
    movimiento_caja_id BIGINT,
    saldado_en         DATETIME,
    CONSTRAINT fk_movimiento_fiado_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id),
    CONSTRAINT fk_movimiento_fiado_caja FOREIGN KEY (movimiento_caja_id) REFERENCES movimiento_caja (id)
);

CREATE INDEX idx_movimiento_fiado_cliente ON movimiento_fiado (cliente_id, saldado_en);
