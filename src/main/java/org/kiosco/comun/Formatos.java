package org.kiosco.comun;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Formatos en castellano de Argentina para usar desde las plantillas: {@code ${@fmt.plata(monto)}}.
 */
@Component("fmt")
public class Formatos {

    private static final Locale ES_AR = Locale.of("es", "AR");
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", ES_AR);
    private static final DateTimeFormatter DIA_CON_ANIO = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", ES_AR);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FECHA_CORTA = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter MES_Y_ANIO = DateTimeFormatter.ofPattern("MMMM yyyy", ES_AR);
    private static final BigDecimal MIL = new BigDecimal("1000");
    private static final BigDecimal MILLON = new BigDecimal("1000000");

    private final Clock clock;

    public Formatos(Clock clock) {
        this.clock = clock;
    }

    /** 12300 → "$ 12.300"; 12300.5 → "$ 12.300,50". */
    public String plata(BigDecimal monto) {
        if (monto == null) {
            return "$ 0";
        }
        boolean tieneCentavos = monto.stripTrailingZeros().scale() > 0;
        DecimalFormat formato = new DecimalFormat(tieneCentavos ? "#,##0.00" : "#,##0", DecimalFormatSymbols.getInstance(ES_AR));
        return (monto.signum() < 0 ? "− $ " : "$ ") + formato.format(monto.abs());
    }

    /** Para ejes de gráficos: "$ 850", "$ 12,5 mil", "$ 1,2 M". */
    public String plataCorta(BigDecimal monto) {
        BigDecimal abs = monto.abs();
        DecimalFormat unDecimal = new DecimalFormat("#,##0.#", DecimalFormatSymbols.getInstance(ES_AR));
        String signo = monto.signum() < 0 ? "− $ " : "$ ";
        if (abs.compareTo(MIL) < 0) {
            return signo + unDecimal.format(abs.setScale(0, RoundingMode.HALF_UP));
        }
        if (abs.compareTo(MILLON) < 0) {
            return signo + unDecimal.format(abs.divide(MIL, 1, RoundingMode.HALF_UP)) + " mil";
        }
        return signo + unDecimal.format(abs.divide(MILLON, 1, RoundingMode.HALF_UP)) + " M";
    }

    /** Qué parte del total es: "34 %". */
    public String porcentaje(BigDecimal parte, BigDecimal total) {
        if (total.signum() == 0) {
            return "—";
        }
        return parte.multiply(BigDecimal.valueOf(100)).divide(total, 0, RoundingMode.HALF_UP) + " %";
    }

    /** Tamaño de archivo: "850 B", "12,3 KB", "1,5 MB". */
    public String bytes(long bytes) {
        DecimalFormat unDecimal = new DecimalFormat("#,##0.#", DecimalFormatSymbols.getInstance(ES_AR));
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return unDecimal.format(bytes / 1024.0) + " KB";
        }
        return unDecimal.format(bytes / (1024.0 * 1024)) + " MB";
    }

    /** "Septiembre 2026". */
    public String mes(YearMonth mes) {
        String texto = mes.format(MES_Y_ANIO);
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }

    /** "Lun 21", para las columnas de un gráfico. */
    public String diaYNumero(LocalDate fecha) {
        return diaCorto(fecha.getDayOfWeek()) + " " + fecha.getDayOfMonth();
    }

    /** Con el signo adelante, para listas de entradas y salidas: "+ $ 3.200" / "− $ 12.800". */
    public String plataConSigno(BigDecimal monto, boolean entrada) {
        return (entrada ? "+ " : "− ") + plata(monto.abs());
    }

    /** "Sábado 26 de septiembre" (agrega el año solo si no es el actual). */
    public String fechaLarga(LocalDate fecha) {
        DateTimeFormatter formato = fecha.getYear() == LocalDate.now(clock).getYear() ? DIA : DIA_CON_ANIO;
        String texto = fecha.format(formato);
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }

    public String hora(LocalDateTime momento) {
        return momento.format(HORA);
    }

    /** "hoy", "ayer", "hace 12 días". */
    public String haceCuanto(LocalDate fecha) {
        long dias = ChronoUnit.DAYS.between(fecha, LocalDate.now(clock));
        if (dias <= 0) {
            return "hoy";
        }
        return dias == 1 ? "ayer" : "hace " + dias + " días";
    }

    public long diasDesde(LocalDate fecha) {
        return ChronoUnit.DAYS.between(fecha, LocalDate.now(clock));
    }

    /** "24/09". */
    public String fechaCorta(LocalDate fecha) {
        return fecha.format(FECHA_CORTA);
    }

    /** {MARTES, VIERNES} → "martes y viernes". */
    public String dias(Collection<DayOfWeek> dias) {
        return enumerar(dias.stream().sorted().map(this::dia).toList());
    }

    /** "lunes"; en minúscula, como va en medio de una oración. */
    public String dia(DayOfWeek dia) {
        return dia.getDisplayName(TextStyle.FULL, ES_AR);
    }

    /** "Lun", para botones chicos. */
    public String diaCorto(DayOfWeek dia) {
        String corto = dia.getDisplayName(TextStyle.SHORT, ES_AR).replace(".", "");
        return Character.toUpperCase(corto.charAt(0)) + corto.substring(1);
    }

    /** ["a", "b", "c"] → "a, b y c". */
    public String enumerar(List<String> partes) {
        if (partes.size() <= 1) {
            return String.join("", partes);
        }
        return String.join(", ", partes.subList(0, partes.size() - 1)) + " y " + partes.getLast();
    }
}
