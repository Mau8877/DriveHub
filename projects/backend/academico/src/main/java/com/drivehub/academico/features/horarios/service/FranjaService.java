package com.drivehub.academico.features.horarios.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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
import com.drivehub.academico.common.exception.ValidationException;
import com.drivehub.academico.features.horarios.dto.DiaBloqueadoResponse;
import com.drivehub.academico.features.horarios.dto.FranjaResponse;
import com.drivehub.academico.features.horarios.enums.EstadoFranja;
import com.drivehub.academico.features.horarios.model.Franja;
import com.drivehub.academico.features.horarios.repository.FranjaRepository;

import lombok.RequiredArgsConstructor;

/**
 * Franjas del instructor (CU12): listarlas por fecha o por rango, y pasarlas entre `LIBRE` y `BLOQUEADA`,
 * una por una o un día completo.
 *
 * * Pasar a `RESERVADA` y volver es de la reserva y su cancelación (CU14, CU15): acá el instructor
 * * no puede tocar una franja reservada.
 * * El cambio de estado es un solo `findAndModify` condicional (o un `updateMulti` filtrado por estado):
 * * dos peticiones simultáneas no se pisan.
 */
@Service
@RequiredArgsConstructor
public class FranjaService {

    static final int DIAS_MAXIMOS_DEL_RANGO = 7;

    private static final Logger LOG = LoggerFactory.getLogger(FranjaService.class);
    private static final String MODULO = "horarios";
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final FranjaRepository franjaRepository;
    private final Clock clock;

    /**
     * Sin fechas, desde hoy en adelante. Con las dos, el rango inclusivo de hasta 7 días; indicar una sola, o
     * un rango al revés o demasiado largo, es un error (`ERR_ACA_13`).
     */
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public Page<FranjaResponse> listar(UUID instructorCodigo, LocalDate fechaDesde, LocalDate fechaHasta,
            EstadoFranja estado, int page, int pageSize) {
        LocalDate desde = fechaDesde;
        if (fechaDesde == null && fechaHasta == null) {
            desde = LocalDate.now(clock);
        } else {
            validarRango(fechaDesde, fechaHasta);
        }
        var pagina = Paginacion.de(page, pageSize, Sort.by("fecha", "horaInicio"));
        return franjaRepository.listar(instructorCodigo, desde, fechaHasta, estado, pagina)
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

    /**
     * Bloquea las franjas libres del día que todavía no empezaron. Las reservadas no se tocan. Repetirlo no
     * cambia nada más, y un día sin franjas libres informa cero.
     */
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public DiaBloqueadoResponse bloquearDia(UUID instructorCodigo, LocalDate fecha) {
        return cambiarDia(instructorCodigo, fecha, EstadoFranja.LIBRE, EstadoFranja.BLOQUEADA, "Día bloqueado");
    }

    @PreAuthorize("hasRole('INSTRUCTOR')")
    public DiaBloqueadoResponse desbloquearDia(UUID instructorCodigo, LocalDate fecha) {
        return cambiarDia(instructorCodigo, fecha, EstadoFranja.BLOQUEADA, EstadoFranja.LIBRE, "Día desbloqueado");
    }

    private DiaBloqueadoResponse cambiarDia(UUID instructorCodigo, LocalDate fecha, EstadoFranja esperado,
            EstadoFranja nuevo, String mensajeLog) {
        LocalDateTime ahora = LocalDateTime.now(clock);
        LocalDate hoy = ahora.toLocalDate();
        if (fecha.isBefore(hoy)) {
            throw new ConflictException(AcademicoErrorCodes.FRANJA_YA_EMPEZO, "El día ya pasó");
        }
        // * Hoy solo se tocan las franjas que todavía no empezaron
        String horaMinima = fecha.equals(hoy) ? ahora.toLocalTime().format(HORA) : null;
        long cambiadas = franjaRepository.cambiarEstadoDelDia(instructorCodigo, fecha, esperado, nuevo, horaMinima);

        LOG.info(mensajeLog, kv("module", MODULO), kv("instructorCodigo", instructorCodigo), kv("fecha", fecha),
            kv("franjasAfectadas", cambiadas));
        return new DiaBloqueadoResponse(fecha, cambiadas);
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

    private static void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new ValidationException(AcademicoErrorCodes.RANGO_FECHAS_INVALIDO,
                "Indica la fecha inicial y la final del rango");
        }
        if (hasta.isBefore(desde)) {
            throw new ValidationException(AcademicoErrorCodes.RANGO_FECHAS_INVALIDO,
                "La fecha inicial no puede ser posterior a la final");
        }
        if (ChronoUnit.DAYS.between(desde, hasta) + 1 > DIAS_MAXIMOS_DEL_RANGO) {
            throw new ValidationException(AcademicoErrorCodes.RANGO_FECHAS_INVALIDO,
                "El rango no puede pasar de " + DIAS_MAXIMOS_DEL_RANGO + " días");
        }
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
