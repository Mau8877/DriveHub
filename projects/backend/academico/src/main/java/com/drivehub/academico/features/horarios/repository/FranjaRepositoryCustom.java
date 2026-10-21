package com.drivehub.academico.features.horarios.repository;

import java.time.LocalDate;
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
     * Las franjas del instructor, con filtros opcionales: `fecha` exacta, `fechaDesde` (inclusive) y `estado`.
     */
    Page<Franja> listar(UUID instructorCodigo, LocalDate fecha, LocalDate fechaDesde, EstadoFranja estado,
            Pageable pageable);

    /**
     * Cambia el estado con un solo `findAndModify` atómico: solo si todavía es el esperado.
     * Devuelve la franja ya actualizada, o vacío si no existe, es de otro instructor o ya no estaba en
     * ese estado (así dos peticiones simultáneas no pisan el estado).
     */
    Optional<Franja> cambiarEstado(UUID codigoFranja, UUID instructorCodigo, EstadoFranja esperado,
            EstadoFranja nuevo);

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
}
