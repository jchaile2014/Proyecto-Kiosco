package org.kiosco.productos;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kiosco.RelojDePrueba;
import org.kiosco.caja.CajaService;
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

@SpringBootTest
@ActiveProfiles("test")
@Import(RelojDePrueba.Config.class)
@Transactional
class ProductoServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 9, 20);

    @Autowired
    ProductoService productos;

    @Autowired
    CajaService caja;

    @Autowired
    RelojDePrueba reloj;

    private Producto coca;

    @BeforeEach
    void setUp() {
        reloj.fijar(HOY.atTime(10, 0));
        coca = productos.guardar(null, "coca", "Coca-Cola 2,25 L", new BigDecimal("3200"), true, 12, 3);
    }

    @Test
    void sinMontoSeCobraElPrecioDeListaYSeDescuentaElStock() {
        ProductoService.VentaDeProducto venta = productos.vender("COCA", 2, HOY, null, null);

        assertThat(venta.venta().getMonto()).isEqualByComparingTo("6400");
        assertThat(venta.venta().getDetalle()).isEqualTo("2 × Coca-Cola 2,25 L");
        assertThat(caja.totalesDel(HOY).ventas()).isEqualByComparingTo("6400");
        assertThat(productos.producto(coca.getId()).getStock()).isEqualTo(10);
    }

    @Test
    void siSeEscribeElMontoSeRespeta() {
        ProductoService.VentaDeProducto venta = productos.vender("coca", 1, HOY, new BigDecimal("3000"), "Con descuento");

        assertThat(venta.venta().getMonto()).isEqualByComparingTo("3000");
        assertThat(venta.venta().getDetalle()).isEqualTo("Con descuento");
    }

    @Test
    void siNoControlaStockElNumeroNoSeToca() {
        Producto pan = productos.guardar(null, "pan", "Pan por kilo", new BigDecimal("2500"), false, null, null);

        productos.vender("pan", 3, HOY, null, null);

        assertThat(productos.producto(pan.getId()).getStock()).isZero();
        assertThat(productos.historialDeStock(pan.getId())).isEmpty();
    }

    @Test
    void borrarLaVentaDesdeLaCajaDevuelveElStock() {
        ProductoService.VentaDeProducto venta = productos.vender("coca", 2, HOY, null, null);

        caja.eliminar(venta.venta().getId());

        assertThat(productos.producto(coca.getId()).getStock()).isEqualTo(12);
        assertThat(productos.historialDeStock(coca.getId())).extracting(MovimientoStock::getMotivo)
                .containsExactly(MotivoStock.AJUSTE);
    }

    @Test
    void venderMasDeLoQueHayDejaElStockNegativoParaQueSeCorrija() {
        productos.vender("coca", 13, HOY, null, null);

        assertThat(productos.producto(coca.getId()).getStock()).isEqualTo(-1);
    }

    @Test
    void unCodigoQueNoExisteSeExplica() {
        assertThatThrownBy(() -> productos.vender("sprite", 1, HOY, null, null))
                .isInstanceOf(DatoInvalidoException.class)
                .hasMessage("No hay ningún producto con el código sprite.");
    }

    @Test
    void noHayDosProductosConElMismoCodigo() {
        assertThatThrownBy(() -> productos.guardar(null, "COCA", "Otra", new BigDecimal("100"), false, null, null))
                .isInstanceOf(DatoInvalidoException.class)
                .hasMessageContaining("Ya hay otro producto");
    }

    @Test
    void cambiarElStockALaManoQuedaComoCorreccion() {
        productos.guardar(coca.getId(), "coca", "Coca-Cola 2,25 L", new BigDecimal("3400"), true, 8, 3);

        Producto editado = productos.producto(coca.getId());
        assertThat(editado.getStock()).isEqualTo(8);
        assertThat(editado.getPrecio()).isEqualByComparingTo("3400");
        assertThat(productos.historialDeStock(coca.getId())).first()
                .satisfies(m -> {
                    assertThat(m.getMotivo()).isEqualTo(MotivoStock.AJUSTE);
                    assertThat(m.getCantidad()).isEqualTo(-4);
                });
    }

    @Test
    void entroMercaderiaSumaAlStock() {
        productos.sumarStock(coca.getId(), 24);

        assertThat(productos.producto(coca.getId()).getStock()).isEqualTo(36);
        assertThat(productos.historialDeStock(coca.getId()).getFirst().getMotivo()).isEqualTo(MotivoStock.INGRESO);
    }

    @Test
    void avisaCuandoQuedaPoco() {
        productos.vender("coca", 9, HOY, null, null);

        assertThat(productos.conPocoStock()).extracting(Producto::getNombre).containsExactly("Coca-Cola 2,25 L");
    }

    @Test
    void seBuscaPorNombreOPorCodigo() {
        productos.guardar(null, "7790895000997", "Sprite 2,25 L", new BigDecimal("3100"), false, null, null);

        assertThat(productos.buscar("sprite")).extracting(Producto::getCodigo).containsExactly("7790895000997");
        assertThat(productos.buscar("77908")).extracting(Producto::getNombre).containsExactly("Sprite 2,25 L");
        assertThat(productos.buscar("")).hasSize(2);
    }
}
