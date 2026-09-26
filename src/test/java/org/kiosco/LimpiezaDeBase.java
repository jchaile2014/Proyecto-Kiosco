package org.kiosco;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/**
 * Los tests de pantallas no corren dentro de una transacción (así se parecen a la app real,
 * donde cada pedido HTTP tiene la suya), de modo que tienen que dejar la base vacía a mano.
 */
public final class LimpiezaDeBase {

    /** En orden, para no violar claves foráneas. */
    private static final List<String> TABLAS =
            List.of("movimiento_fiado", "cliente", "movimiento_caja", "nota_pedido", "pedido", "proveedor", "cierre_diario");

    private LimpiezaDeBase() {
    }

    public static void vaciar(JdbcTemplate jdbc) {
        TABLAS.forEach(tabla -> jdbc.update("delete from " + tabla));
    }
}
