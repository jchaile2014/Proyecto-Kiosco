package org.kiosco.resumenes;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kiosco.LimpiezaDeBase;
import org.kiosco.RelojDePrueba;
import org.kiosco.caja.CajaService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(RelojDePrueba.Config.class)
class ResumenesControllerTest {

    private static final LocalDate HOY = LocalDate.of(2026, 9, 20);

    @Autowired
    MockMvc mvc;

    @Autowired
    CajaService caja;

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
    void muestraLosUltimos7DiasYElMesPorMes() throws Exception {
        caja.registrarVenta(HOY, new BigDecimal("85300"), null);
        caja.registrarGasto(HOY, new BigDecimal("12800"), "Panadero", null);

        mvc.perform(get("/resumenes"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Últimos 7 días"),
                        containsString("del 14/09 al 20/09"),
                        containsString("$ 85.300"),
                        containsString("Panadero"),
                        containsString("Vendido por día"),
                        containsString("Domingo 20 de septiembre: Vendido $ 85.300"),
                        containsString("Mes por mes"),
                        containsString("Septiembre 2026"),
                        containsString("lo que va"))));
    }

    @Test
    void elDetalleDeUnMesMuestraSusDias() throws Exception {
        caja.registrarVenta(HOY, new BigDecimal("85300"), null);

        mvc.perform(get("/resumenes/2026/9"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Resumen del mes"),
                        containsString("Septiembre 2026"),
                        containsString("Martes 1 de septiembre"),
                        containsString("Domingo 20 de septiembre"))));
    }

    @Test
    void unMesQueNoExisteOTodaviaNoLlegoVuelveAlResumen() throws Exception {
        mvc.perform(get("/resumenes/2026/13")).andExpect(redirectedUrl("/resumenes"));
        mvc.perform(get("/resumenes/2026/10")).andExpect(redirectedUrl("/resumenes"));
    }
}
