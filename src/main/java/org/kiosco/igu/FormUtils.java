package org.kiosco.igu;

import javax.swing.*;

/**
 * Validaciones compartidas por los formularios Swing: evita que datos mal
 * tipeados terminen en un stack trace en vez de un mensaje claro al usuario.
 */
final class FormUtils {

    private FormUtils() {
    }

    static class ValidationException extends RuntimeException {
        ValidationException(String mensaje) {
            super(mensaje);
        }
    }

    static String requireTexto(JTextField campo, String nombreCampo) {
        String valor = campo.getText().trim();
        if (valor.isEmpty()) {
            throw new ValidationException(nombreCampo + " no puede estar vacío.");
        }
        return valor;
    }

    static int parseEntero(JTextField campo, String nombreCampo) {
        try {
            return Integer.parseInt(campo.getText().trim());
        } catch (NumberFormatException e) {
            throw new ValidationException(nombreCampo + " debe ser un número entero válido.");
        }
    }

    static int parseEnteroPositivo(JTextField campo, String nombreCampo) {
        int valor = parseEntero(campo, nombreCampo);
        if (valor <= 0) {
            throw new ValidationException(nombreCampo + " debe ser mayor que cero.");
        }
        return valor;
    }

    static double parseDecimal(JTextField campo, String nombreCampo) {
        try {
            return Double.parseDouble(campo.getText().trim());
        } catch (NumberFormatException e) {
            throw new ValidationException(nombreCampo + " debe ser un número válido.");
        }
    }

    static double parsePositivo(JTextField campo, String nombreCampo) {
        double valor = parseDecimal(campo, nombreCampo);
        if (valor <= 0) {
            throw new ValidationException(nombreCampo + " debe ser mayor que cero.");
        }
        return valor;
    }

    static void manejarError(JFrame origen, Exception ex) {
        if (ex instanceof ValidationException) {
            JOptionPane.showMessageDialog(origen, ex.getMessage(), "Datos inválidos", JOptionPane.WARNING_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(origen, "Ocurrió un error inesperado: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
