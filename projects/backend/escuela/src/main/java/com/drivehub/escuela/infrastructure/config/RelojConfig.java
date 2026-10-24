package com.drivehub.escuela.infrastructure.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reloj inyectable: permite probar reglas que dependen de la fecha de hoy
 * (ej. el vencimiento de la licencia de un instructor) con una fecha fija.
 */
@Configuration
public class RelojConfig {

    @Bean
    public Clock reloj() {
        return Clock.systemDefaultZone();
    }
}
