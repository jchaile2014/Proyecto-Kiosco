package org.kiosco.productos;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(RelojDePrueba.Config.class)
class ProductosControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ProductoService productos;

    @Autowired
    RelojDePrueba reloj;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        LimpiezaDeBase.vaciar(jdbc);
        reloj.fijar(LocalDate.of(2026, 9, 20).atTime(10, 0));
    }

    @AfterEach
    void limpiar() {
        LimpiezaDeBase.vaciar(jdbc);
    }

    @Test
    void sinProductosLaCajaNoPideCodigoYLaListaExplicaQueSonOpcionales() throws Exception {
        mvc.perform(get("/")).andExpect(content().string(not(containsString("venta-codigo"))));
        mvc.perform(get("/productos")).andExpect(content().string(containsString("Todavía no cargaste productos")));
    }

    @Test
    void cargaUnProductoConStock() throws Exception {
        mvc.perform(post("/productos").with(csrf())
                        .param("codigo", "coca")
                        .param("nombre", "Coca-Cola 2,25 L")
                        .param("precio", "3.200")
                        .param("controlaStock", "true")
                        .param("stock", "12")
                        .param("stockMinimo", "3"))
                .andExpect(redirectedUrl("/productos"))
                .andExpect(flash().attribute("aviso", "Coca-Cola 2,25 L agregado a la lista."));

        mvc.perform(get("/productos"))
                .andExpect(content().string(allOf(containsString("Coca-Cola 2,25 L"), containsString("$ 3.200"))));
    }

    @Test
    void unPrecioMalEscritoVuelveAlFormulario() throws Exception {
        mvc.perform(post("/productos").with(csrf())
                        .param("codigo", "coca").param("nombre", "Coca").param("precio", "tres mil"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Escribí solo números")));
    }

    @Test
    void venderPorCodigoDesdeLaCajaCompletaElPrecioYDiceCuantoQueda() throws Exception {
        productos.guardar(null, "coca", "Coca-Cola 2,25 L", new BigDecimal("3200"), true, 12, null);

        mvc.perform(post("/movimientos/venta").with(csrf())
                        .header("HX-Request", "true")
                        .param("monto", "")
                        .param("codigo", "coca"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Coca-Cola 2,25 L: $ 3.200 anotados. Quedan 11."),
                        containsString("+ $ 3.200"))));
    }

    @Test
    void unCodigoDeBarrasEscaneadoEnElMontoSeTomaComoProducto() throws Exception {
        productos.guardar(null, "7790895000997", "Sprite 2,25 L", new BigDecimal("3100"), false, null, null);

        mvc.perform(post("/movimientos/venta").with(csrf())
                        .header("HX-Request", "true")
                        .param("monto", "7790895000997"))
                .andExpect(content().string(allOf(
                        containsString("Sprite 2,25 L: $ 3.100 anotados."),
                        not(containsString("demasiado grande")))));
    }

    @Test
    void entroMercaderiaDesdeLaLista() throws Exception {
        Producto coca = productos.guardar(null, "coca", "Coca-Cola 2,25 L", new BigDecimal("3200"), true, 12, null);

        mvc.perform(post("/productos/{id}/sumar", coca.getId()).with(csrf())
                        .header("HX-Request", "true")
                        .param("cantidad", "24"))
                .andExpect(content().string(containsString("Entraron 24 de Coca-Cola 2,25 L. Ahora hay 36.")));
        assertThat(productos.producto(coca.getId()).getStock()).isEqualTo(36);
    }

    @Test
    void laBusquedaFiltraLaLista() throws Exception {
        productos.guardar(null, "coca", "Coca-Cola 2,25 L", new BigDecimal("3200"), false, null, null);
        productos.guardar(null, "alfa", "Alfajor triple", new BigDecimal("1500"), false, null, null);

        mvc.perform(get("/productos").param("q", "alfa"))
                .andExpect(content().string(allOf(containsString("Alfajor triple"), not(containsString("Coca-Cola 2,25 L")))));
    }
}
