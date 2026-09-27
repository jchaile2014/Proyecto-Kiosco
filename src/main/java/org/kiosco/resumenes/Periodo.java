package org.kiosco.resumenes;

import org.kiosco.caja.Totales;
import org.kiosco.comun.MontoPorNombre;

import java.time.LocalDate;
import java.util.List;

/**
 * Un rango de días con sus totales, día por día y en conjunto.
 *
 * @param pedidosPorProveedor lo pagado a cada uno, juntando las boletas de pedidos y lo pagado
 *                            en el momento (el panadero, un pedido que no se anotó…)
 */
public record Periodo(LocalDate desde, LocalDate hasta, List<Dia> dias, Totales totales,
                      List<MontoPorNombre> pedidosPorProveedor) {

    public record Dia(LocalDate fecha, Totales totales) {
    }
}
