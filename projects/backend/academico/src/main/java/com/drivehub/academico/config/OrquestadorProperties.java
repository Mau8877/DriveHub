package com.drivehub.academico.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuración del orquestador de clases y horas (CU44), prefijo `app.orquestador`.
 *
 * * `minutosParaDeshacer`: una reserva a medias más joven que esto se intenta completar; con esta edad o más se
 * * deshace (se devuelve la hora, se libera la franja y se cancela). Evita confirmar una clase que el estudiante ya
 * * dio por perdida.
 * * `maximoIntentos`: fallos por causas propias antes de marcar la reserva para revisión manual. Que Pagos no
 * * responda NO cuenta.
 * * `habilitado`, `cron` y `lote`: el proceso programado que recorre las reservas a medias de todos los estudiantes.
 */
@ConfigurationProperties(prefix = "app.orquestador")
public record OrquestadorProperties(
        @DefaultValue("60") int minutosParaDeshacer,
        @DefaultValue("5") int maximoIntentos,
        @DefaultValue("true") boolean habilitado,
        @DefaultValue("0 * * * * *") String cron,
        @DefaultValue("50") int lote) {
}
