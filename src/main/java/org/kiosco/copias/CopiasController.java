package org.kiosco.copias;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
class CopiasController {

    private final CopiaDeSeguridadService copias;

    CopiasController(CopiaDeSeguridadService copias) {
        this.copias = copias;
    }

    @GetMapping("/copias")
    String pagina(Model model) {
        model.addAttribute("disponible", copias.disponible());
        model.addAttribute("carpeta", copias.carpeta().toString());
        model.addAttribute("copias", copias.copias());
        model.addAttribute("ultimo", copias.ultimoResultado());
        return "copias";
    }

    @PostMapping("/copias")
    String hacerAhora(RedirectAttributes redirect) {
        try {
            CopiaDeSeguridadService.Copia copia = copias.hacerCopia();
            redirect.addFlashAttribute("aviso", "Copia guardada: " + copia.archivo().getFileName());
        } catch (CopiaFallidaException e) {
            redirect.addFlashAttribute("errorCopia", e.getMessage());
        }
        return "redirect:/copias";
    }
}
