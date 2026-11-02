package com.drivehub.academico.features.reservas.repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.drivehub.academico.features.reservas.enums.EstadoReserva;
import com.drivehub.academico.features.reservas.model.Reserva;

/**
 * Consultas de reservas que Spring Data no deriva solas (CU14).
 *
 * * `hoy` y `ahora` (`HH:mm`) son la fecha y la hora actuales en la zona de la escuela: las pone el service con su
 * * `Clock`, el repository no conoce el reloj.
 */
public interface ReservaRepositoryCustom {

    /**
     * Si el estudiante ya tiene una reserva `PENDIENTE_PAGO` o `CONFIRMADA` en esa fecha y hora de inicio. Las
     * canceladas no cuentan.
     */
    default boolean existeSolape(UUID estudianteCodigo, LocalDate fecha, String horaInicio) {
        return existeSolape(estudianteCodigo, fecha, horaInicio, null);
    }

    /**
     * Lo mismo, dejando afuera la reserva `excluyendoReserva` (nulo = ninguna): la clase que se está reprogramando
     * no choca consigo misma (CU15). Cuenta también las `REPROGRAMACION_PENDIENTE`, que todavía ocupan su horario.
     */
    boolean existeSolape(UUID estudianteCodigo, LocalDate fecha, String horaInicio, UUID excluyendoReserva);

    /**
     * Inicia la reprogramación (CU15) con un solo `findAndModify` atómico: la reserva pasa de `CONFIRMADA` a
     * `REPROGRAMACION_PENDIENTE` guardando la franja a la que se mueve. Vacío si no existe, es de otro estudiante o
     * ya no estaba `CONFIRMADA`.
     */
    Optional<Reserva> iniciarReprogramacion(UUID codigoReserva, UUID estudianteCodigo, Long franjaDestinoId,
            String motivo, Instant ahora);

    /**
     * Termina la reprogramación (CU15) con un solo `findAndModify` atómico, solo si sigue `REPROGRAMACION_PENDIENTE`:
     * copia la franja nueva a la reserva, vuelve a `CONFIRMADA` y borra la franja destino. Vacío si ya no estaba
     * a medias (así repetirlo es inocuo).
     */
    Optional<Reserva> completarReprogramacion(UUID codigoReserva, UUID estudianteCodigo, Long franjaId,
            LocalDate fecha, String horaInicio, String horaFin, UUID instructorCodigo, Instant ahora);

    /**
     * Deshace la reprogramación (CU15), solo si sigue `REPROGRAMACION_PENDIENTE`: vuelve a `CONFIRMADA` en la franja
     * original y borra la franja destino. Vacío si ya no estaba a medias.
     */
    Optional<Reserva> revertirReprogramacion(UUID codigoReserva, UUID estudianteCodigo, String motivo, Instant ahora);

    /**
     * Las clases por venir: reservas `CONFIRMADA` (o `REPROGRAMACION_PENDIENTE`, que sigue siendo del estudiante en
     * su franja anterior) que todavía no terminaron (fecha posterior a `hoy`, o de hoy con hora de fin posterior a
     * `ahora`), de la más próxima a la más lejana. El orden lo fija el repository.
     */
    Page<Reserva> proximas(UUID estudianteCodigo, LocalDate hoy, String ahora, Pageable pageable);

    /**
     * El historial: reservas `COMPLETADA` o `NO_ASISTIO`, las `CANCELADA` que alguna vez estuvieron confirmadas
     * (una que nunca logró confirmarse no fue una clase del estudiante), y las `CONFIRMADA` que ya terminaron (lo
     * contrario exacto de {@link #proximas}), de la más reciente a la más antigua. Las `PENDIENTE_PAGO` no
     * aparecen en ninguna de las dos.
     */
    Page<Reserva> historial(UUID estudianteCodigo, LocalDate hoy, String ahora, Pageable pageable);

    /**
     * Las reservas `PENDIENTE_PAGO` del estudiante creadas antes de `anteriorA`: las que quedaron a medias.
     */
    List<Reserva> pendientesDeEstudiante(UUID estudianteCodigo, Instant anteriorA);

    /**
     * Las reservas a medias de TODOS los estudiantes (CU44): en cualquiera de los `estados` transitorios, no borradas,
     * sin marca de revisión y cuyo último cambio (`updatedAt`) fue antes de `anteriorA`, de la más antigua a la más
     * reciente, hasta `limite`.
     *
     * * Se mide por `updatedAt` y no por `createdAt` (CU15): una clase confirmada hace días que se acaba de cancelar
     * * no es "vieja", y el proceso programado no debe pisar la petición del estudiante.
     */
    List<Reserva> pendientesAntiguas(Set<EstadoReserva> estados, Instant anteriorA, int limite);

    /**
     * Suma un intento fallido con un `findAndModify` atómico, solo si la reserva sigue en alguno de los `estados`.
     * Devuelve la reserva con el contador ya actualizado, o vacío si ya no está pendiente.
     */
    Optional<Reserva> registrarIntentoFallido(UUID codigoReserva, Set<EstadoReserva> estados);

    /**
     * Marca la reserva para revisión humana, sin cambiar su estado, y deja el motivo en el historial. Solo si sigue en
     * alguno de los `estados` y todavía no estaba marcada (repetirlo es inocuo). Vacío si no se aplicó.
     */
    Optional<Reserva> marcarParaRevision(UUID codigoReserva, String motivo, Instant ahora, Set<EstadoReserva> estados);

    /**
     * Inicia la cancelación (CU15) con un solo `findAndModify` atómico: la reserva pasa de `CONFIRMADA` a
     * `CANCELACION_PENDIENTE` y queda fijado si la hora se devuelve (`sinDevolucion` falso) o se pierde. Vacío si
     * no existe, es de otro estudiante o ya no estaba `CONFIRMADA` (así dos cancelaciones simultáneas no se pisan).
     */
    Optional<Reserva> iniciarCancelacion(UUID codigoReserva, UUID estudianteCodigo, boolean sinDevolucion,
            String motivo, Instant ahora);

    /**
     * Inicia la cancelación por el instructor (CU16) con un solo `findAndModify` atómico: la reserva pasa de
     * `CONFIRMADA` a `CANCELACION_PENDIENTE`, la hora SIEMPRE se devuelve (`sinDevolucion` falso) y queda marcada
     * como `canceladaPorInstructor` para que se bloquee la franja. Vacío si no existe, es de otro instructor o ya no
     * estaba `CONFIRMADA` (así una cancelación del estudiante y otra del instructor no se pisan).
     */
    Optional<Reserva> iniciarCancelacionPorInstructor(UUID codigoReserva, UUID instructorCodigo, String motivo,
            Instant ahora);

    /**
     * Cierra la clase (CU17) con un solo `findAndModify` atómico: la reserva pasa de `CONFIRMADA` a `COMPLETADA` o
     * `NO_ASISTIO` (`nuevoEstado`). Vacío si no existe, es de otro instructor o ya no estaba `CONFIRMADA` (así dos
     * cierres, o un cierre y una cancelación a la vez, no se pisan).
     *
     * @throws IllegalArgumentException si `nuevoEstado` no es `COMPLETADA` ni `NO_ASISTIO` (error de programación)
     */
    Optional<Reserva> cerrar(UUID codigoReserva, UUID instructorCodigo, EstadoReserva nuevoEstado, String motivo,
            Instant ahora);

    /**
     * La agenda del instructor (CU16): sus reservas de esa fecha que son o fueron una clase: `CONFIRMADA`,
     * `REPROGRAMACION_PENDIENTE`, `COMPLETADA`, `NO_ASISTIO`, `CANCELACION_PENDIENTE` y las `CANCELADA` que alguna
     * vez estuvieron confirmadas. Nunca las `PENDIENTE_PAGO`. Por hora de inicio; el orden lo fija el repository.
     */
    Page<Reserva> agendaDeInstructor(UUID instructorCodigo, LocalDate fecha, Pageable pageable);

    /**
     * Cambia el estado con un solo `findAndModify` atómico, solo si todavía es el esperado, y agrega la entrada
     * al historial de estados. También fija `updatedAt`, que `findAndModify` no actualiza solo. Devuelve la reserva ya actualizada, o vacío si no existe, es de otro estudiante o
     * ya no estaba en ese estado (así dos peticiones simultáneas no pisan el estado).
     */
    Optional<Reserva> cambiarEstado(UUID codigoReserva, UUID estudianteCodigo, EstadoReserva esperado,
            EstadoReserva nuevo, String motivo, Instant ahora);
}
