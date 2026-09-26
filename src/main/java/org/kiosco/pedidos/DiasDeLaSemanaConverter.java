package org.kiosco.pedidos;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/** Guarda un conjunto de días como texto: {MARTES, VIERNES} → "TUESDAY,FRIDAY". */
@Converter
class DiasDeLaSemanaConverter implements AttributeConverter<Set<DayOfWeek>, String> {

    @Override
    public String convertToDatabaseColumn(Set<DayOfWeek> dias) {
        if (dias == null || dias.isEmpty()) {
            return "";
        }
        return EnumSet.copyOf(dias).stream().map(DayOfWeek::name).collect(Collectors.joining(","));
    }

    @Override
    public Set<DayOfWeek> convertToEntityAttribute(String texto) {
        EnumSet<DayOfWeek> dias = EnumSet.noneOf(DayOfWeek.class);
        if (texto != null && !texto.isBlank()) {
            Arrays.stream(texto.split(",")).map(String::trim).map(DayOfWeek::valueOf).forEach(dias::add);
        }
        return dias;
    }
}
