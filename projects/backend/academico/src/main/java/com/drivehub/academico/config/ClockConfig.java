package com.drivehub.academico.config;

import java.time.Clock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * El reloj del servicio, en la zona horaria de la escuela. Existe como bean para poder
 * fijar la hora en los tests.
 */
@Configuration
@EnableConfigurationProperties(HorariosProperties.class)
public class ClockConfig {

    @Bean
    public Clock clock(HorariosProperties propiedades) {
        return Clock.system(propiedades.zonaHoraria());
    }
}
