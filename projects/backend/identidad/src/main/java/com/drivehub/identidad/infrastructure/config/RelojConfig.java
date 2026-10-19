package com.drivehub.identidad.infrastructure.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reloj inyectable: permite probar reglas que dependen de la fecha de hoy
 * (ej. la edad mínima del registro) con una fecha fija.
 */
@Configuration
public class RelojConfig {

    @Bean
    public Clock reloj() {
        return Clock.systemDefaultZone();
    }
}
