package org.kiosco.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

/**
 * Toda la app pregunta "qué día es hoy" a este reloj, así los tests pueden fijar la fecha.
 */
@Configuration
@EnableScheduling
public class RelojConfig {

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
