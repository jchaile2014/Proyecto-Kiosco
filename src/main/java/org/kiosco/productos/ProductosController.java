package org.kiosco.productos;

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

import java.util.List;

@Controller
class ProductosController {

    private final ProductoService servicio;
    private final Formatos fmt;

    ProductosController(ProductoService servicio, Formatos fmt) {
        this.servicio = servicio;
        this.fmt = fmt;
    }

    /** La búsqueda se actualiza mientras se escribe (HTMX toma solo el panel de esta misma página). */
    @GetMapping("/productos")
    String lista(@RequestParam(defaultValue = "") String q, Model model) {
        cargarLista(model, q);
        return "productos";
    }

    @PostMapping("/productos/{id}/sumar")
    String sumarStock(@PathVariable Long id, @RequestParam(required = false) String cantidad,
                      @RequestParam(defaultValue = "") String q,
                      @RequestHeader(name = "HX-Request", required = false) String htmx,
                      Model model, RedirectAttributes redirect) {
        try {
            int entraron = leerEntero(cantidad, "La cantidad");
            Producto producto = servicio.sumarStock(id, entraron);
            String aviso = "Entraron " + entraron + " de " + producto.getNombre() + ". Ahora hay " + producto.getStock() + ".";
            if (htmx == null) {
                redirect.addFlashAttribute("aviso", aviso);
                return "redirect:/productos";
            }
            model.addAttribute("aviso", aviso);
        } catch (DatoInvalidoException e) {
            model.addAttribute("errorSumar", e.getMessage());
            model.addAttribute("productoConError", id);
        }
        cargarLista(model, q);
        return "productos :: panel";
    }

    @GetMapping("/productos/nuevo")
    String nuevo(Model model) {
        ProductoForm form = new ProductoForm();
        form.setControlaStock(false);
        return formulario(model, null, form);
    }

    @GetMapping("/productos/{id}")
    String editar(@PathVariable Long id, Model model) {
        Producto producto = servicio.producto(id);
        model.addAttribute("historial", servicio.historialDeStock(id));
        return formulario(model, id, ProductoForm.de(producto, fmt.plata(producto.getPrecio()).replace("$ ", "")));
    }

    @PostMapping("/productos")
    String crear(@ModelAttribute("productoForm") ProductoForm form, Model model, RedirectAttributes redirect) {
        return guardar(null, form, model, redirect);
    }

    @PostMapping("/productos/{id}")
    String actualizar(@PathVariable Long id, @ModelAttribute("productoForm") ProductoForm form,
                      Model model, RedirectAttributes redirect) {
        return guardar(id, form, model, redirect);
    }

    @PostMapping("/productos/{id}/baja")
    String darDeBaja(@PathVariable Long id, RedirectAttributes redirect) {
        String nombre = servicio.producto(id).getNombre();
        servicio.darDeBaja(id);
        redirect.addFlashAttribute("aviso", nombre + " ya no aparece en la lista de productos.");
        return "redirect:/productos";
    }

    private String guardar(Long id, ProductoForm form, Model model, RedirectAttributes redirect) {
        try {
            Producto producto = servicio.guardar(id, form.getCodigo(), form.getNombre(), Montos.parse(form.getPrecio()),
                    form.isControlaStock(), leerEnteroOpcional(form.getStock(), "El stock"),
                    leerEnteroOpcional(form.getStockMinimo(), "El stock mínimo"));
            redirect.addFlashAttribute("aviso", id == null
                    ? producto.getNombre() + " agregado a la lista."
                    : "Cambios de " + producto.getNombre() + " guardados.");
            return "redirect:/productos";
        } catch (DatoInvalidoException e) {
            model.addAttribute("error", e.getMessage());
            if (id != null) {
                model.addAttribute("historial", servicio.historialDeStock(id));
            }
            return formulario(model, id, form);
        }
    }

    private String formulario(Model model, Long id, ProductoForm form) {
        model.addAttribute("titulo", id == null ? "Nuevo producto" : "Editar producto");
        model.addAttribute("productoId", id);
        model.addAttribute("productoForm", form);
        if (!model.containsAttribute("historial")) {
            model.addAttribute("historial", List.of());
        }
        return "producto-form";
    }

    private void cargarLista(Model model, String busqueda) {
        List<Producto> productos = servicio.buscar(busqueda);
        List<Producto> pocoStock = servicio.conPocoStock();
        model.addAttribute("busqueda", busqueda);
        model.addAttribute("productos", productos);
        model.addAttribute("pocoStock", pocoStock);
        model.addAttribute("avisoPocoStock", pocoStock.isEmpty() ? null
                : "Queda poco de " + fmt.enumerar(pocoStock.stream().map(Producto::getNombre).toList()) + ".");
    }

    private static int leerEntero(String texto, String campo) {
        Integer valor = leerEnteroOpcional(texto, campo);
        if (valor == null) {
            throw new DatoInvalidoException(campo + " no puede quedar vacía.");
        }
        return valor;
    }

    private static Integer leerEnteroOpcional(String texto, String campo) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(texto.strip().replace(".", ""));
        } catch (NumberFormatException e) {
            throw new DatoInvalidoException(campo + " tiene que ser un número entero, por ejemplo 12.");
        }
    }
}
