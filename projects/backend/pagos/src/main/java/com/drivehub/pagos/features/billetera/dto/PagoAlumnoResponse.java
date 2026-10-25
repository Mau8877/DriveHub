package com.drivehub.pagos.features.billetera.dto;

import java.time.Instant;
import java.util.UUID;

import com.drivehub.pagos.features.pagos.enums.EstadoPagoVisible;

/**
 * Una compra del alumno vista por su instructor (CU29): plan, horas, fecha y estado. Solo lectura.
 */
public record PagoAlumnoResponse(UUID codigo, String planNombre, Short horas, EstadoPagoVisible estado,
        Instant creadoEn) {
}
