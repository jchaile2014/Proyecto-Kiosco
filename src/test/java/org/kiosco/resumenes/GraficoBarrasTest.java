package org.kiosco.resumenes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.kiosco.comun.Formatos;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GraficoBarrasTest {

    private final Formatos fmt = new Formatos(Clock.systemDefaultZone());

    @ParameterizedTest(name = "máximo {0} → paso {1}")
    @CsvSource({
            "95000,   25000",
            "100000,  25000",
            "130000,  50000",
            "1900000, 500000",
            "50,      100",
            "0,       1000",
    })
    void elEjeUsaPasosRedondos(String maximo, String paso) {
        assertThat(GraficoBarras.pasoRedondo(new BigDecimal(maximo))).isEqualByComparingTo(paso);
    }

    @Test
    void lasAlturasSonProporcionalesAlTopeDelEje() {
        GraficoBarras g = GraficoBarras.armar("prueba", List.of("Vendido"), List.of(
                columna("Lun", "50000"),
                columna("Mar", "95000")), GraficoBarras.Destacar.MAXIMO, fmt);

        assertThat(g.lineas()).extracting(GraficoBarras.Linea::texto)
                .containsExactly("$ 0", "$ 25 mil", "$ 50 mil", "$ 75 mil", "$ 100 mil");
        assertThat(g.grupos()).extracting(gr -> gr.barras().getFirst().altura()).containsExactly(50.0, 95.0);
    }

    @Test
    void conUnaSerieSoloSeEscribeElValorDelMejorDia() {
        GraficoBarras g = GraficoBarras.armar("prueba", List.of("Vendido"), List.of(
                columna("Lun", "30000"),
                columna("Mar", "95000"),
                columna("Mié", "95000")), GraficoBarras.Destacar.MAXIMO, fmt);

        assertThat(g.grupos()).extracting(gr -> gr.barras().getFirst().etiqueta())
                .containsExactly(null, "$ 95 mil", null);
    }

    @Test
    void conVariasSeriesSoloSeEscribeLaPrimeraDeLaUltimaColumnaParaQueNoSePisen() {
        GraficoBarras g = GraficoBarras.armar("prueba", List.of("Vendido", "Pedidos"), List.of(
                new GraficoBarras.Columna("Ago", "Agosto", "/a", List.of(new BigDecimal("1800000"), new BigDecimal("600000"))),
                new GraficoBarras.Columna("Sep", "Septiembre", "/s", List.of(new BigDecimal("900000"), new BigDecimal("300000")))),
                GraficoBarras.Destacar.ULTIMA_COLUMNA, fmt);

        assertThat(g.grupos().getFirst().barras()).extracting(GraficoBarras.Barra::etiqueta).containsOnlyNulls();
        assertThat(g.grupos().getLast().barras()).extracting(GraficoBarras.Barra::etiqueta)
                .containsExactly("$ 900 mil", null);
        assertThat(g.grupos().getLast().descripcionAccesible())
                .isEqualTo("Septiembre: Vendido $ 900.000, Pedidos $ 300.000");
    }

    @Test
    void sinDatosElGraficoIgualTieneEje() {
        GraficoBarras g = GraficoBarras.armar("prueba", List.of("Vendido"), List.of(columna("Lun", "0")),
                GraficoBarras.Destacar.MAXIMO, fmt);

        assertThat(g.lineas()).hasSize(2);
        assertThat(g.grupos().getFirst().barras().getFirst().altura()).isZero();
    }

    private static GraficoBarras.Columna columna(String etiqueta, String valor) {
        return new GraficoBarras.Columna(etiqueta, etiqueta, "/", List.of(new BigDecimal(valor)));
    }
}
