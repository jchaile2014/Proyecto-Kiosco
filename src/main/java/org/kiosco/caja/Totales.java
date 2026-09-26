package org.kiosco.caja;

import java.math.BigDecimal;
import java.util.List;

/** Cuánto entró y salió en un período, separado por origen. */
public record Totales(BigDecimal ventas, BigDecimal cobrosFiado, BigDecimal pagosPedidos, BigDecimal gastos) {

    public static final Totales CERO = new Totales(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

    public static Totales desde(List<MovimientoCajaRepository.TotalPorTipo> filas) {
        Totales t = CERO;
        for (var fila : filas) {
            BigDecimal total = fila.getTotal();
            t = switch (fila.getTipo()) {
                case VENTA -> new Totales(total, t.cobrosFiado, t.pagosPedidos, t.gastos);
                case COBRO_FIADO -> new Totales(t.ventas, total, t.pagosPedidos, t.gastos);
                case PAGO_PEDIDO -> new Totales(t.ventas, t.cobrosFiado, total, t.gastos);
                case GASTO -> new Totales(t.ventas, t.cobrosFiado, t.pagosPedidos, total);
            };
        }
        return t;
    }

    public Totales mas(Totales otro) {
        return new Totales(ventas.add(otro.ventas), cobrosFiado.add(otro.cobrosFiado),
                pagosPedidos.add(otro.pagosPedidos), gastos.add(otro.gastos));
    }

    public BigDecimal entradas() {
        return ventas.add(cobrosFiado);
    }

    public BigDecimal salidas() {
        return pagosPedidos.add(gastos);
    }

    public BigDecimal balance() {
        return entradas().subtract(salidas());
    }
}
