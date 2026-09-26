package org.kiosco.comun;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MontosTest {

    @ParameterizedTest(name = "\"{0}\" → {1}")
    @CsvSource(delimiter = '|', value = {
            "1500        | 1500",
            "1.500       | 1500",
            "1.500,50    | 1500.50",
            "1500,5      | 1500.5",
            "1500.5      | 1500.5",
            "12.50       | 12.50",
            "$ 2.300     | 2300",
            "1.234.567   | 1234567",
            "1.234.567,8 | 1234567.8",
            "0,50        | 0.50",
    })
    void entiendeLaFormaArgentinaDeEscribirPlata(String texto, BigDecimal esperado) {
        assertThat(Montos.parse(texto)).isEqualByComparingTo(esperado);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "abc", "-100", "0", "0,00", "1,2,3", "1.50.0", "15.0000", "1500,505", ".5", "99999999999"})
    void rechazaLoQueNoEsUnMontoValido(String texto) {
        assertThatThrownBy(() -> Montos.parse(texto)).isInstanceOf(MontoInvalidoException.class);
    }

    @ParameterizedTest(name = "\"{0}\" → {1} %")
    @CsvSource(delimiter = '|', value = {
            "10    | 10",
            "2,5   | 2.5",
            "2.5   | 2.5",
            "10 %  | 10",
            "100   | 100",
    })
    void entiendeElPorcentajeDeInteres(String texto, BigDecimal esperado) {
        assertThat(Montos.parsePorcentaje(texto)).isEqualByComparingTo(esperado);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "diez", "0", "-5", "150", "2,555"})
    void rechazaPorcentajesInvalidos(String texto) {
        assertThatThrownBy(() -> Montos.parsePorcentaje(texto)).isInstanceOf(MontoInvalidoException.class);
    }
}
