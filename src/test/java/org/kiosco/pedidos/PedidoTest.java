package org.kiosco.pedidos;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PedidoTest {

    /** Sábado. */
    private static final LocalDate HOY = LocalDate.of(2026, 9, 26);

    @ParameterizedTest(name = "a {0} días → \"{1}\"")
    @CsvSource({
            "0,   Llega hoy",
            "1,   Llega mañana",
            "3,   Llega el martes",
            "10,  Llega el 06/10",
            "-1,  Atrasado 1 día",
            "-4,  Atrasado 4 días",
    })
    void diceCuandoLlegaEnCastellano(int dias, String esperado) {
        Pedido pedido = new Pedido(null, HOY, null, HOY.plusDays(dias), null);
        assertThat(pedido.cuandoLlega(HOY)).isEqualTo(esperado);
    }

    @Test
    void sinFechaSeMuestraComoTal() {
        Pedido pedido = new Pedido(null, HOY, null, null, null);
        assertThat(pedido.cuandoLlega(HOY)).isEqualTo("Sin fecha");
        assertThat(pedido.esParaHoy(HOY)).isTrue();
        assertThat(pedido.atrasado(HOY)).isFalse();
    }
}
