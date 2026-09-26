package org.kiosco.fiados;

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
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest
@ActiveProfiles("test")
@Import(RelojDePrueba.Config.class)
@Transactional
class FiadoServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 9, 20);

    @Autowired
    FiadoService fiados;

    @Autowired
    CajaService caja;

    @Autowired
    RelojDePrueba reloj;

    @BeforeEach
    void setUp() {
        reloj.fijar(HOY.atTime(10, 0));
    }

    @Test
    void laPrimeraVezQueLlevaSeAgregaComoClienteYDespuesSeLoReconocePorElNombre() {
        MovimientoFiado pan = fiados.anotar("Marta", "Pan", plata("2300"));
        MovimientoFiado leche = fiados.anotar("  marta ", "Leche", plata("1200"));

        assertThat(leche.getClienteId()).isEqualTo(pan.getClienteId());
        assertThat(fiados.cuentas()).singleElement().satisfies(c -> {
            assertThat(c.cliente().getNombre()).isEqualTo("Marta");
            assertThat(c.saldo()).isEqualByComparingTo("3500");
        });
    }

    @Test
    void loQueSeLlevaFiadoNoTocaLaCaja() {
        fiados.anotar("Marta", "Pan", plata("2300"));

        assertThat(caja.movimientosDel(HOY)).isEmpty();
    }

    @Test
    void laLibretaVaSumandoRenglonPorRenglon() {
        Long marta = fiados.anotar("Marta", "Pan y leche", plata("2300")).getClienteId();
        fiados.anotar(marta, "Fiambre", plata("4500"));
        fiados.registrarPago(marta, plata("5000"));
        fiados.anotar(marta, "Cigarrillos", plata("3800"));

        assertThat(fiados.libreta(marta)).extracting(RenglonLibreta::saldo)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(plata("2300"), plata("6800"), plata("1800"), plata("5600"));
        assertThat(fiados.cuenta(marta).saldo()).isEqualByComparingTo("5600");
    }

    @Test
    void unPagoParcialEntraALaCajaDeHoy() {
        Long marta = fiados.anotar("Marta", null, plata("10000")).getClienteId();

        fiados.registrarPago(marta, plata("4000"));

        assertThat(caja.totalesDel(HOY).cobrosFiado()).isEqualByComparingTo("4000");
        assertThat(fiados.cuenta(marta).saldo()).isEqualByComparingTo("6000");
    }

    @Test
    void cuandoYaPagoTodoLaPlataEntraALaCajaYLaCuentaPasaAlHistorial() {
        Long marta = fiados.anotar("Marta", "Pan", plata("2300")).getClienteId();
        fiados.anotar(marta, "Fiambre", plata("4500"));

        BigDecimal pagado = fiados.pagarTodo(marta);

        assertThat(pagado).isEqualByComparingTo("6800");
        assertThat(caja.movimientosDel(HOY))
                .extracting(MovimientoCaja::getTipo, MovimientoCaja::getDetalle)
                .containsExactly(tuple(TipoMovimiento.COBRO_FIADO, "Marta"));
        assertThat(caja.totalesDel(HOY).entradas()).isEqualByComparingTo("6800");
        assertThat(fiados.libreta(marta)).as("la libreta queda limpia").isEmpty();
        assertThat(fiados.cuenta(marta).debe()).isFalse();
        assertThat(fiados.historial(marta)).singleElement().satisfies(cuenta -> {
            assertThat(cuenta.renglones()).hasSize(3);
            assertThat(cuenta.totalPagado()).isEqualByComparingTo("6800");
        });
    }

    @Test
    void despuesDeSaldarLaCuentaLoNuevoEmpiezaDeCero() {
        Long marta = fiados.anotar("Marta", "Pan", plata("2300")).getClienteId();
        fiados.pagarTodo(marta);

        fiados.anotar(marta, "Leche", plata("1200"));

        assertThat(fiados.libreta(marta)).singleElement()
                .satisfies(r -> assertThat(r.saldo()).isEqualByComparingTo("1200"));
    }

    @Test
    void noPuedePagarMasDeLoQueDebe() {
        Long marta = fiados.anotar("Marta", null, plata("1000")).getClienteId();

        assertThatThrownBy(() -> fiados.registrarPago(marta, plata("1500")))
                .isInstanceOf(DatoInvalidoException.class)
                .hasMessage("Debe $ 1.000: el pago no puede ser mayor.");
        assertThat(caja.movimientosDel(HOY)).isEmpty();
    }

    @Test
    void elInteresSeCalculaSobreLoQueDebeAhoraYSeRedondeaAPesos() {
        Long marta = fiados.anotar("Marta", null, plata("12300")).getClienteId();

        MovimientoFiado diez = fiados.aplicarInteres(marta, new BigDecimal("10"));
        MovimientoFiado dosYMedio = fiados.aplicarInteres(marta, new BigDecimal("2.5"));

        assertThat(diez.getMonto()).isEqualByComparingTo("1230");
        assertThat(diez.getDetalle()).isEqualTo("Interés 10 % sobre $ 12.300");
        assertThat(dosYMedio.getMonto()).as("2,5 % de 13.530 = 338,25").isEqualByComparingTo("338");
        assertThat(dosYMedio.getDetalle()).isEqualTo("Interés 2,5 % sobre $ 13.530");
        assertThat(caja.movimientosDel(HOY)).as("el interés no es plata que entra").isEmpty();
    }

    @Test
    void borrarUnPagoAnotadoPorErrorLoSacaTambienDeLaCaja() {
        Long marta = fiados.anotar("Marta", null, plata("10000")).getClienteId();
        MovimientoFiado pago = fiados.registrarPago(marta, plata("4000"));

        fiados.borrarRenglon(marta, pago.getId());

        assertThat(caja.movimientosDel(HOY)).isEmpty();
        assertThat(fiados.cuenta(marta).saldo()).isEqualByComparingTo("10000");
    }

    @Test
    void noSeBorraLoQueLlevoSiYaPagoParteDeEso() {
        MovimientoFiado llevo = fiados.anotar("Marta", null, plata("1000"));
        fiados.registrarPago(llevo.getClienteId(), plata("600"));

        assertThatThrownBy(() -> fiados.borrarRenglon(llevo.getClienteId(), llevo.getId()))
                .isInstanceOf(DatoInvalidoException.class)
                .hasMessageContaining("Borrá primero el pago");
    }

    @Test
    void deshacerElYaPagoReabreLaCuentaYSacaLaPlataDeLaCaja() {
        Long marta = fiados.anotar("Marta", null, plata("5000")).getClienteId();
        fiados.pagarTodo(marta);

        fiados.deshacerUltimaCuentaSaldada(marta);

        assertThat(fiados.cuenta(marta).saldo()).isEqualByComparingTo("5000");
        assertThat(fiados.historial(marta)).isEmpty();
        assertThat(caja.movimientosDel(HOY)).isEmpty();
    }

    @Test
    void elCobroDeUnFiadoNoSePuedeBorrarDesdeLaCaja() {
        Long marta = fiados.anotar("Marta", null, plata("5000")).getClienteId();
        fiados.pagarTodo(marta);
        Long cobro = caja.movimientosDel(HOY).getFirst().getId();

        assertThatThrownBy(() -> caja.eliminar(cobro))
                .isInstanceOf(DatoInvalidoException.class)
                .hasMessageContaining("Fiados");
    }

    @Test
    void noSePuedeSacarDeLaListaAQuienTodaviaDebe() {
        Long marta = fiados.anotar("Marta", null, plata("5000")).getClienteId();

        assertThatThrownBy(() -> fiados.darDeBaja(marta)).isInstanceOf(DatoInvalidoException.class);

        fiados.pagarTodo(marta);
        fiados.darDeBaja(marta);
        assertThat(fiados.cuentas()).isEmpty();
    }

    @Test
    void lasCuentasSeOrdenanPorLoQueDebenYSeSumaElTotal() {
        fiados.anotar("Marta", null, plata("3000"));
        fiados.anotar("Carlos", null, plata("9000"));
        Long lucia = fiados.anotar("Lucía", null, plata("2000")).getClienteId();
        fiados.pagarTodo(lucia);

        assertThat(fiados.cuentas()).extracting(c -> c.cliente().getNombre()).containsExactly("Carlos", "Marta", "Lucía");
        assertThat(fiados.totalEnLaCalle()).isEqualByComparingTo("12000");
    }

    private static BigDecimal plata(String monto) {
        return new BigDecimal(monto);
    }
}
