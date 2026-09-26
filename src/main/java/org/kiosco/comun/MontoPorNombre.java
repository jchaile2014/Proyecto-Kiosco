package org.kiosco.comun;

import java.math.BigDecimal;

/** Un total con su nombre: "Panadero — $ 84.000", "Coca-Cola — $ 150.000". */
public record MontoPorNombre(String nombre, BigDecimal monto) {
}
