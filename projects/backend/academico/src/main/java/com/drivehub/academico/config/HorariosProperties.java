package com.drivehub.academico.config;

import java.time.ZoneId;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuración de los horarios del instructor (CU12), prefijo `app.horarios`.
 *
 * * La zona horaria es la de la escuela: decide qué es "hoy" y "ahora" al generar franjas.
 * * El horizonte (28 días contando hoy) y el cron son del servicio, no de cada horario.
 */
@ConfigurationProperties(prefix = "app.horarios")
public record HorariosProperties(
        @DefaultValue("America/La_Paz") ZoneId zonaHoraria,
        @DefaultValue("28") int horizonteDias,
        @DefaultValue("0 0 2 * * *") String extensionCron,
        @DefaultValue("true") boolean jobHabilitado) {
}
