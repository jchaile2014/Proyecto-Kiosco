package org.kiosco.fiados;

import org.kiosco.comun.DatoInvalidoException;
import org.kiosco.comun.Formatos;
import org.kiosco.comun.MontoInvalidoException;
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
import java.util.List;

/**
 * Fiados: la lista de cuentas y la libreta de cada cliente. Las acciones responden con el
 * panel que las pidió (lista o libreta) para que HTMX lo reemplace sin recargar.
 */
@Controller
class FiadosController {

    private static final String PANEL_CLIENTE = "panel-cliente";

    private final FiadoService servicio;
    private final Formatos fmt;

    FiadosController(FiadoService servicio, Formatos fmt) {
        this.servicio = servicio;
        this.fmt = fmt;
    }

    // --- Lista ---

    @GetMapping("/fiados")
    String lista(Model model) {
        cargarLista(model);
        return "fiados";
    }

    @PostMapping("/fiados/anotar")
    String anotar(@ModelAttribute("fiadoForm") FiadoForm form,
                  @RequestHeader(name = "HX-Request", required = false) String htmx, Model model) {
        try {
            MovimientoFiado renglon = servicio.anotar(form.getCliente(), form.getDescripcion(), Montos.parse(form.getMonto()));
            CuentaCliente cuenta = servicio.cuenta(renglon.getClienteId());
            model.addAttribute("aviso", "Anotado a " + cuenta.cliente().getNombre() + ": " + fmt.plata(renglon.getMonto())
                    + ". Ahora debe " + fmt.plata(cuenta.saldo()) + ".");
            model.addAttribute("fiadoForm", new FiadoForm());
        } catch (DatoInvalidoException e) {
            model.addAttribute("errorAnotar", e.getMessage());
            model.addAttribute("campoConError", e instanceof MontoInvalidoException ? "monto"
                    : form.getCliente() == null || form.getCliente().isBlank() ? "cliente" : null);
        }
        if (htmx == null) {
            return "redirect:/fiados";
        }
        cargarLista(model);
        return "fiados :: panel";
    }

    /** "Ya pagó": se puede hacer desde la lista o desde la libreta del cliente. */
    @PostMapping("/fiados/{id}/pago-total")
    String pagoTotal(@PathVariable Long id,
                     @RequestHeader(name = "HX-Request", required = false) String htmx,
                     @RequestHeader(name = "HX-Target", required = false) String destino, Model model) {
        String nombre = servicio.cliente(id).getNombre();
        BigDecimal pagado = servicio.pagarTodo(id);
        model.addAttribute("aviso", nombre + " pagó " + fmt.plata(pagado)
                + ". Entró a la caja de hoy y la cuenta quedó en cero.");
        if (htmx == null) {
            return "redirect:/fiados/" + id;
        }
        if (PANEL_CLIENTE.equals(destino)) {
            cargarCliente(model, id);
            return "cliente :: panel";
        }
        cargarLista(model);
        return "fiados :: panel";
    }

    // --- Libreta de un cliente ---

    @GetMapping("/fiados/{id}")
    String libreta(@PathVariable Long id, Model model) {
        cargarCliente(model, id);
        return "cliente";
    }

    @PostMapping("/fiados/{id}/anotar")
    String anotarEnLibreta(@PathVariable Long id, @ModelAttribute("fiadoForm") FiadoForm form,
                           @RequestHeader(name = "HX-Request", required = false) String htmx, Model model) {
        try {
            MovimientoFiado renglon = servicio.anotar(id, form.getDescripcion(), Montos.parse(form.getMonto()));
            model.addAttribute("aviso", "Anotado: " + renglon.getDetalle() + " por " + fmt.plata(renglon.getMonto()) + ".");
            model.addAttribute("fiadoForm", new FiadoForm());
        } catch (DatoInvalidoException e) {
            model.addAttribute("errorLleva", e.getMessage());
        }
        return responderCliente(id, htmx, model);
    }

    @PostMapping("/fiados/{id}/pago")
    String pagoParcial(@PathVariable Long id, @RequestParam(required = false) String monto,
                       @RequestHeader(name = "HX-Request", required = false) String htmx, Model model) {
        try {
            MovimientoFiado pago = servicio.registrarPago(id, Montos.parse(monto));
            model.addAttribute("aviso", "Pago de " + fmt.plata(pago.getMonto()) + " anotado. Entró a la caja de hoy.");
        } catch (DatoInvalidoException e) {
            model.addAttribute("errorPago", e.getMessage());
            model.addAttribute("montoPago", monto);
        }
        return responderCliente(id, htmx, model);
    }

    @PostMapping("/fiados/{id}/interes")
    String interes(@PathVariable Long id, @RequestParam(required = false) String porcentaje,
                   @RequestHeader(name = "HX-Request", required = false) String htmx, Model model) {
        try {
            MovimientoFiado interes = servicio.aplicarInteres(id, Montos.parsePorcentaje(porcentaje));
            model.addAttribute("aviso", interes.getDetalle() + ": se sumaron " + fmt.plata(interes.getMonto()) + ".");
        } catch (DatoInvalidoException e) {
            model.addAttribute("errorInteres", e.getMessage());
            model.addAttribute("porcentaje", porcentaje);
        }
        return responderCliente(id, htmx, model);
    }

    @PostMapping("/fiados/{id}/renglones/{renglonId}/borrar")
    String borrarRenglon(@PathVariable Long id, @PathVariable Long renglonId,
                         @RequestHeader(name = "HX-Request", required = false) String htmx, Model model) {
        servicio.borrarRenglon(id, renglonId);
        return responderCliente(id, htmx, model);
    }

    @PostMapping("/fiados/{id}/deshacer")
    String deshacer(@PathVariable Long id,
                    @RequestHeader(name = "HX-Request", required = false) String htmx, Model model) {
        servicio.deshacerUltimaCuentaSaldada(id);
        model.addAttribute("aviso", "Listo: la cuenta volvió a estar abierta y el pago se sacó de la caja.");
        return responderCliente(id, htmx, model);
    }

    // --- Datos del cliente ---

    @GetMapping("/fiados/{id}/editar")
    String editar(@PathVariable Long id, Model model) {
        Cliente cliente = servicio.cliente(id);
        model.addAttribute("cliente", cliente);
        model.addAttribute("nombre", cliente.getNombre());
        model.addAttribute("telefono", cliente.getTelefono());
        return "cliente-form";
    }

    @PostMapping("/fiados/{id}/editar")
    String guardar(@PathVariable Long id, @RequestParam(required = false) String nombre,
                   @RequestParam(required = false) String telefono, Model model, RedirectAttributes redirect) {
        try {
            servicio.guardarCliente(id, nombre, telefono);
            redirect.addFlashAttribute("aviso", "Datos guardados.");
            return "redirect:/fiados/" + id;
        } catch (DatoInvalidoException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("cliente", servicio.cliente(id));
            model.addAttribute("nombre", nombre);
            model.addAttribute("telefono", telefono);
            return "cliente-form";
        }
    }

    @PostMapping("/fiados/{id}/baja")
    String darDeBaja(@PathVariable Long id, RedirectAttributes redirect) {
        String nombre = servicio.cliente(id).getNombre();
        servicio.darDeBaja(id);
        redirect.addFlashAttribute("aviso", nombre + " ya no aparece en la lista de clientes.");
        return "redirect:/fiados";
    }

    // --- Internos ---

    private String responderCliente(Long id, String htmx, Model model) {
        if (htmx == null) {
            return "redirect:/fiados/" + id;
        }
        cargarCliente(model, id);
        return "cliente :: panel";
    }

    private void cargarLista(Model model) {
        List<CuentaCliente> cuentas = servicio.cuentas();
        model.addAttribute("abiertas", cuentas.stream().filter(CuentaCliente::debe).toList());
        model.addAttribute("alDia", cuentas.stream().filter(c -> !c.debe()).toList());
        model.addAttribute("totalEnLaCalle", servicio.totalEnLaCalle());
        model.addAttribute("nombres", servicio.nombresDeClientes());
        if (!model.containsAttribute("fiadoForm")) {
            model.addAttribute("fiadoForm", new FiadoForm());
        }
    }

    private void cargarCliente(Model model, Long id) {
        CuentaCliente cuenta = servicio.cuenta(id);
        model.addAttribute("titulo", cuenta.cliente().getNombre());
        model.addAttribute("cuenta", cuenta);
        model.addAttribute("libreta", servicio.libreta(id));
        model.addAttribute("historial", servicio.historial(id));
        if (!model.containsAttribute("fiadoForm")) {
            model.addAttribute("fiadoForm", new FiadoForm());
        }
    }
}
