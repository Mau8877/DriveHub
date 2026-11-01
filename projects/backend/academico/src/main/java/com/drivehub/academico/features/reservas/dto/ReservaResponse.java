package com.drivehub.academico.features.reservas.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.drivehub.academico.features.reservas.enums.EstadoReserva;
import com.drivehub.academico.features.reservas.model.Reserva;

/**
 * Una clase reservada del estudiante (CU14). Sin ids internos ni vehículo.
 *
 * @param terminada la hora de fin ya pasó: una `CONFIRMADA` con `terminada` es «pendiente de cierre» (CU17)
 * @param cancelacionATiempo la clase se puede cancelar con devolución de la hora y reprogramar (CU15): está
 *        `CONFIRMADA`, no empezó y faltan las horas de plazo o más. Lo calcula el servidor con su reloj
 *
 * * Los estados transitorios de CU15 no se muestran: `CANCELACION_PENDIENTE` se ve `CANCELADA` y
 * * `REPROGRAMACION_PENDIENTE` se ve `CONFIRMADA` (como `PENDIENTE_PAGO`, el cliente no los conoce).
 */
public record ReservaResponse(
        UUID codigo,
        LocalDate fecha,
        String horaInicio,
        String horaFin,
        UUID instructorCodigo,
        String instructorNombre,
        EstadoReserva estado,
        boolean terminada,
        boolean cancelacionATiempo) {

    public static ReservaResponse de(Reserva reserva, String instructorNombre, boolean terminada,
            boolean cancelacionATiempo) {
        return new ReservaResponse(reserva.getCodigoReserva(), reserva.getFecha(), reserva.getHoraInicio(),
            reserva.getHoraFin(), reserva.getInstructorCodigo(), instructorNombre, estadoVisible(reserva.getEstado()),
            terminada, cancelacionATiempo);
    }

    private static EstadoReserva estadoVisible(EstadoReserva estado) {
        return switch (estado) {
            case CANCELACION_PENDIENTE -> EstadoReserva.CANCELADA;
            case REPROGRAMACION_PENDIENTE -> EstadoReserva.CONFIRMADA;
            default -> estado;
        };
    }
}
