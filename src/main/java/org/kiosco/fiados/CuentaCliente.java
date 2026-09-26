package org.kiosco.fiados;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Cómo está la cuenta de un cliente.
 *
 * @param desde      fecha del renglón más viejo sin saldar (null si está al día)
 * @param ultimoPago última vez que pagó algo de la cuenta abierta (null si todavía no pagó)
 */
public record CuentaCliente(Cliente cliente, BigDecimal saldo, LocalDate desde, LocalDate ultimoPago) {

    static CuentaCliente de(Cliente cliente, MovimientoFiadoRepository.SaldoAbierto abierto) {
        return abierto == null
                ? new CuentaCliente(cliente, BigDecimal.ZERO, null, null)
                : new CuentaCliente(cliente, abierto.getSaldo(), abierto.getDesde(), abierto.getUltimoPago());
    }

    public boolean debe() {
        return saldo.signum() > 0;
    }
}
