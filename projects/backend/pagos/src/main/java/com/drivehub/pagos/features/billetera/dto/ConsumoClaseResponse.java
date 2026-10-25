package com.drivehub.pagos.features.billetera.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Las horas netas (consumidas menos devueltas) de una clase del alumno con el instructor (CU29). `fecha` es la
 * del consumo: Académico no informa la fecha real de la clase.
 */
public record ConsumoClaseResponse(UUID claseCodigo, Instant fecha, int horas) {
}
