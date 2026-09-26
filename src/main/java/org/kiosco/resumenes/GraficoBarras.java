package org.kiosco.resumenes;

import org.kiosco.comun.Formatos;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Lo que necesita la plantilla para dibujar un gráfico de columnas con HTML y CSS: la
 * escala ya calculada (alturas en %), las líneas de la grilla con valores redondos y las
 * etiquetas. Todo se arma en el servidor; no hace falta ninguna librería de gráficos.
 *
 * @param series nombres de las series, en orden fijo (la serie 1 siempre es "Vendido")
 */
public record GraficoBarras(String descripcion, List<String> series, List<Grupo> grupos, List<Linea> lineas) {

    private static final BigDecimal PASO_MINIMO = new BigDecimal("100");

    /** Una columna del gráfico (un día o un mes) con una barra por serie. */
    public record Grupo(String etiqueta, String titulo, String url, List<Barra> barras) {

        /** Para lectores de pantalla: "Lunes 21: Vendido $ 85.300". */
        public String descripcionAccesible() {
            return titulo + ": " + barras.stream().map(b -> b.nombreSerie() + " " + b.monto())
                    .collect(Collectors.joining(", "));
        }
    }

    /**
     * @param serie    número de serie (1, 2…), elige el color
     * @param altura   porcentaje del alto del gráfico
     * @param etiqueta valor escrito sobre la barra, solo en las barras que se destacan
     */
    public record Barra(int serie, String nombreSerie, String monto, double altura, String etiqueta) {
    }

    public record Linea(double altura, String texto) {
    }

    /** Datos de entrada de una columna: un valor por serie. */
    public record Columna(String etiqueta, String titulo, String url, List<BigDecimal> valores) {
    }

    /** Qué barras llevan el valor escrito arriba: pocas, para que se lean. */
    public enum Destacar {
        /** La barra más alta (gráficos de una sola serie). */
        MAXIMO,
        /**
         * La primera serie de la última columna (por ejemplo, lo vendido en el mes en curso).
         * Solo una: con barras pegadas, dos etiquetas se pisan.
         */
        ULTIMA_COLUMNA
    }

    public static GraficoBarras armar(String descripcion, List<String> series, List<Columna> columnas,
                                      Destacar destacar, Formatos fmt) {
        BigDecimal maximo = columnas.stream().flatMap(c -> c.valores().stream())
                .max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
        BigDecimal paso = pasoRedondo(maximo);
        BigDecimal tope = maximo.signum() <= 0 ? paso : maximo.divide(paso, 0, RoundingMode.CEILING).multiply(paso);

        List<Linea> lineas = new ArrayList<>();
        for (BigDecimal valor = BigDecimal.ZERO; valor.compareTo(tope) <= 0; valor = valor.add(paso)) {
            lineas.add(new Linea(altura(valor, tope), fmt.plataCorta(valor)));
        }

        List<Grupo> grupos = new ArrayList<>();
        boolean maximoYaMarcado = false;
        for (int i = 0; i < columnas.size(); i++) {
            Columna columna = columnas.get(i);
            List<Barra> barras = new ArrayList<>();
            for (int s = 0; s < series.size(); s++) {
                BigDecimal valor = columna.valores().get(s);
                boolean etiquetar = valor.signum() > 0 && switch (destacar) {
                    case MAXIMO -> !maximoYaMarcado && valor.compareTo(maximo) == 0;
                    case ULTIMA_COLUMNA -> i == columnas.size() - 1 && s == 0;
                };
                maximoYaMarcado |= etiquetar && destacar == Destacar.MAXIMO;
                barras.add(new Barra(s + 1, series.get(s), fmt.plata(valor), altura(valor, tope),
                        etiquetar ? fmt.plataCorta(valor) : null));
            }
            grupos.add(new Grupo(columna.etiqueta(), columna.titulo(), columna.url(), barras));
        }
        return new GraficoBarras(descripcion, series, grupos, lineas);
    }

    /** Divide el eje en unas 4 partes con números redondos: 1, 2, 2,5 o 5 por potencia de 10. */
    static BigDecimal pasoRedondo(BigDecimal maximo) {
        if (maximo.signum() <= 0) {
            return new BigDecimal("1000");
        }
        double bruto = maximo.doubleValue() / 4;
        double magnitud = Math.pow(10, Math.floor(Math.log10(bruto)));
        double normalizado = bruto / magnitud;
        double redondo = normalizado <= 1 ? 1 : normalizado <= 2 ? 2 : normalizado <= 2.5 ? 2.5 : normalizado <= 5 ? 5 : 10;
        BigDecimal paso = BigDecimal.valueOf(redondo * magnitud).setScale(0, RoundingMode.HALF_UP);
        return paso.max(PASO_MINIMO);
    }

    private static double altura(BigDecimal valor, BigDecimal tope) {
        return valor.multiply(BigDecimal.valueOf(100)).divide(tope, 2, RoundingMode.HALF_UP).doubleValue();
    }
}
