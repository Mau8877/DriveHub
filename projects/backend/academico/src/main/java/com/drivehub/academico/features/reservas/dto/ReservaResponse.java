package com.drivehub.academico.features.reservas.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.drivehub.academico.features.reservas.enums.EstadoReserva;
import com.drivehub.academico.features.reservas.model.Reserva;

/**
 * Una clase reservada del estudiante (CU14). Sin ids internos ni vehículo.
 *
 * @param terminada la hora de fin ya pasó: una `CONFIRMADA` con `terminada` es «pendiente de cierre» (CU17)
 */
public record ReservaResponse(
        UUID codigo,
        LocalDate fecha,
        String horaInicio,
        String horaFin,
        UUID instructorCodigo,
        String instructorNombre,
        EstadoReserva estado,
        boolean terminada) {

    public static ReservaResponse de(Reserva reserva, String instructorNombre, boolean terminada) {
        return new ReservaResponse(reserva.getCodigoReserva(), reserva.getFecha(), reserva.getHoraInicio(),
            reserva.getHoraFin(), reserva.getInstructorCodigo(), instructorNombre, reserva.getEstado(), terminada);
    }
}
