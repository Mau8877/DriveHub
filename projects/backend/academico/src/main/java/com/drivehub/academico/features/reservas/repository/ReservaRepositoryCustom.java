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
    boolean existeSolape(UUID estudianteCodigo, LocalDate fecha, String horaInicio);

    /**
     * Las clases por venir: reservas `CONFIRMADA` que todavía no terminaron (fecha posterior a `hoy`, o de hoy con
     * hora de fin posterior a `ahora`), de la más próxima a la más lejana. El orden lo fija el repository.
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
     * sin marca de revisión y creadas antes de `anteriorA`, de la más antigua a la más reciente, hasta `limite`.
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
     * Cambia el estado con un solo `findAndModify` atómico, solo si todavía es el esperado, y agrega la entrada
     * al historial de estados. Devuelve la reserva ya actualizada, o vacío si no existe, es de otro estudiante o
     * ya no estaba en ese estado (así dos peticiones simultáneas no pisan el estado).
     */
    Optional<Reserva> cambiarEstado(UUID codigoReserva, UUID estudianteCodigo, EstadoReserva esperado,
            EstadoReserva nuevo, String motivo, Instant ahora);
}
