package org.kiosco.comun;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
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
