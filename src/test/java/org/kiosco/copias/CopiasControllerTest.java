package org.kiosco.copias;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Los tests corren con H2, sin MySQL: acá se simula que hay copias para ver las pantallas. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CopiasControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    CopiaDeSeguridadService copias;

    @Test
    void muestraLasCopiasGuardadas() throws Exception {
        Path carpeta = Path.of("/home/kiosco/.kiosco/copias");
        when(copias.disponible()).thenReturn(true);
        when(copias.carpeta()).thenReturn(carpeta);
        when(copias.copias()).thenReturn(List.of(
                new CopiaDeSeguridadService.Copia(carpeta.resolve("kiosco-2026-09-26.sql.gz"), LocalDate.of(2026, 9, 26), 48_300)));
        when(copias.ultimoResultado()).thenReturn(
                new CopiaDeSeguridadService.Resultado(LocalDateTime.of(2026, 9, 26, 9, 30), true, "ok"));

        mvc.perform(get("/copias"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("kiosco-2026-09-26.sql.gz"),
                        containsString("47,2 KB"),
                        containsString("Salió bien"),
                        containsString("/home/kiosco/.kiosco/copias"))));
    }

    @Test
    void siLaCopiaFallaLoDiceEnLaCajaDeHoy() throws Exception {
        when(copias.ultimoResultado()).thenReturn(
                new CopiaDeSeguridadService.Resultado(LocalDateTime.now(), false, "Access denied for user 'kiosco'"));

        mvc.perform(get("/"))
                .andExpect(content().string(containsString("La última copia de seguridad falló: Access denied")));
    }

    @Test
    void elBotonDeCopiaAvisaSiFallo() throws Exception {
        when(copias.hacerCopia()).thenThrow(new CopiaFallidaException("mysqldump: command not found"));

        mvc.perform(post("/copias").with(csrf()))
                .andExpect(redirectedUrl("/copias"))
                .andExpect(flash().attribute("errorCopia", "mysqldump: command not found"));
    }
}
