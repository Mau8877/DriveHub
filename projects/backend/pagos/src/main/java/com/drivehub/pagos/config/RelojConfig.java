package com.drivehub.pagos.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * El reloj del servicio. Un bean aparte para que los tests puedan fijar la hora: quien necesita "ahora"
 * lo pide a este reloj y no a `Instant.now()`.
 */
@Configuration
public class RelojConfig {

    @Bean
    public Clock reloj() {
        return Clock.systemUTC();
    }
}
