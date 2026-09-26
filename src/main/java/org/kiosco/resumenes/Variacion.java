package org.kiosco.resumenes;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Cuánto cambió algo contra el período anterior, en %. */
public record Variacion(long porcentaje) {

    /** Null si no hay con qué comparar (el período anterior fue cero o negativo). */
    static Variacion entre(BigDecimal actual, BigDecimal anterior) {
        if (anterior.signum() <= 0) {
            return null;
        }
        BigDecimal cambio = actual.subtract(anterior).multiply(BigDecimal.valueOf(100))
                .divide(anterior, 0, RoundingMode.HALF_UP);
        return new Variacion(cambio.longValue());
    }

    public boolean sube() {
        return porcentaje > 0;
    }

    public boolean baja() {
        return porcentaje < 0;
    }

    /** "+12 %", "−5 %" o "Igual". */
    public String texto() {
        if (porcentaje == 0) {
            return "Igual";
        }
        return (porcentaje > 0 ? "+" : "−") + Math.abs(porcentaje) + " %";
    }
}
