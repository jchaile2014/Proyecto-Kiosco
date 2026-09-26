package org.kiosco.hoy;

import org.kiosco.caja.CajaService;
import org.kiosco.caja.MovimientoCaja;
import org.kiosco.cierre.CierreDiarioService;
import org.kiosco.comun.DatoInvalidoException;
import org.kiosco.comun.Formatos;
import org.kiosco.comun.MontoInvalidoException;
import org.kiosco.comun.Montos;
import org.kiosco.pedidos.Pedido;
import org.kiosco.pedidos.PedidoService;
import org.kiosco.pedidos.Proveedor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

/**
 * Pantalla principal: la caja de un día. Por defecto muestra hoy; con ?fecha= se puede
 * mirar (y corregir) un día anterior.
 *
 * <p>Los formularios funcionan con HTMX: cuando el pedido viene de HTMX se devuelve solo
 * el panel del día para reemplazarlo sin recargar la página.
 */
@Controller
class HoyController {

    private final CajaService caja;
    private final CierreDiarioService cierres;
    private final PedidoService pedidos;
    private final Formatos fmt;
    private final Clock clock;

    HoyController(CajaService caja, CierreDiarioService cierres, PedidoService pedidos, Formatos fmt, Clock clock) {
        this.caja = caja;
        this.cierres = cierres;
        this.pedidos = pedidos;
        this.fmt = fmt;
        this.clock = clock;
    }

    @GetMapping("/")
    String dia(@RequestParam(required = false) @DateTimeFormat(iso = DATE) LocalDate fecha, Model model) {
        LocalDate hoy = LocalDate.now(clock);
        cargarDia(model, fecha == null || fecha.isAfter(hoy) ? hoy : fecha);
        return "hoy";
    }

    @PostMapping("/movimientos/venta")
    String anotarVenta(@RequestParam(required = false) @DateTimeFormat(iso = DATE) LocalDate fecha,
                       @ModelAttribute("ventaForm") VentaForm form,
                       @RequestHeader(name = "HX-Request", required = false) String htmx,
                       Model model) {
        LocalDate dia = fecha != null ? fecha : LocalDate.now(clock);
        boolean ok;
        try {
            BigDecimal monto = Montos.parse(form.getMonto());
            caja.registrarVenta(dia, monto, form.getDescripcion());
            model.addAttribute("ventaForm", new VentaForm());
            model.addAttribute("avisoVenta", "Venta de " + fmt.plata(monto) + " anotada");
            ok = true;
        } catch (DatoInvalidoException e) {
            model.addAttribute("errorVenta", e.getMessage());
            ok = false;
        }
        return responder(dia, htmx != null, ok, model);
    }

    @PostMapping("/movimientos/gasto")
    String anotarGasto(@RequestParam(required = false) @DateTimeFormat(iso = DATE) LocalDate fecha,
                       @ModelAttribute("gastoForm") GastoForm form,
                       @RequestHeader(name = "HX-Request", required = false) String htmx,
                       Model model) {
        LocalDate dia = fecha != null ? fecha : LocalDate.now(clock);
        boolean ok;
        try {
            BigDecimal monto = Montos.parse(form.getMonto());
            MovimientoCaja gasto = caja.registrarGasto(dia, monto, form.getCategoria(), form.getDescripcion());
            model.addAttribute("gastoForm", new GastoForm());
            model.addAttribute("avisoGasto", gasto.getCategoria() + ": " + fmt.plata(monto) + " anotado");
            ok = true;
        } catch (DatoInvalidoException e) {
            model.addAttribute("errorGasto", e.getMessage());
            model.addAttribute("campoConError", e instanceof MontoInvalidoException ? "monto"
                    : form.getCategoria() == null || form.getCategoria().isBlank() ? "categoria" : null);
            ok = false;
        }
        return responder(dia, htmx != null, ok, model);
    }

    @PostMapping("/movimientos/{id}/eliminar")
    String eliminar(@PathVariable Long id,
                    @RequestHeader(name = "HX-Request", required = false) String htmx,
                    Model model) {
        LocalDate dia = caja.eliminar(id);
        return responder(dia, htmx != null, true, model);
    }

    /** "Llegó" desde la caja del día: registra el pedido y actualiza lo que salió hoy. */
    @PostMapping("/hoy/pedidos/{id}/llego")
    String llegoPedido(@PathVariable Long id, @RequestParam(required = false) String montoBoleta,
                       @RequestHeader(name = "HX-Request", required = false) String htmx,
                       Model model) {
        boolean ok;
        try {
            BigDecimal monto = Montos.parse(montoBoleta);
            Pedido pedido = pedidos.registrarLlegada(id, monto);
            model.addAttribute("avisoPedido", "Llegó el pedido de " + pedido.getProveedor().getNombre()
                    + ": " + fmt.plata(monto) + " anotados como salida.");
            ok = true;
        } catch (DatoInvalidoException e) {
            model.addAttribute("errorLlegada", e.getMessage());
            model.addAttribute("pedidoConError", id);
            model.addAttribute("montoIngresado", montoBoleta);
            ok = false;
        }
        return responder(LocalDate.now(clock), htmx != null, ok, model);
    }

    private String responder(LocalDate dia, boolean htmx, boolean ok, Model model) {
        if (!htmx && ok) {
            return dia.equals(LocalDate.now(clock)) ? "redirect:/" : "redirect:/?fecha=" + dia;
        }
        cargarDia(model, dia);
        return htmx ? "hoy :: panel" : "hoy";
    }

    private void cargarDia(Model model, LocalDate dia) {
        LocalDate hoy = LocalDate.now(clock);
        boolean esHoy = dia.equals(hoy);

        model.addAttribute("titulo", esHoy ? "Hoy" : fmt.fechaLarga(dia));
        model.addAttribute("fecha", dia);
        model.addAttribute("hoy", hoy);
        model.addAttribute("esHoy", esHoy);
        model.addAttribute("diaAnterior", dia.minusDays(1));
        model.addAttribute("diaSiguiente", esHoy ? null : dia.plusDays(1));
        model.addAttribute("cierre", cierres.cierreDel(dia).orElse(null));
        model.addAttribute("totales", caja.totalesDel(dia));
        model.addAttribute("movimientos", caja.movimientosDel(dia));
        model.addAttribute("categorias", caja.categoriasDeGasto());
        // Los avisos de pedidos solo tienen sentido en la caja de hoy
        model.addAttribute("pedidosParaHoy", esHoy ? pedidos.pendientesParaHoy() : List.of());
        model.addAttribute("avisoPreventistas", esHoy ? avisoPreventistas(pedidos.quienesVienenHoy()) : null);
        if (!model.containsAttribute("ventaForm")) {
            model.addAttribute("ventaForm", new VentaForm());
        }
        if (!model.containsAttribute("gastoForm")) {
            model.addAttribute("gastoForm", new GastoForm());
        }
    }

    /** "Hoy viene el preventista de Coca-Cola (2 cosas anotadas) y Lácteos." */
    private String avisoPreventistas(List<Proveedor> vienen) {
        if (vienen.isEmpty()) {
            return null;
        }
        List<String> partes = vienen.stream().map(p -> switch (p.getNotas().size()) {
            case 0 -> p.getNombre();
            case 1 -> p.getNombre() + " (1 cosa anotada)";
            default -> p.getNombre() + " (" + p.getNotas().size() + " cosas anotadas)";
        }).toList();
        return (vienen.size() == 1 ? "Hoy viene el preventista de " : "Hoy vienen los preventistas de ")
                + fmt.enumerar(partes) + ".";
    }
}
