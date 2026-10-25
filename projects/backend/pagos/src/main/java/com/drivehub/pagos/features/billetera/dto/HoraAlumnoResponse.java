package com.drivehub.pagos.features.billetera.dto;

import java.util.UUID;

/**
 * Un alumno del instructor con sus horas (CU29). `horasCompradas` y `horasRestantes` son del alumno en total;
 * `horasGastadasConmigo` cuenta solo lo gastado con el instructor que consulta.
 */
public record HoraAlumnoResponse(
        UUID estudianteCodigo,
        String nombres,
        String apellidos,
        int horasCompradas,
        int horasGastadasConmigo,
        int horasRestantes) {
}
