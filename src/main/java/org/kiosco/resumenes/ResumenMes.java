package org.kiosco.resumenes;

import org.kiosco.caja.Totales;

import java.time.YearMonth;

/** Un renglón del resumen mes por mes; el mes en curso muestra "lo que va". */
public record ResumenMes(YearMonth mes, Totales totales, boolean enCurso) {
}
