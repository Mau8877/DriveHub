package com.drivehub.academico.features.reservas.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.drivehub.academico.features.reservas.enums.EstadoReserva;
import com.drivehub.academico.features.reservas.model.Reserva;

/**
 * Una clase de la agenda del instructor (CU16). Sin ids internos ni vehículo.
 *
 * @param codigo el código de la reserva (la clase dictada de CU17 se crea después a partir de ella)
 * @param terminada la hora de fin ya pasó: una `CONFIRMADA` con `terminada` es «pendiente de cierre» (CU17)
 * @param cancelable la clase está `CONFIRMADA` y todavía no empezó. Lo calcula el servidor con su reloj; el instructor
 *        no tiene plazo de anticipación
 * @param cerrable la clase está `CONFIRMADA` y ya empezó (CU17): es el complemento exacto de `cancelable`, nunca
 *        los dos verdaderos. Lo calcula el servidor con su reloj
 * @param claseCodigo el `codigoClase` de la clase dictada, solo si la reserva está `COMPLETADA` o `NO_ASISTIO` y ya
 *        tiene clase (CU17); si no, `null`
 * @param evaluable la reserva está `COMPLETADA`, tiene clase y todavía le falta algún tipo de prueba (CU18). Lo
 *        calcula el servidor; nunca es verdadero junto con `cancelable` o `cerrable`
 * @param conPrueba la clase tiene al menos una prueba de clase (CU18)
 *
 * * Los estados transitorios de CU15 no se muestran: `CANCELACION_PENDIENTE` se ve `CANCELADA` y
 * * `REPROGRAMACION_PENDIENTE` se ve `CONFIRMADA`, igual que en {@link ReservaResponse}.
 */
public record ClaseAgendaResponse(
        UUID codigo,
        LocalDate fecha,
        String horaInicio,
        String horaFin,
        UUID estudianteCodigo,
        String estudianteNombre,
        EstadoReserva estado,
        boolean terminada,
        boolean cancelable,
        boolean cerrable,
        UUID claseCodigo,
        boolean evaluable,
        boolean conPrueba) {

    public static ClaseAgendaResponse de(Reserva reserva, String estudianteNombre, boolean terminada,
            boolean cancelable, boolean cerrable, UUID claseCodigo, boolean evaluable, boolean conPrueba) {
        return new ClaseAgendaResponse(reserva.getCodigoReserva(), reserva.getFecha(), reserva.getHoraInicio(),
            reserva.getHoraFin(), reserva.getEstudianteCodigo(), estudianteNombre, estadoVisible(reserva.getEstado()),
            terminada, cancelable, cerrable, claseCodigo, evaluable, conPrueba);
    }

    private static EstadoReserva estadoVisible(EstadoReserva estado) {
        return switch (estado) {
            case CANCELACION_PENDIENTE -> EstadoReserva.CANCELADA;
            case REPROGRAMACION_PENDIENTE -> EstadoReserva.CONFIRMADA;
            default -> estado;
        };
    }
}
