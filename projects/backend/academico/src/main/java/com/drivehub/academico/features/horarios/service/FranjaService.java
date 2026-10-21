package com.drivehub.academico.features.horarios.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.drivehub.academico.common.exception.AcademicoErrorCodes;
import com.drivehub.academico.common.exception.ConflictException;
import com.drivehub.academico.common.exception.NotFoundException;
import com.drivehub.academico.features.horarios.dto.FranjaResponse;
import com.drivehub.academico.features.horarios.enums.EstadoFranja;
import com.drivehub.academico.features.horarios.model.Franja;
import com.drivehub.academico.features.horarios.repository.FranjaRepository;

import lombok.RequiredArgsConstructor;

/**
 * Franjas del instructor (CU12): listarlas y pasarlas entre `LIBRE` y `BLOQUEADA`.
 *
 * * Pasar a `RESERVADA` y volver es de la reserva y su cancelación (CU14, CU15): acá el instructor
 * * no puede tocar una franja reservada.
 * * El cambio de estado es un solo `findAndModify` condicional: dos peticiones simultáneas no se pisan.
 */
@Service
@RequiredArgsConstructor
public class FranjaService {

    private static final Logger LOG = LoggerFactory.getLogger(FranjaService.class);
    private static final String MODULO = "horarios";

    private final FranjaRepository franjaRepository;
    private final Clock clock;

    @PreAuthorize("hasRole('INSTRUCTOR')")
    public Page<FranjaResponse> listar(UUID instructorCodigo, LocalDate fecha, EstadoFranja estado, int page,
            int pageSize) {
        // * Sin fecha: desde hoy en adelante
        LocalDate desde = fecha == null ? LocalDate.now(clock) : null;
        var pagina = Paginacion.de(page, pageSize, Sort.by("fecha", "horaInicio"));
        return franjaRepository.listar(instructorCodigo, fecha, desde, estado, pagina)
            .map(FranjaService::aRespuesta);
    }

    @PreAuthorize("hasRole('INSTRUCTOR')")
    public FranjaResponse bloquear(UUID instructorCodigo, UUID franjaCodigo) {
        return cambiar(instructorCodigo, franjaCodigo, EstadoFranja.LIBRE, EstadoFranja.BLOQUEADA,
            AcademicoErrorCodes.FRANJA_NO_ESTA_LIBRE, "La franja no está libre: solo se bloquean las libres",
            "Franja bloqueada");
    }

    @PreAuthorize("hasRole('INSTRUCTOR')")
    public FranjaResponse desbloquear(UUID instructorCodigo, UUID franjaCodigo) {
        return cambiar(instructorCodigo, franjaCodigo, EstadoFranja.BLOQUEADA, EstadoFranja.LIBRE,
            AcademicoErrorCodes.FRANJA_NO_ESTA_BLOQUEADA,
            "La franja no está bloqueada: solo se desbloquean las bloqueadas", "Franja desbloqueada");
    }

    private FranjaResponse cambiar(UUID instructorCodigo, UUID franjaCodigo, EstadoFranja esperado,
            EstadoFranja nuevo, String codigoEstadoIncorrecto, String mensajeEstadoIncorrecto, String mensajeLog) {
        Franja franja = franjaRepository
            .findByCodigoFranjaAndInstructorCodigoAndDeletedAtIsNull(franjaCodigo, instructorCodigo)
            .orElseThrow(() -> new NotFoundException(AcademicoErrorCodes.FRANJA_NO_ENCONTRADA,
                "La franja no existe"));
        if (yaEmpezo(franja)) {
            throw new ConflictException(AcademicoErrorCodes.FRANJA_YA_EMPEZO, "La franja ya empezó");
        }
        // * Atómico: si entre la lectura y esta escritura cambió de estado, no se pisa y se informa el estado
        Franja actualizada = franjaRepository.cambiarEstado(franjaCodigo, instructorCodigo, esperado, nuevo)
            .orElseThrow(() -> new ConflictException(codigoEstadoIncorrecto, mensajeEstadoIncorrecto));

        LOG.info(mensajeLog, kv("module", MODULO), kv("instructorCodigo", instructorCodigo),
            kv("franjaCodigo", franjaCodigo), kv("estado", nuevo));
        return aRespuesta(actualizada);
    }

    private boolean yaEmpezo(Franja franja) {
        LocalDateTime inicio = LocalDateTime.of(franja.getFecha(), LocalTime.parse(franja.getHoraInicio()));
        return !inicio.isAfter(LocalDateTime.now(clock));
    }

    private static FranjaResponse aRespuesta(Franja f) {
        return new FranjaResponse(f.getCodigoFranja(), f.getFecha(), f.getHoraInicio(), f.getHoraFin(),
            f.getEstado());
    }
}
