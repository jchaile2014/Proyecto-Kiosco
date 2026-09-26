package org.kiosco.fiados;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Una cuenta que el cliente terminó de pagar, con sus renglones. */
public record CuentaSaldada(LocalDateTime saldadaEn, List<MovimientoFiado> renglones) {

    public BigDecimal totalPagado() {
        return renglones.stream()
                .filter(r -> r.getTipo() == TipoMovimientoFiado.PAGO)
                .map(MovimientoFiado::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
