package org.kiosco.fiados;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kiosco.LimpiezaDeBase;
import org.kiosco.RelojDePrueba;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(RelojDePrueba.Config.class)
class FiadosControllerTest {

    private static final LocalDate HOY = LocalDate.of(2026, 9, 20);

    @Autowired
    MockMvc mvc;

    @Autowired
    FiadoService fiados;

    @Autowired
    RelojDePrueba reloj;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        LimpiezaDeBase.vaciar(jdbc);
        reloj.fijar(HOY.atTime(10, 0));
    }

    @AfterEach
    void limpiar() {
        LimpiezaDeBase.vaciar(jdbc);
    }

    @Test
    void sinFiadosDiceQueNadieDebe() throws Exception {
        mvc.perform(get("/fiados"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Nadie te debe nada.")));
    }

    @Test
    void anotarDesdeLaListaAgregaAlClienteYDiceCuantoDebe() throws Exception {
        mvc.perform(post("/fiados/anotar").with(csrf())
                        .header("HX-Request", "true")
                        .param("cliente", "Marta")
                        .param("descripcion", "Pan y leche")
                        .param("monto", "2.300"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Anotado a Marta: $ 2.300. Ahora debe $ 2.300."),
                        containsString("1 cliente con cuenta abierta"),
                        not(containsString("<html")))));
    }

    @Test
    void siFaltaElNombreLoMarcaSinAnotar() throws Exception {
        mvc.perform(post("/fiados/anotar").with(csrf())
                        .header("HX-Request", "true")
                        .param("cliente", "")
                        .param("monto", "2300"))
                .andExpect(content().string(allOf(
                        containsString("Poné el nombre de quién se lo lleva."),
                        containsString("is-invalid"),
                        containsString("Nadie te debe nada."))));
    }

    @Test
    void yaPagoDesdeLaListaSumaLaPlataALaCajaDeHoy() throws Exception {
        Long marta = fiados.anotar("Marta", "Varios", new BigDecimal("6800")).getClienteId();

        mvc.perform(post("/fiados/{id}/pago-total", marta).with(csrf())
                        .header("HX-Request", "true")
                        .header("HX-Target", "panel-fiados"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Marta pagó $ 6.800. Entró a la caja de hoy y la cuenta quedó en cero."),
                        containsString("Nadie te debe nada."))));

        mvc.perform(get("/"))
                .andExpect(content().string(allOf(
                        containsString("$ 6.800"),
                        containsString("Fiados cobrados $ 6.800"),
                        containsString("Cobro de fiado"))));
    }

    @Test
    void yaPagoDesdeLaLibretaMuestraLaCuentaAlDiaYElHistorial() throws Exception {
        Long marta = fiados.anotar("Marta", "Varios", new BigDecimal("6800")).getClienteId();

        mvc.perform(post("/fiados/{id}/pago-total", marta).with(csrf())
                        .header("HX-Request", "true")
                        .header("HX-Target", "panel-cliente"))
                .andExpect(content().string(allOf(
                        containsString("Al día"),
                        containsString("Cuentas que ya pagó"),
                        containsString("pagó $ 6.800"))));
    }

    @Test
    void laLibretaMuestraLoQueVaDebiendo() throws Exception {
        Long marta = fiados.anotar("Marta", "Pan y leche", new BigDecimal("2300")).getClienteId();
        fiados.anotar(marta, "Fiambre", new BigDecimal("4500"));

        mvc.perform(get("/fiados/{id}", marta))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Libreta de"),
                        containsString("Va debiendo"),
                        containsString("Fiambre"),
                        containsString("$ 6.800"))));
    }

    @Test
    void aplicaInteresDesdeLaLibreta() throws Exception {
        Long marta = fiados.anotar("Marta", null, new BigDecimal("12300")).getClienteId();

        mvc.perform(post("/fiados/{id}/interes", marta).with(csrf())
                        .header("HX-Request", "true")
                        .param("porcentaje", "10"))
                .andExpect(content().string(allOf(
                        containsString("Interés 10 % sobre $ 12.300: se sumaron $ 1.230."),
                        containsString("$ 13.530"))));
    }

    @Test
    void unPagoMayorALaDeudaSeMarcaEnElCampo() throws Exception {
        Long marta = fiados.anotar("Marta", null, new BigDecimal("1000")).getClienteId();

        mvc.perform(post("/fiados/{id}/pago", marta).with(csrf())
                        .header("HX-Request", "true")
                        .param("monto", "1.500"))
                .andExpect(content().string(allOf(
                        containsString("Debe $ 1.000: el pago no puede ser mayor."),
                        containsString("value=\"1.500\""))));
    }

    @Test
    void unPorcentajeQueNoEsNumeroSeExplica() throws Exception {
        Long marta = fiados.anotar("Marta", null, new BigDecimal("1000")).getClienteId();

        mvc.perform(post("/fiados/{id}/interes", marta).with(csrf())
                        .header("HX-Request", "true")
                        .param("porcentaje", "diez"))
                .andExpect(content().string(containsString("Escribí el porcentaje con números")));
    }
}
