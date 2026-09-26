package org.kiosco.resumenes;

import org.kiosco.caja.Totales;
import org.kiosco.comun.MontoPorNombre;

import java.time.LocalDate;
import java.util.List;

/** Un rango de días con sus totales, día por día y en conjunto. */
public record Periodo(LocalDate desde, LocalDate hasta, List<Dia> dias, Totales totales,
                      List<MontoPorNombre> gastosPorCategoria, List<MontoPorNombre> pagadoPorProveedor) {

    public record Dia(LocalDate fecha, Totales totales) {
    }
}
