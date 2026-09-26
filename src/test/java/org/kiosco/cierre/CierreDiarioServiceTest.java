package org.kiosco.cierre;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kiosco.RelojDePrueba;
import org.kiosco.caja.CajaService;
import org.kiosco.caja.MovimientoCaja;
import org.kiosco.comun.DatoInvalidoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Import(RelojDePrueba.Config.class)
@Transactional
class CierreDiarioServiceTest {

    private static final LocalDate DIA_20 = LocalDate.of(2026, 9, 20);
    private static final LocalDate DIA_21 = LocalDate.of(2026, 9, 21);
    private static final LocalDate DIA_23 = LocalDate.of(2026, 9, 23);

    @Autowired
    CajaService caja;

    @Autowired
    CierreDiarioService cierres;

    @Autowired
    RelojDePrueba reloj;

    @BeforeEach
    void empezarElDia20() {
        reloj.fijar(DIA_20.atTime(10, 0));
    }

    @Test
    void cierraSoloLosDiasQueYaTerminaronAunqueLaAppHayaEstadoApagada() {
        caja.registrarVenta(DIA_20, new BigDecimal("1000"), null);
        caja.registrarVenta(DIA_20, new BigDecimal("500.50"), "Gaseosa");
        caja.registrarGasto(DIA_20, new BigDecimal("300"), "Panadero", null);

        reloj.fijar(DIA_23.atTime(9, 0));
        caja.registrarVenta(DIA_23, new BigDecimal("2000"), null);

        assertThat(cierres.cerrarDiasPendientes()).isEqualTo(3);

        CierreDiario dia20 = cierres.cierreDel(DIA_20).orElseThrow();
        assertThat(dia20.getVentas()).isEqualByComparingTo("1500.50");
        assertThat(dia20.getGastos()).isEqualByComparingTo("300");
        assertThat(dia20.getBalance()).isEqualByComparingTo("1200.50");
        assertThat(cierres.cierreDel(DIA_21).orElseThrow().getBalance()).isEqualByComparingTo("0");
        assertThat(cierres.cierreDel(DIA_23)).as("hoy sigue abierto").isEmpty();

        assertThat(cierres.cerrarDiasPendientes()).as("no vuelve a cerrar lo ya cerrado").isZero();
    }

    @Test
    void siSeCorrigeUnDiaYaCerradoElCierreSeRecalcula() {
        MovimientoCaja venta = caja.registrarVenta(DIA_20, new BigDecimal("1000"), null);
        reloj.fijar(DIA_21.atTime(8, 0));
        cierres.cerrarDiasPendientes();

        caja.registrarGasto(DIA_20, new BigDecimal("400"), "Otros", "Me olvidé de anotarlo");
        assertThat(cierres.cierreDel(DIA_20).orElseThrow().getBalance()).isEqualByComparingTo("600");

        caja.eliminar(venta.getId());
        assertThat(cierres.cierreDel(DIA_20).orElseThrow().getBalance()).isEqualByComparingTo("-400");
    }

    @Test
    void noDejaAnotarEnDiasQueTodaviaNoLlegaron() {
        assertThatThrownBy(() -> caja.registrarVenta(DIA_21, new BigDecimal("100"), null))
                .isInstanceOf(DatoInvalidoException.class);
    }

    @Test
    void lasCategoriasDeGastoNoSeDuplicanPorMayusculas() {
        caja.registrarGasto(DIA_20, new BigDecimal("100"), "  panadero ", null);
        caja.registrarGasto(DIA_20, new BigDecimal("100"), "gas", null);

        assertThat(caja.categoriasDeGasto())
                .containsOnlyOnce("Panadero")
                .contains("Gas")
                .startsWith("Panadero");
        assertThat(caja.movimientosDel(DIA_20)).extracting(MovimientoCaja::getCategoria).contains("Panadero");
    }

    @Test
    void registraLaHoraRealDeCarga() {
        reloj.fijar(LocalDateTime.of(2026, 9, 20, 18, 42));
        MovimientoCaja venta = caja.registrarVenta(DIA_20, new BigDecimal("100"), null);
        assertThat(venta.getRegistradoEn()).isEqualTo(LocalDateTime.of(2026, 9, 20, 18, 42));
    }
}
