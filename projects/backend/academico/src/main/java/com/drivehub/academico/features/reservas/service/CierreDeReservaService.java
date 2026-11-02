package com.drivehub.academico.features.reservas.service;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.drivehub.academico.common.exception.AcademicoErrorCodes;
import com.drivehub.academico.common.exception.ConflictException;
import com.drivehub.academico.common.exception.NotFoundException;
import com.drivehub.academico.features.clases.enums.Asistencia;
import com.drivehub.academico.features.reservas.enums.EstadoReserva;
import com.drivehub.academico.features.reservas.model.Reserva;
import com.drivehub.academico.features.reservas.repository.ReservaRepository;

import lombok.RequiredArgsConstructor;

/**
 * Lo que el módulo de clases necesita de las reservas para cerrar una clase (CU17): saber si una reserva se puede
 * cerrar y dejarla `COMPLETADA` o `NO_ASISTIO`. El módulo de clases no toca el repositorio de reservas.
 *
 * * No loguea: el cierre se registra una sola vez, en `ClaseService`.
 */
@Service
@RequiredArgsConstructor
public class CierreDeReservaService {

    private final ReservaRepository reservaRepository;
    private final PlazoDeClase plazo;
    private final Clock clock;

    /**
     * La reserva del instructor, lista para cerrarse: `CONFIRMADA` y con la clase ya empezada.
     *
     * @throws NotFoundException `ERR_ACA_35` si no existe, está borrada o es de otro instructor
     * @throws ConflictException `ERR_ACA_37` si no está `CONFIRMADA` (ya cerrada, cancelada o a medias) o todavía
     *         no empezó
     */
    public Reserva buscarCerrable(UUID instructorCodigo, UUID codigoReserva) {
        Reserva reserva = reservaRepository
            .findByCodigoReservaAndInstructorCodigoAndDeletedAtIsNull(codigoReserva, instructorCodigo)
            .orElseThrow(() -> new NotFoundException(AcademicoErrorCodes.CLASE_INSTRUCTOR_NO_ENCONTRADA,
                "No encontramos esa clase"));
        if (reserva.getEstado() != EstadoReserva.CONFIRMADA || !plazo.haEmpezado(reserva)) {
            throw new ConflictException(AcademicoErrorCodes.CLASE_NO_CERRABLE, "Esa clase no se puede cerrar");
        }
        return reserva;
    }

    /**
     * Deja la reserva `COMPLETADA` (`PRESENTE` o `TARDE`) o `NO_ASISTIO` (`AUSENTE`), solo si sigue `CONFIRMADA`.
     * Vacío si otra petición la cambió antes: quien llama decide qué hacer.
     */
    public Optional<Reserva> marcarCerrada(UUID instructorCodigo, UUID codigoReserva, Asistencia asistencia) {
        EstadoReserva nuevo = asistencia == Asistencia.AUSENTE ? EstadoReserva.NO_ASISTIO : EstadoReserva.COMPLETADA;
        return reservaRepository.cerrar(codigoReserva, instructorCodigo, nuevo,
            "Clase cerrada por el instructor (" + asistencia + ")", clock.instant());
    }
}
