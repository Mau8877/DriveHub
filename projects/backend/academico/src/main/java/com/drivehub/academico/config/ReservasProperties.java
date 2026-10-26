package com.drivehub.academico.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuración de las reservas del estudiante (CU14), prefijo `app.reservas`.
 *
 * * El costo de una clase es del servicio, no de cada franja: la duración de la franja (de 15 a 240 minutos, CU12)
 * * no cambia cuántas horas de la billetera cuesta.
 * * `minutosParaRetomar`: cuánto espera una reserva `PENDIENTE_PAGO` antes de que se la retome al consultar las
 * * clases (así no se pisa una confirmación que todavía está en curso).
 */
@ConfigurationProperties(prefix = "app.reservas")
public record ReservasProperties(
        @DefaultValue("1") int horasPorClase,
        @DefaultValue("10") int maximoPorConfirmacion,
        @DefaultValue("2") int minutosParaRetomar) {
}
