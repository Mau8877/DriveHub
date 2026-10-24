package com.drivehub.academico.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita las tareas programadas (@Scheduled) del servicio.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
