package org.kiosco;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Reloj que los tests pueden adelantar para simular que pasan los días. */
public class RelojDePrueba extends Clock {

    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");

    private Instant ahora = LocalDateTime.of(2026, 9, 20, 10, 0).atZone(ZONA).toInstant();

    public void fijar(LocalDateTime momento) {
        this.ahora = momento.atZone(ZONA).toInstant();
    }

    @Override
    public ZoneId getZone() {
        return ZONA;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Instant instant() {
        return ahora;
    }

    @TestConfiguration
    public static class Config {

        @Bean
        @Primary
        RelojDePrueba relojDePrueba() {
            return new RelojDePrueba();
        }
    }
}
