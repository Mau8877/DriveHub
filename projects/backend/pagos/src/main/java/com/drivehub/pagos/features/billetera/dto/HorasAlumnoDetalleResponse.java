package com.drivehub.pagos.features.billetera.dto;

import java.util.List;

/**
 * El detalle de un alumno para su instructor (CU29): el resumen de horas, el tiempo gastado con él clase a
 * clase (la más reciente primero) y las compras del alumno (las más recientes primero, hasta 20).
 */
public record HorasAlumnoDetalleResponse(
        HoraAlumnoResponse resumen,
        List<ConsumoClaseResponse> consumos,
        List<PagoAlumnoResponse> pagos) {
}
