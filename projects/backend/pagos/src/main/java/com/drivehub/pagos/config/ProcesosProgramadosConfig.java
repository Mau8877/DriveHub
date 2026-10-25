package com.drivehub.pagos.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Activa los procesos periódicos de Pagos (`@Scheduled`): el vencimiento de pagos pendientes (CU26) y los que
 * sumen los demás casos de uso. Hay una sola instancia del servicio; con más de una habría que evitar que
 * corran a la vez.
 */
@Configuration
@EnableScheduling
public class ProcesosProgramadosConfig {
}
