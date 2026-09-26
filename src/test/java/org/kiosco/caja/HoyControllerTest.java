package org.kiosco.caja;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kiosco.RelojDePrueba;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(RelojDePrueba.Config.class)
@Transactional
class HoyControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    RelojDePrueba reloj;

    @BeforeEach
    void setUp() {
        reloj.fijar(LocalDateTime.of(2026, 9, 20, 10, 0));
    }

    @Test
    void muestraLaCajaDeHoy() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Domingo 20 de septiembre"),
                        containsString("Anotar venta"),
                        containsString("Todavía no hay nada anotado"))));
    }

    @Test
    void conHtmxAnotaLaVentaYDevuelveSoloElPanelActualizado() throws Exception {
        mvc.perform(post("/movimientos/venta").with(csrf())
                        .header("HX-Request", "true")
                        .param("monto", "1.500,50")
                        .param("descripcion", "Gaseosa"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Venta de $ 1.500,50 anotada"),
                        containsString("+ $ 1.500,50"),
                        containsString("Gaseosa"),
                        not(containsString("<html")))));
    }

    @Test
    void siElMontoNoSeEntiendeMuestraElErrorSinGuardar() throws Exception {
        mvc.perform(post("/movimientos/venta").with(csrf())
                        .header("HX-Request", "true")
                        .param("monto", "abc"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Escribí solo números"),
                        containsString("is-invalid"),
                        containsString("Todavía no hay nada anotado"))));
    }

    @Test
    void elGastoPideEnQueSeGasto() throws Exception {
        mvc.perform(post("/movimientos/gasto").with(csrf())
                        .header("HX-Request", "true")
                        .param("monto", "12800"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Elegí o escribí en qué se gastó")));
    }

    @Test
    void sinHtmxRedirigeDespuesDeGuardar() throws Exception {
        mvc.perform(post("/movimientos/venta").with(csrf()).param("monto", "500"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void rechazaCargasSinTokenCsrf() throws Exception {
        mvc.perform(post("/movimientos/venta").param("monto", "500"))
                .andExpect(status().isForbidden());
    }
}
