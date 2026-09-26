package org.kiosco.hoy;

import org.kiosco.caja.CajaService;
import org.kiosco.caja.MovimientoCaja;
import org.kiosco.cierre.CierreDiarioService;
import org.kiosco.comun.DatoInvalidoException;
import org.kiosco.copias.CopiaDeSeguridadService;
import org.kiosco.comun.Formatos;
import org.kiosco.comun.MontoInvalidoException;
import org.kiosco.comun.Montos;
import org.kiosco.pedidos.Pedido;
import org.kiosco.pedidos.PedidoService;
import org.kiosco.pedidos.Proveedor;
import org.kiosco.productos.Producto;
import org.kiosco.productos.ProductoService;
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
import java.util.regex.Pattern;

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

    /** EAN-8, UPC-A, EAN-13…: nadie escribe un monto de 8 dígitos o más sin puntos. */
    private static final Pattern CODIGO_DE_BARRAS = Pattern.compile("\\d{8,14}");

    private final CajaService caja;
    private final CierreDiarioService cierres;
    private final PedidoService pedidos;
    private final ProductoService productos;
    private final CopiaDeSeguridadService copias;
    private final Formatos fmt;
    private final Clock clock;

    HoyController(CajaService caja, CierreDiarioService cierres, PedidoService pedidos, ProductoService productos,
                  CopiaDeSeguridadService copias, Formatos fmt, Clock clock) {
        this.caja = caja;
        this.cierres = cierres;
        this.pedidos = pedidos;
        this.productos = productos;
        this.copias = copias;
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
            if (!form.tieneCodigo() && CODIGO_DE_BARRAS.matcher(textoOVacio(form.getMonto())).matches()
                    && productos.existeCodigo(form.getMonto().strip())) {
                // Se escaneó un código de barras en el campo del monto (donde está el cursor)
                form.setCodigo(form.getMonto().strip());
                form.setMonto(null);
            }
            if (form.tieneCodigo()) {
                // Con código: sin monto se cobra el precio de lista y se descuenta el stock
                BigDecimal monto = form.getMonto() == null || form.getMonto().isBlank() ? null : Montos.parse(form.getMonto());
                ProductoService.VentaDeProducto venta = productos.vender(form.getCodigo(), leerCantidad(form.getCantidad()),
                        dia, monto, form.getDescripcion());
                model.addAttribute("avisoVenta", avisoDeVenta(venta));
            } else {
                BigDecimal monto = Montos.parse(form.getMonto());
                caja.registrarVenta(dia, monto, form.getDescripcion());
                model.addAttribute("avisoVenta", "Venta de " + fmt.plata(monto) + " anotada");
            }
            model.addAttribute("ventaForm", new VentaForm());
            ok = true;
        } catch (DatoInvalidoException e) {
            model.addAttribute("errorVenta", e.getMessage());
            ok = false;
        }
        return responder(dia, htmx != null, ok, model);
    }

    private static String textoOVacio(String texto) {
        return texto == null ? "" : texto.strip();
    }

    private static int leerCantidad(String texto) {
        if (texto == null || texto.isBlank()) {
            return 1;
        }
        try {
            return Integer.parseInt(texto.strip());
        } catch (NumberFormatException e) {
            throw new DatoInvalidoException("La cantidad tiene que ser un número entero, por ejemplo 2.");
        }
    }

    /** "Coca-Cola 2,25 L: $ 3.200 anotados. Quedan 11." */
    private String avisoDeVenta(ProductoService.VentaDeProducto venta) {
        Producto producto = venta.producto();
        String aviso = venta.venta().getDetalle() + ": " + fmt.plata(venta.venta().getMonto()) + " anotados.";
        if (!producto.isControlaStock()) {
            return aviso;
        }
        if (producto.getStock() < 0) {
            return aviso + " Ojo: el stock quedó en " + producto.getStock() + ", revisalo en Productos.";
        }
        return aviso + (producto.getStock() == 1 ? " Queda 1." : " Quedan " + producto.getStock() + ".");
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
        // El código de producto solo aparece si se cargó alguno: la caja funciona igual sin productos
        model.addAttribute("productos", productos.buscar(""));
        CopiaDeSeguridadService.Resultado ultimaCopia = copias.ultimoResultado();
        model.addAttribute("copiaFallida", ultimaCopia != null && !ultimaCopia.ok() ? ultimaCopia.mensaje() : null);
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
