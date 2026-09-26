package org.kiosco.fiados;

import java.math.BigDecimal;

/** Un renglón con lo que "va debiendo" después de él, como la suma al margen de una libreta de papel. */
public record RenglonLibreta(MovimientoFiado movimiento, BigDecimal saldo) {
}
