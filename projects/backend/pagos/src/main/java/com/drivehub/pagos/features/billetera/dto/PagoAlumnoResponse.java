package com.drivehub.pagos.features.billetera.dto;

import java.time.Instant;
import java.util.UUID;

import com.drivehub.pagos.features.pagos.enums.EstadoPagoVisible;

/**
 * Una compra del alumno vista por su instructor (CU29): plan, horas, fecha y estado. Solo lectura.
 * `comprobanteCodigo` (CU30) es el comprobante de la compra con su PDF ya listo, o `null` si todavía no hay.
 */
public record PagoAlumnoResponse(UUID codigo, String planNombre, Short horas, EstadoPagoVisible estado,
        Instant creadoEn, UUID comprobanteCodigo) {
}
