package com.drivehub.academico.features.horarios.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.drivehub.academico.features.horarios.enums.EstadoFranja;
import com.drivehub.academico.features.horarios.model.Franja;

/**
 * Consultas de franjas que Spring Data no deriva solas.
 */
public interface FranjaRepositoryCustom {

    /**
     * Las franjas del instructor, con filtros opcionales: `fechaDesde` y `fechaHasta` (ambas inclusivas; sin
     * `fechaHasta` no hay tope) y `estado`. Una fecha exacta es `fechaDesde = fechaHasta`.
     */
    Page<Franja> listar(UUID instructorCodigo, LocalDate fechaDesde, LocalDate fechaHasta, EstadoFranja estado,
            Pageable pageable);

    /**
     * Cambia el estado con un solo `findAndModify` atómico: solo si todavía es el esperado.
     * Devuelve la franja ya actualizada, o vacío si no existe, es de otro instructor o ya no estaba en
     * ese estado (así dos peticiones simultáneas no pisan el estado).
     */
    Optional<Franja> cambiarEstado(UUID codigoFranja, UUID instructorCodigo, EstadoFranja esperado,
            EstadoFranja nuevo);

    /**
     * Cambia de estado todas las franjas del instructor de una fecha que estén en el estado `esperado`. Si
     * `horaMinima` (`HH:mm`) no es nulo, solo las que empiezan después de esa hora. Devuelve cuántas cambió.
     *
     * * Un `updateMulti` es atómico por documento: no pisa una franja que cambió de estado en el medio.
     */
    long cambiarEstadoDelDia(UUID instructorCodigo, LocalDate fecha, EstadoFranja esperado, EstadoFranja nuevo,
            String horaMinima);

    /**
     * Borra las franjas futuras `LIBRE` o `BLOQUEADA` de un horario: las de fecha posterior a `hoy`, o de hoy
     * con hora de inicio posterior a `horaActual` (`HH:mm`). Devuelve cuántas borró.
     *
     * ! Borrado físico y no soft delete, a propósito: son datos derivados del horario y una fila marcada como
     * ! borrada seguiría ocupando la clave única (instructor, fecha, hora), lo que impediría crear otro
     * ! horario en esa hora.
     */
    long eliminarFuturasNoReservadas(Long plantillaId, LocalDate hoy, String horaActual);

    /**
     * Inserta la franja; si ya existe una del mismo instructor en esa fecha y hora, no hace nada y devuelve
     * `false`. Lo que vuelve idempotente la generación: repetirla no duplica ni falla.
     */
    boolean insertarSiNoExiste(Franja franja);

    /**
     * Las franjas que un estudiante puede reservar (CU14): `LIBRE`, de esa escuela y que todavía no empezaron
     * (de un día posterior a `hoy`, o de hoy con hora de inicio posterior a `ahora`, `HH:mm`). Cada filtro es
     * opcional (`null` = sin filtro): rango de fechas (ambas inclusivas), instructor, y hora de inicio entre
     * `horaDesde` (inclusive) y `horaHasta` (exclusive). Orden fijo: fecha, hora de inicio e instructor.
     */
    Page<Franja> disponibles(UUID escuelaCodigo, LocalDate hoy, String ahora, LocalDate fechaDesde,
            LocalDate fechaHasta, UUID instructorCodigo, String horaDesde, String horaHasta, Pageable pageable);

    /**
     * Los instructores de esa escuela que tienen al menos una franja disponible con esos criterios (los mismos
     * de {@link #disponibles}, sin el filtro de instructor ni de hora).
     */
    List<UUID> instructoresConDisponibles(UUID escuelaCodigo, LocalDate hoy, String ahora, LocalDate fechaDesde,
            LocalDate fechaHasta);

    /**
     * Aparta la franja para una reserva (CU14) con un solo `findAndModify` atómico: pasa de `LIBRE` a `RESERVADA`
     * y guarda el `_id` de la reserva, solo si es de esa escuela y todavía no empezó. Devuelve la franja ya
     * actualizada, o vacío si ya no estaba libre (quien llega segundo recibe vacío: dos estudiantes nunca se
     * quedan con la misma).
     */
    Optional<Franja> apartar(UUID codigoFranja, UUID escuelaCodigo, Long reservaId, LocalDate hoy, String ahora);

    /**
     * Devuelve la franja a `LIBRE` y le quita la reserva, solo si sigue `RESERVADA` por esa reserva (nunca libera
     * la reserva de otro). Devuelve si cambió algo.
     */
    boolean liberar(UUID codigoFranja, Long reservaId);
}
