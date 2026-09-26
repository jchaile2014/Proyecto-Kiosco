package org.kiosco.comun;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

class FormatosTest {

    private final Formatos fmt = new Formatos(Clock.fixed(
            LocalDate.of(2026, 9, 26).atStartOfDay(ZoneId.of("America/Argentina/Buenos_Aires")).toInstant(),
            ZoneId.of("America/Argentina/Buenos_Aires")));

    @ParameterizedTest(name = "{0} → \"{1}\"")
    @CsvSource(delimiter = '|', value = {
            "12300     | $ 12.300",
            "12300.5   | $ 12.300,50",
            "0         | $ 0",
            "-400      | − $ 400",
    })
    void plata(String monto, String esperado) {
        assertThat(fmt.plata(new BigDecimal(monto))).isEqualTo(esperado);
    }

    @ParameterizedTest(name = "{0} → \"{1}\"")
    @CsvSource(delimiter = '|', value = {
            "850       | $ 850",
            "12500     | $ 12,5 mil",
            "150000    | $ 150 mil",
            "1250000   | $ 1,3 M",
            "0         | $ 0",
    })
    void plataCortaParaLosEjes(String monto, String esperado) {
        assertThat(fmt.plataCorta(new BigDecimal(monto))).isEqualTo(esperado);
    }

    @Test
    void fechasYDiasEnCastellano() {
        assertThat(fmt.fechaLarga(LocalDate.of(2026, 9, 26))).isEqualTo("Sábado 26 de septiembre");
        assertThat(fmt.fechaLarga(LocalDate.of(2025, 12, 31))).isEqualTo("Miércoles 31 de diciembre de 2025");
        assertThat(fmt.mes(YearMonth.of(2026, 9))).isEqualTo("Septiembre 2026");
        assertThat(fmt.diaYNumero(LocalDate.of(2026, 9, 21))).isEqualTo("Lun 21");
        assertThat(fmt.dias(EnumSet.of(DayOfWeek.FRIDAY, DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)))
                .isEqualTo("lunes, miércoles y viernes");
        assertThat(fmt.haceCuanto(LocalDate.of(2026, 9, 14))).isEqualTo("hace 12 días");
        assertThat(fmt.haceCuanto(LocalDate.of(2026, 9, 25))).isEqualTo("ayer");
    }

    @Test
    void porcentajeDelTotal() {
        assertThat(fmt.porcentaje(new BigDecimal("62400"), new BigDecimal("75200"))).isEqualTo("83 %");
        assertThat(fmt.porcentaje(BigDecimal.ONE, BigDecimal.ZERO)).isEqualTo("—");
    }
}
