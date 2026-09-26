package org.kiosco.pedidos;

import org.kiosco.comun.DatoInvalidoException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;

@Controller
class ProveedoresController {

    private final PedidoService servicio;

    ProveedoresController(PedidoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/proveedores/nuevo")
    String nuevo(Model model) {
        return formulario(model, null, new ProveedorForm());
    }

    @GetMapping("/proveedores/{id}")
    String editar(@PathVariable Long id, Model model) {
        return formulario(model, id, ProveedorForm.de(servicio.proveedor(id)));
    }

    @PostMapping("/proveedores")
    String crear(@ModelAttribute("proveedorForm") ProveedorForm form, Model model, RedirectAttributes redirect) {
        return guardar(null, form, model, redirect);
    }

    @PostMapping("/proveedores/{id}")
    String actualizar(@PathVariable Long id, @ModelAttribute("proveedorForm") ProveedorForm form,
                      Model model, RedirectAttributes redirect) {
        return guardar(id, form, model, redirect);
    }

    @PostMapping("/proveedores/{id}/baja")
    String darDeBaja(@PathVariable Long id, RedirectAttributes redirect) {
        String nombre = servicio.proveedor(id).getNombre();
        servicio.darDeBaja(id);
        redirect.addFlashAttribute("aviso", nombre + " ya no aparece en tu lista de proveedores.");
        return "redirect:/pedidos";
    }

    private String guardar(Long id, ProveedorForm form, Model model, RedirectAttributes redirect) {
        try {
            Proveedor proveedor = servicio.guardarProveedor(id, form.getNombre(), form.getTelefono(), form.getDias());
            redirect.addFlashAttribute("aviso", id == null
                    ? proveedor.getNombre() + " agregado a tus proveedores."
                    : "Cambios de " + proveedor.getNombre() + " guardados.");
            return "redirect:/pedidos";
        } catch (DatoInvalidoException e) {
            model.addAttribute("error", e.getMessage());
            return formulario(model, id, form);
        }
    }

    private String formulario(Model model, Long id, ProveedorForm form) {
        model.addAttribute("titulo", id == null ? "Nuevo proveedor" : "Editar proveedor");
        model.addAttribute("proveedorId", id);
        model.addAttribute("proveedorForm", form);
        model.addAttribute("diasSemana", DayOfWeek.values());
        return "proveedor-form";
    }
}
