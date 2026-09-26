package org.kiosco.resumenes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kiosco.RelojDePrueba;
import org.kiosco.caja.CajaService;
import org.kiosco.cierre.CierreDiarioService;
import org.kiosco.comun.MontoPorNombre;
import org.kiosco.pedidos.Pedido;
import org.kiosco.pedidos.PedidoService;
import org.kiosco.pedidos.Proveedor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(RelojDePrueba.Config.class)
@Transactional
class ResumenServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 9, 20);

    @Autowired
    ResumenService resumenes;

    @Autowired
    CajaService caja;

    @Autowired
    CierreDiarioService cierres;

    @Autowired
    PedidoService pedidos;

    @Autowired
    RelojDePrueba reloj;

    @BeforeEach
    void setUp() {
        reloj.fijar(HOY.atTime(10, 0));
    }

    @Test
    void losUltimos7DiasUsanLosCierresGuardadosYCalculanHoyEnElMomento() {
        venderEl(HOY.minusDays(7), "9999");   // afuera: es de la semana anterior
        venderEl(HOY.minusDays(6), "1000");
        venderEl(HOY.minusDays(1), "2000");
        reloj.fijar(HOY.atTime(10, 0));
        cierres.cerrarDiasPendientes();
        caja.registrarVenta(HOY, plata("500"), null);

        Periodo semana = resumenes.ultimos7Dias();

        assertThat(semana.desde()).isEqualTo(HOY.minusDays(6));
        assertThat(semana.hasta()).isEqualTo(HOY);
        assertThat(semana.dias()).hasSize(7);
        assertThat(semana.dias().getLast().totales().ventas()).as("hoy, sin cerrar").isEqualByComparingTo("500");
        assertThat(semana.totales().ventas()).isEqualByComparingTo("3500");
        assertThat(resumenes.semanaAnterior().ventas()).isEqualByComparingTo("9999");
    }

    @Test
    void elMesPorMesSumaLoGuardadoYAgregaHoyAlMesEnCurso() {
        venderEl(LocalDate.of(2026, 8, 30), "70000");
        venderEl(LocalDate.of(2026, 9, 1), "30000");
        reloj.fijar(HOY.atTime(10, 0));
        cierres.cerrarDiasPendientes();
        caja.registrarVenta(HOY, plata("5000"), null);

        assertThat(resumenes.meses()).satisfiesExactly(
                septiembre -> {
                    assertThat(septiembre.mes()).isEqualTo(YearMonth.of(2026, 9));
                    assertThat(septiembre.enCurso()).isTrue();
                    assertThat(septiembre.totales().ventas()).isEqualByComparingTo("35000");
                },
                agosto -> {
                    assertThat(agosto.mes()).isEqualTo(YearMonth.of(2026, 8));
                    assertThat(agosto.enCurso()).isFalse();
                    assertThat(agosto.totales().ventas()).isEqualByComparingTo("70000");
                });
    }

    @Test
    void sinMovimientosIgualApareceElMesEnCurso() {
        assertThat(resumenes.meses()).singleElement()
                .satisfies(m -> assertThat(m.totales().balance()).isEqualByComparingTo("0"));
    }

    @Test
    void elPeriodoDiceEnQueSeGastoYCuantoSeLePagoACadaProveedor() {
        caja.registrarGasto(HOY, plata("12000"), "Panadero", null);
        caja.registrarGasto(HOY, plata("3000"), "Panadero", null);
        caja.registrarGasto(HOY, plata("48500"), "Servicios", "Luz");
        Proveedor coca = pedidos.guardarProveedor(null, "Coca-Cola", null, null);
        Pedido pedido = pedidos.armarPedido(coca.getId(), null, null, null);
        pedidos.registrarLlegada(pedido.getId(), plata("76500"));

        Periodo semana = resumenes.ultimos7Dias();

        assertThat(semana.gastosPorCategoria()).extracting(MontoPorNombre::nombre).containsExactly("Servicios", "Panadero");
        assertThat(semana.gastosPorCategoria()).extracting(MontoPorNombre::monto)
                .usingElementComparator(BigDecimal::compareTo).containsExactly(plata("48500"), plata("15000"));
        assertThat(semana.pagadoPorProveedor()).singleElement().satisfies(p -> {
            assertThat(p.nombre()).isEqualTo("Coca-Cola");
            assertThat(p.monto()).isEqualByComparingTo("76500");
        });
        assertThat(semana.totales().balance()).isEqualByComparingTo("-140000");
    }

    private void venderEl(LocalDate dia, String monto) {
        reloj.fijar(dia.atTime(12, 0));
        caja.registrarVenta(dia, plata(monto), null);
    }

    private static BigDecimal plata(String monto) {
        return new BigDecimal(monto);
    }
}
