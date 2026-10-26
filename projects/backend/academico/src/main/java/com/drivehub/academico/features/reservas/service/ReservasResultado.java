package com.drivehub.academico.features.reservas.service;

import java.util.List;

import com.drivehub.academico.features.reservas.model.Reserva;

/**
 * Lo que dejó confirmar varias franjas a la vez (CU14): las que quedaron reservadas, las que no con su motivo, y las
 * horas que le quedan al estudiante.
 *
 * @param horasDisponibles el saldo real que informa Pagos; nulo si Pagos no respondió al pedirlo (las reservas ya
 *         hechas no se deshacen por eso)
 */
public record ReservasResultado(List<Reserva> reservadas, List<FranjaRechazada> rechazadas,
        Integer horasDisponibles) {
}
