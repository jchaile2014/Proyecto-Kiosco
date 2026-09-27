package org.kiosco.resumenes;

import org.kiosco.caja.Totales;
import org.kiosco.comun.Formatos;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Controller
class ResumenesController {

    private static final int MESES_EN_EL_GRAFICO = 12;

    private final ResumenService resumenes;
    private final Formatos fmt;
    private final Clock clock;

    ResumenesController(ResumenService resumenes, Formatos fmt, Clock clock) {
        this.resumenes = resumenes;
        this.fmt = fmt;
        this.clock = clock;
    }

    @GetMapping("/resumenes")
    String resumenes(Model model) {
        Periodo semana = resumenes.ultimos7Dias();
        Totales anterior = resumenes.semanaAnterior();
        model.addAttribute("hoy", LocalDate.now(clock));
        model.addAttribute("semana", semana);
        model.addAttribute("variacionVentas", Variacion.entre(semana.totales().ventas(), anterior.ventas()));
        model.addAttribute("graficoSemana", graficoPorDia("Vendido por día en los últimos 7 días", semana, true));

        List<ResumenMes> meses = resumenes.meses();
        model.addAttribute("meses", meses);
        model.addAttribute("graficoMeses", graficoPorMes(meses));
        return "resumenes";
    }

    @GetMapping("/resumenes/{anio}/{mes}")
    String mes(@PathVariable int anio, @PathVariable int mes, Model model) {
        YearMonth actual = YearMonth.now(clock);
        YearMonth elegido;
        try {
            elegido = YearMonth.of(anio, mes);
        } catch (DateTimeException e) {
            return "redirect:/resumenes";
        }
        if (elegido.isAfter(actual)) {
            return "redirect:/resumenes";
        }
        Periodo periodo = resumenes.periodo(elegido.atDay(1), elegido.atEndOfMonth());
        boolean enCurso = elegido.equals(actual);

        model.addAttribute("hoy", LocalDate.now(clock));
        model.addAttribute("titulo", fmt.mes(elegido));
        model.addAttribute("mes", elegido);
        model.addAttribute("enCurso", enCurso);
        model.addAttribute("periodo", periodo);
        model.addAttribute("grafico", graficoPorDia("Vendido por día en " + fmt.mes(elegido), periodo, false));
        model.addAttribute("mesAnterior", elegido.minusMonths(1));
        model.addAttribute("mesSiguiente", enCurso ? null : elegido.plusMonths(1));
        return "resumen-mes";
    }

    /** Una sola serie: lo vendido cada día, con la barra del mejor día marcada. */
    private GraficoBarras graficoPorDia(String descripcion, Periodo periodo, boolean conDiaDeLaSemana) {
        List<GraficoBarras.Columna> columnas = periodo.dias().stream()
                .map(d -> new GraficoBarras.Columna(
                        conDiaDeLaSemana ? fmt.diaYNumero(d.fecha()) : String.valueOf(d.fecha().getDayOfMonth()),
                        fmt.fechaLarga(d.fecha()),
                        "/?fecha=" + d.fecha(),
                        List.of(d.totales().ventas())))
                .toList();
        return GraficoBarras.armar(descripcion, List.of("Vendido"), columnas, GraficoBarras.Destacar.MAXIMO, fmt);
    }

    /** Vendido contra pedidos (todo lo pagado a proveedores), los últimos 12 meses de más viejo a más nuevo. */
    private GraficoBarras graficoPorMes(List<ResumenMes> meses) {
        List<GraficoBarras.Columna> columnas = meses.stream()
                .limit(MESES_EN_EL_GRAFICO)
                .sorted((a, b) -> a.mes().compareTo(b.mes()))
                .map(m -> new GraficoBarras.Columna(
                        fmt.mes(m.mes()).substring(0, 3) + " " + m.mes().getYear(),
                        fmt.mes(m.mes()) + (m.enCurso() ? " (lo que va)" : ""),
                        "/resumenes/" + m.mes().getYear() + "/" + m.mes().getMonthValue(),
                        List.of(m.totales().ventas(), m.totales().pedidos())))
                .toList();
        return GraficoBarras.armar("Vendido y pedidos, mes por mes", List.of("Vendido", "Pedidos"),
                columnas, GraficoBarras.Destacar.ULTIMA_COLUMNA, fmt);
    }
}
