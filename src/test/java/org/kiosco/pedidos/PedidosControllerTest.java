package org.kiosco.pedidos;

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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(RelojDePrueba.Config.class)
class PedidosControllerTest {

    /** Domingo. */
    private static final LocalDate HOY = LocalDate.of(2026, 9, 20);

    @Autowired
    MockMvc mvc;

    @Autowired
    PedidoService pedidos;

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
    void sinProveedoresInvitaACargarElPrimero() throws Exception {
        mvc.perform(get("/pedidos"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Empezá cargando tus proveedores")));
    }

    @Test
    void cargaUnProveedorConLosDiasDelPreventista() throws Exception {
        mvc.perform(post("/proveedores").with(csrf())
                        .param("nombre", "Distribuidora Sur")
                        .param("dias", "TUESDAY", "FRIDAY"))
                .andExpect(redirectedUrl("/pedidos"))
                .andExpect(flash().attribute("aviso", "Distribuidora Sur agregado a tus proveedores."));

        mvc.perform(get("/pedidos"))
                .andExpect(content().string(allOf(
                        containsString("Distribuidora Sur"),
                        containsString("Preventista: martes y viernes"))));
    }

    @Test
    void alEditarAparecenMarcadosLosDiasQueYaTenia() throws Exception {
        Proveedor coca = pedidos.guardarProveedor(null, "Coca-Cola", null, EnumSet.of(DayOfWeek.TUESDAY));

        mvc.perform(get("/proveedores/{id}", coca.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("value=\"TUESDAY\" checked=\"checked\">"),
                        not(containsString("value=\"MONDAY\" checked")),
                        // la etiqueta tiene que quedar pegada a la casilla para que Tabler la pinte
                        not(containsString("_dias")))));
    }

    @Test
    void siSeDesmarcanTodosLosDiasQuedaSinPreventista() throws Exception {
        Proveedor coca = pedidos.guardarProveedor(null, "Coca-Cola", null, EnumSet.of(DayOfWeek.TUESDAY));

        mvc.perform(post("/proveedores/{id}", coca.getId()).with(csrf()).param("nombre", "Coca-Cola"))
                .andExpect(redirectedUrl("/pedidos"));

        assertThat(pedidos.proveedor(coca.getId()).getDiasPreventista()).isEmpty();
    }

    @Test
    void siFaltaElNombreVuelveAlFormularioConElError() throws Exception {
        mvc.perform(post("/proveedores").with(csrf()).param("nombre", " "))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Poné el nombre del proveedor.")));
    }

    @Test
    void anotaQuePedirleConHtmx() throws Exception {
        Proveedor coca = pedidos.guardarProveedor(null, "Coca-Cola", null, null);

        mvc.perform(post("/pedidos/proveedores/{id}/notas", coca.getId()).with(csrf())
                        .header("HX-Request", "true")
                        .param("texto", "Sprite 2,25 L x6"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Sprite 2,25 L x6"),
                        not(containsString("<html")))));
    }

    @Test
    void elFormularioDePedidoVieneCompletadoConLasNotas() throws Exception {
        Proveedor coca = pedidos.guardarProveedor(null, "Coca-Cola", null, null);
        pedidos.anotar(coca.getId(), "Sprite x6");
        pedidos.anotar(coca.getId(), "Agua x12");

        mvc.perform(get("/pedidos/nuevo").param("proveedor", coca.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sprite x6\nAgua x12</textarea>")));
    }

    @Test
    void armaUnPedidoSinFechaNiMonto() throws Exception {
        Proveedor coca = pedidos.guardarProveedor(null, "Coca-Cola", null, null);

        mvc.perform(post("/pedidos/nuevo").with(csrf())
                        .param("proveedor", coca.getId().toString())
                        .param("detalle", "Coca 2,25 L x12")
                        .param("fechaEstimada", "")
                        .param("montoEstimado", ""))
                .andExpect(redirectedUrl("/pedidos"));

        assertThat(pedidos.pendientes()).singleElement()
                .satisfies(p -> assertThat(p.getFechaEstimada()).isNull());
    }

    @Test
    void marcarQueLlegoDesdeLaCajaDeHoyActualizaLoQueSalio() throws Exception {
        Proveedor coca = pedidos.guardarProveedor(null, "Coca-Cola", null, EnumSet.of(DayOfWeek.SUNDAY));
        Pedido pedido = pedidos.armarPedido(coca.getId(), "Coca x12", HOY, null);

        mvc.perform(get("/"))
                .andExpect(content().string(allOf(
                        containsString("Hoy viene el preventista de Coca-Cola."),
                        containsString("Pedidos por llegar"),
                        containsString("Llega hoy"))));

        mvc.perform(post("/hoy/pedidos/{id}/llego", pedido.getId()).with(csrf())
                        .header("HX-Request", "true")
                        .param("montoBoleta", "28.400"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Llegó el pedido de Coca-Cola: $ 28.400 anotados como salida."),
                        containsString("<div class=\"h1 mb-1 text-red\">$ 28.400</div>"),
                        containsString("Boleta de Coca-Cola"))));
    }

    @Test
    void siLaBoletaNoTieneMontoValidoLoMarcaEnLaFila() throws Exception {
        Proveedor coca = pedidos.guardarProveedor(null, "Coca-Cola", null, null);
        Pedido pedido = pedidos.armarPedido(coca.getId(), null, null, null);

        mvc.perform(post("/pedidos/{id}/llego", pedido.getId()).with(csrf())
                        .header("HX-Request", "true")
                        .param("montoBoleta", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(containsString("Ingresá un monto."), containsString("is-invalid"))));
        assertThat(pedidos.pendientes()).hasSize(1);
    }

    @Test
    void siLaPaginaQuedoViejaVuelveAMostrarlaConElMotivo() throws Exception {
        Proveedor coca = pedidos.guardarProveedor(null, "Coca-Cola", null, null);
        Pedido pedido = pedidos.armarPedido(coca.getId(), null, null, null);
        pedidos.registrarLlegada(pedido.getId(), new BigDecimal("100"));

        mvc.perform(post("/pedidos/{id}/cancelar", pedido.getId()).with(csrf())
                        .header("HX-Request", "true")
                        .header("HX-Current-URL", "http://localhost:8080/pedidos"))
                .andExpect(status().isNoContent())
                .andExpect(header().string("HX-Redirect", "/pedidos"))
                .andExpect(flash().attribute("errorGeneral", "Solo se puede cancelar un pedido pendiente."));
    }
}
