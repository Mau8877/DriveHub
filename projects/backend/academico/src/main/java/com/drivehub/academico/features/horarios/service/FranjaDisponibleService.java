package com.drivehub.academico.features.horarios.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.drivehub.academico.common.exception.AcademicoErrorCodes;
import com.drivehub.academico.common.exception.ForbiddenException;
import com.drivehub.academico.common.exception.ValidationException;
import com.drivehub.academico.features.horarios.dto.FranjaDisponibleResponse;
import com.drivehub.academico.features.horarios.dto.InstructorDisponibleResponse;
import com.drivehub.academico.features.horarios.model.Franja;
import com.drivehub.academico.features.horarios.repository.FranjaRepository;
import com.drivehub.academico.features.reservas.client.IdentidadClient;

import lombok.RequiredArgsConstructor;

/**
 * Las franjas que un estudiante puede reservar (CU14): las libres y futuras de los instructores de su escuela,
 * con filtros, y los instructores que tienen alguna.
 *
 * * Es la vista del estudiante; la del instructor sobre sus propias franjas es {@link FranjaService} (CU12).
 * * La escuela sale del token, nunca de un parámetro. Sin ella no hay sobre qué ofrecer: `ERR_ACA_16`.
 * * «Ahora» es el del reloj del servicio, en la zona horaria de la escuela.
 * * Los nombres de los instructores los entrega Identidad; si falta alguno (cuenta dada de baja) se muestra
 * * «Instructor», y si Identidad no responde la consulta falla con 503 `ERR_ACA_17` (no se muestra a medias).
 */
@Service
@RequiredArgsConstructor
public class FranjaDisponibleService {

    static final int DIAS_MAXIMOS_DEL_RANGO = 28;
    static final String NOMBRE_SIN_DATOS = "Instructor";

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final Pattern FORMATO_HORA = Pattern.compile("^([01]\\d|2[0-3]):[0-5]\\d$");

    private final FranjaRepository franjaRepository;
    private final IdentidadClient identidadClient;
    private final Clock clock;

    /**
     * Sin fechas, desde hoy en adelante. Con las dos, el rango inclusivo de hasta 28 días; indicar una sola, un
     * rango al revés o demasiado largo es un error (`ERR_ACA_13`). `horaDesde` es inclusiva y `horaHasta`
     * exclusiva, ambas `HH:mm` (si no, `ERR_SYS_01`).
     */
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public Page<FranjaDisponibleResponse> listar(UUID escuelaCodigo, String bearer, LocalDate fechaDesde,
            LocalDate fechaHasta, UUID instructorCodigo, String horaDesde, String horaHasta, int page,
            int pageSize) {
        exigirEscuela(escuelaCodigo);
        validarRango(fechaDesde, fechaHasta);
        validarHora(horaDesde);
        validarHora(horaHasta);
        LocalDateTime ahora = LocalDateTime.now(clock);
        var pagina = Paginacion.de(page, pageSize, Sort.by("fecha", "horaInicio"));

        Page<Franja> franjas = franjaRepository.disponibles(escuelaCodigo, ahora.toLocalDate(),
            ahora.toLocalTime().format(HORA), fechaDesde, fechaHasta, instructorCodigo, horaDesde, horaHasta,
            pagina);
        Map<UUID, String> nombres = nombresDe(
            franjas.getContent().stream().map(Franja::getInstructorCodigo).toList(), bearer);
        return franjas.map(f -> new FranjaDisponibleResponse(f.getCodigoFranja(), f.getFecha(), f.getHoraInicio(),
            f.getHoraFin(), f.getInstructorCodigo(), nombres.getOrDefault(f.getInstructorCodigo(), NOMBRE_SIN_DATOS)));
    }

    /**
     * Los instructores con al menos una franja disponible en ese rango de fechas (o desde hoy), ordenados por
     * nombre.
     */
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public List<InstructorDisponibleResponse> instructores(UUID escuelaCodigo, String bearer, LocalDate fechaDesde,
            LocalDate fechaHasta) {
        exigirEscuela(escuelaCodigo);
        validarRango(fechaDesde, fechaHasta);
        LocalDateTime ahora = LocalDateTime.now(clock);

        List<UUID> codigos = franjaRepository.instructoresConDisponibles(escuelaCodigo, ahora.toLocalDate(),
            ahora.toLocalTime().format(HORA), fechaDesde, fechaHasta);
        Map<UUID, String> nombres = nombresDe(codigos, bearer);
        return codigos.stream()
            .map(c -> new InstructorDisponibleResponse(c, nombres.getOrDefault(c, NOMBRE_SIN_DATOS)))
            .sorted(Comparator.comparing(InstructorDisponibleResponse::nombre, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    private Map<UUID, String> nombresDe(List<UUID> codigos, String bearer) {
        if (codigos.isEmpty()) {
            return Map.of();
        }
        return identidadClient.nombresDeInstructores(codigos, bearer);
    }

    private static void exigirEscuela(UUID escuelaCodigo) {
        if (escuelaCodigo == null) {
            throw new ForbiddenException(AcademicoErrorCodes.TOKEN_SIN_ESCUELA,
                "Tu cuenta no pertenece a ninguna escuela");
        }
    }

    private static void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde == null && hasta == null) {
            return;
        }
        if (desde == null || hasta == null || hasta.isBefore(desde)
                || ChronoUnit.DAYS.between(desde, hasta) + 1 > DIAS_MAXIMOS_DEL_RANGO) {
            throw new ValidationException(AcademicoErrorCodes.RANGO_FECHAS_INVALIDO,
                "El rango de fechas no es válido: indica las dos fechas, en orden y de hasta "
                    + DIAS_MAXIMOS_DEL_RANGO + " días");
        }
    }

    private static void validarHora(String hora) {
        if (hora != null && !FORMATO_HORA.matcher(hora).matches()) {
            throw new ValidationException(AcademicoErrorCodes.VALIDACION, "La hora debe tener el formato HH:mm");
        }
    }
}
