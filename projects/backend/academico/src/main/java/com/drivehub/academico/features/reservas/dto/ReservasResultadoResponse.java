package com.drivehub.academico.features.reservas.dto;

import java.util.List;

/**
 * El resultado de confirmar varias franjas a la vez (CU14): las que quedaron reservadas, las que no con su motivo y
 * las horas que le quedan al estudiante.
 *
 * @param horasDisponibles el saldo real que informa Pagos; nulo si no respondió al pedirlo (la app lo vuelve a
 *         pedir; las reservas ya hechas no se deshacen)
 */
public record ReservasResultadoResponse(
        List<ReservaResponse> reservadas,
        List<FranjaRechazadaResponse> rechazadas,
        Integer horasDisponibles) {
}
