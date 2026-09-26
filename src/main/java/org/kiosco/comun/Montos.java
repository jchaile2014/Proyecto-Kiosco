package org.kiosco.comun;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Convierte lo que se tipea en el campo "monto" a {@link BigDecimal}, aceptando
 * la forma argentina de escribir plata: "1500", "1.500", "1.500,50", "$ 2.300" o "1500.5".
 */
public final class Montos {

    public static final BigDecimal MAXIMO = new BigDecimal("9999999999.99");

    private static final Pattern SOLO_NUMEROS = Pattern.compile("[0-9.,]+");
    private static final Pattern MILES_CON_PUNTO = Pattern.compile("\\d{1,3}(\\.\\d{3})+");
    private static final Pattern ENTERO = Pattern.compile("\\d+");
    private static final Pattern DECIMALES = Pattern.compile("\\d{1,2}");

    private Montos() {
    }

    public static BigDecimal parse(String texto) {
        String limpio = texto == null ? "" : texto.replace("$", "").replaceAll("[\\s\\u00A0]", "");
        if (limpio.isEmpty()) {
            throw new MontoInvalidoException("Ingresá un monto.");
        }
        if (limpio.startsWith("-")) {
            throw new MontoInvalidoException("El monto tiene que ser mayor a cero.");
        }
        if (!SOLO_NUMEROS.matcher(limpio).matches()) {
            throw new MontoInvalidoException("Escribí solo números, por ejemplo 1500 o 1.500,50.");
        }

        BigDecimal monto = new BigDecimal(normalizar(limpio));
        if (monto.signum() <= 0) {
            throw new MontoInvalidoException("El monto tiene que ser mayor a cero.");
        }
        if (monto.compareTo(MAXIMO) > 0) {
            throw new MontoInvalidoException("Ese monto es demasiado grande.");
        }
        return monto;
    }

    /** Devuelve el número con punto decimal y sin separador de miles, listo para BigDecimal. */
    private static String normalizar(String s) {
        int comas = contar(s, ',');
        int puntos = contar(s, '.');

        if (comas > 1) {
            throw formatoInvalido();
        }
        if (comas == 1) {
            String entera = s.substring(0, s.indexOf(','));
            String decimal = s.substring(s.indexOf(',') + 1);
            boolean enteraValida = ENTERO.matcher(entera).matches() || MILES_CON_PUNTO.matcher(entera).matches();
            if (!enteraValida || !DECIMALES.matcher(decimal).matches()) {
                throw formatoInvalido();
            }
            return entera.replace(".", "") + "." + decimal;
        }
        if (puntos == 0) {
            return s;
        }
        if (puntos == 1) {
            String decimal = s.substring(s.indexOf('.') + 1);
            // "1.500" es mil quinientos; "1500.5" o "12.50" llevan centavos
            if (decimal.length() == 3 && MILES_CON_PUNTO.matcher(s).matches()) {
                return s.replace(".", "");
            }
            if (DECIMALES.matcher(decimal).matches() && s.indexOf('.') > 0) {
                return s;
            }
            throw formatoInvalido();
        }
        if (MILES_CON_PUNTO.matcher(s).matches()) {
            return s.replace(".", "");
        }
        throw formatoInvalido();
    }

    private static int contar(String s, char c) {
        return (int) s.chars().filter(ch -> ch == c).count();
    }

    private static MontoInvalidoException formatoInvalido() {
        return new MontoInvalidoException("No entiendo ese monto. Probá con 1500 o 1.500,50.");
    }
}
