package org.kiosco.pedidos;

import org.kiosco.comun.DatoInvalidoException;
import org.kiosco.comun.Formatos;
import org.kiosco.comun.Montos;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;

/**
 * Pantalla de pedidos: notas por proveedor, pedidos pendientes y los que ya llegaron.
 * Las acciones rápidas (notas, "llegó") responden con el panel para HTMX; armar un
 * pedido es un formulario aparte.
 */
@Controller
class PedidosController {

    private final PedidoService servicio;
    private final Formatos fmt;
    private final Clock clock;

    PedidosController(PedidoService servicio, Formatos fmt, Clock clock) {
        this.servicio = servicio;
        this.fmt = fmt;
        this.clock = clock;
    }

    @GetMapping("/pedidos")
    String pagina(Model model) {
        cargar(model);
        return "pedidos";
    }

    @PostMapping("/pedidos/proveedores/{id}/notas")
    String anotar(@PathVariable Long id, @RequestParam(required = false) String texto,
                  @RequestHeader(name = "HX-Request", required = false) String htmx, Model model) {
        try {
            servicio.anotar(id, texto);
        } catch (DatoInvalidoException e) {
            model.addAttribute("errorNota", e.getMessage());
            model.addAttribute("textoNota", texto);
        }
        model.addAttribute("proveedorEnfocado", id);
        return responder(htmx, model);
    }

    @PostMapping("/pedidos/proveedores/{id}/notas/{notaId}/borrar")
    String borrarNota(@PathVariable Long id, @PathVariable Long notaId,
                      @RequestHeader(name = "HX-Request", required = false) String htmx, Model model) {
        servicio.borrarNota(id, notaId);
        return responder(htmx, model);
    }

    @PostMapping("/pedidos/{id}/llego")
    String llego(@PathVariable Long id, @RequestParam(required = false) String montoBoleta,
                 @RequestHeader(name = "HX-Request", required = false) String htmx, Model model) {
        try {
            BigDecimal monto = Montos.parse(montoBoleta);
            Pedido pedido = servicio.registrarLlegada(id, monto);
            model.addAttribute("aviso", "Llegó el pedido de " + pedido.getProveedor().getNombre()
                    + ": " + fmt.plata(monto) + " anotados como salida de hoy.");
        } catch (DatoInvalidoException e) {
            model.addAttribute("errorLlegada", e.getMessage());
            model.addAttribute("pedidoConError", id);
            model.addAttribute("montoIngresado", montoBoleta);
        }
        return responder(htmx, model);
    }

    @PostMapping("/pedidos/{id}/deshacer")
    String deshacer(@PathVariable Long id,
                    @RequestHeader(name = "HX-Request", required = false) String htmx, Model model) {
        servicio.deshacerLlegada(id);
        model.addAttribute("aviso", "Listo: el pedido volvió a pendiente y se borró el pago de la caja.");
        return responder(htmx, model);
    }

    @PostMapping("/pedidos/{id}/cancelar")
    String cancelar(@PathVariable Long id,
                    @RequestHeader(name = "HX-Request", required = false) String htmx, Model model) {
        servicio.cancelar(id);
        return responder(htmx, model);
    }

    @GetMapping("/pedidos/nuevo")
    String nuevo(@RequestParam Long proveedor, Model model) {
        Proveedor elegido = servicio.proveedor(proveedor);
        PedidoForm form = new PedidoForm();
        form.setDetalle(String.join("\n", elegido.getNotas().stream().map(NotaPedido::getTexto).toList()));
        model.addAttribute("proveedor", elegido);
        model.addAttribute("pedidoForm", form);
        model.addAttribute("hoy", LocalDate.now(clock));
        return "pedido-form";
    }

    @PostMapping("/pedidos/nuevo")
    String armar(@RequestParam Long proveedor, @ModelAttribute("pedidoForm") PedidoForm form,
                 Model model, RedirectAttributes redirect) {
        try {
            BigDecimal estimado = form.getMontoEstimado() == null || form.getMontoEstimado().isBlank()
                    ? null : Montos.parse(form.getMontoEstimado());
            Pedido pedido = servicio.armarPedido(proveedor, form.getDetalle(), form.getFechaEstimada(), estimado);
            redirect.addFlashAttribute("aviso", "Pedido a " + pedido.getProveedor().getNombre() + " anotado.");
            return "redirect:/pedidos";
        } catch (DatoInvalidoException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("proveedor", servicio.proveedor(proveedor));
            model.addAttribute("hoy", LocalDate.now(clock));
            return "pedido-form";
        }
    }

    private String responder(String htmx, Model model) {
        if (htmx == null) {
            return "redirect:/pedidos";
        }
        cargar(model);
        return "pedidos :: panel";
    }

    private void cargar(Model model) {
        model.addAttribute("hoy", LocalDate.now(clock));
        model.addAttribute("proveedores", servicio.proveedoresConNotas());
        model.addAttribute("pendientes", servicio.pendientes());
        model.addAttribute("llegados", servicio.llegadosRecientes());
    }
}
