package org.kiosco.pedidos;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kiosco.RelojDePrueba;
import org.kiosco.caja.CajaService;
import org.kiosco.caja.MovimientoCaja;
import org.kiosco.caja.TipoMovimiento;
import org.kiosco.comun.DatoInvalidoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest
@ActiveProfiles("test")
@Import(RelojDePrueba.Config.class)
@Transactional
class PedidoServiceTest {

    /** Domingo. */
    private static final LocalDate HOY = LocalDate.of(2026, 9, 20);

    @Autowired
    PedidoService pedidos;

    @Autowired
    CajaService caja;

    @Autowired
    RelojDePrueba reloj;

    private Proveedor coca;

    @BeforeEach
    void setUp() {
        reloj.fijar(HOY.atTime(10, 0));
        coca = pedidos.guardarProveedor(null, "Coca-Cola", null, EnumSet.of(DayOfWeek.SUNDAY, DayOfWeek.WEDNESDAY));
    }

    @Test
    void armarElPedidoPasaLasNotasAlPedidoYDejaLaListaVacia() {
        pedidos.anotar(coca.getId(), "Sprite x6");
        pedidos.anotar(coca.getId(), "Agua x12");

        Pedido pedido = pedidos.armarPedido(coca.getId(), "Sprite x6\nAgua x12", null, null);

        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.PENDIENTE);
        assertThat(pedido.getFechaPedido()).isEqualTo(HOY);
        assertThat(pedido.getDetalle()).isEqualTo("Sprite x6\nAgua x12");
        assertThat(pedidos.proveedor(coca.getId()).getNotas()).isEmpty();
    }

    @Test
    void cuandoLlegaLaBoletaSaleDeLaCajaDeHoy() {
        Pedido pedido = pedidos.armarPedido(coca.getId(), null, HOY, null);

        pedidos.registrarLlegada(pedido.getId(), new BigDecimal("28400"));

        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.LLEGO);
        assertThat(pedido.getMontoBoleta()).isEqualByComparingTo("28400");
        assertThat(caja.totalesDel(HOY).pagosPedidos()).isEqualByComparingTo("28400");
        assertThat(caja.totalesDel(HOY).balance()).isEqualByComparingTo("-28400");
        assertThat(caja.movimientosDel(HOY))
                .extracting(MovimientoCaja::getTipo, MovimientoCaja::getDetalle)
                .containsExactly(tuple(TipoMovimiento.PAGO_PEDIDO, "Boleta de Coca-Cola"));
    }

    @Test
    void noSePuedeMarcarDosVecesQueLlegoElMismoPedido() {
        Pedido pedido = pedidos.armarPedido(coca.getId(), null, null, null);
        pedidos.registrarLlegada(pedido.getId(), new BigDecimal("100"));

        assertThatThrownBy(() -> pedidos.registrarLlegada(pedido.getId(), new BigDecimal("100")))
                .isInstanceOf(DatoInvalidoException.class);
        assertThat(caja.totalesDel(HOY).pagosPedidos()).isEqualByComparingTo("100");
    }

    @Test
    void deshacerLaLlegadaVuelveAPendienteYBorraElPago() {
        Pedido pedido = pedidos.armarPedido(coca.getId(), null, null, null);
        pedidos.registrarLlegada(pedido.getId(), new BigDecimal("5000"));

        pedidos.deshacerLlegada(pedido.getId());

        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.PENDIENTE);
        assertThat(pedido.getMontoBoleta()).isNull();
        assertThat(caja.movimientosDel(HOY)).isEmpty();
    }

    @Test
    void elPagoDeUnPedidoNoSePuedeBorrarDesdeLaCaja() {
        Pedido pedido = pedidos.armarPedido(coca.getId(), null, null, null);
        pedidos.registrarLlegada(pedido.getId(), new BigDecimal("5000"));
        Long idDelPago = caja.movimientosDel(HOY).getFirst().getId();

        assertThatThrownBy(() -> caja.eliminar(idDelPago))
                .isInstanceOf(DatoInvalidoException.class)
                .hasMessageContaining("Pedidos");
    }

    @Test
    void enLaCajaDeHoyAparecenLosSinFechaLosDeHoyYLosAtrasadosPeroNoLosFuturos() {
        Pedido sinFecha = pedidos.armarPedido(coca.getId(), "sin fecha", null, null);
        Pedido deHoy = pedidos.armarPedido(coca.getId(), "hoy", HOY, null);
        Pedido deManiana = pedidos.armarPedido(coca.getId(), "mañana", HOY.plusDays(1), null);
        reloj.fijar(HOY.plusDays(2).atTime(9, 0));

        assertThat(pedidos.pendientesParaHoy()).containsExactly(deHoy, deManiana, sinFecha);
        assertThat(deHoy.cuandoLlega(HOY.plusDays(2))).isEqualTo("Atrasado 2 días");

        reloj.fijar(HOY.atTime(9, 0));
        assertThat(pedidos.pendientesParaHoy()).containsExactly(deHoy, sinFecha);
        assertThat(pedidos.pendientes()).containsExactly(deHoy, deManiana, sinFecha);
    }

    @Test
    void unPedidoCanceladoDejaDeEstarPendiente() {
        Pedido pedido = pedidos.armarPedido(coca.getId(), null, null, null);

        pedidos.cancelar(pedido.getId());

        assertThat(pedidos.pendientes()).isEmpty();
        assertThatThrownBy(() -> pedidos.registrarLlegada(pedido.getId(), BigDecimal.TEN))
                .isInstanceOf(DatoInvalidoException.class);
    }

    @Test
    void avisaQuePreventistasVienenHoy() {
        pedidos.guardarProveedor(null, "Lácteos", null, EnumSet.of(DayOfWeek.MONDAY));

        assertThat(pedidos.quienesVienenHoy()).extracting(Proveedor::getNombre).containsExactly("Coca-Cola");
    }

    @Test
    void noHayDosProveedoresConElMismoNombre() {
        assertThatThrownBy(() -> pedidos.guardarProveedor(null, "  coca-cola ", null, null))
                .isInstanceOf(DatoInvalidoException.class)
                .hasMessageContaining("Ya tenés");

        Proveedor editado = pedidos.guardarProveedor(coca.getId(), "Coca-Cola", "11 4000-1234", EnumSet.of(DayOfWeek.FRIDAY));
        assertThat(editado.getTelefono()).isEqualTo("11 4000-1234");
        assertThat(editado.getDiasPreventista()).containsExactly(DayOfWeek.FRIDAY);
    }

    @Test
    void laFechaDeLlegadaNoPuedeSerAnteriorAHoy() {
        assertThatThrownBy(() -> pedidos.armarPedido(coca.getId(), null, HOY.minusDays(1), null))
                .isInstanceOf(DatoInvalidoException.class);
    }

    @Test
    void unProveedorDadoDeBajaNoApareceMasEnLaLista() {
        pedidos.darDeBaja(coca.getId());

        assertThat(pedidos.proveedoresConNotas()).isEmpty();
        assertThat(pedidos.quienesVienenHoy()).isEmpty();
    }
}
