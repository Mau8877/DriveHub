package com.drivehub.academico.features.horarios.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.drivehub.academico.common.exception.AcademicoErrorCodes;
import com.drivehub.academico.common.exception.ForbiddenException;
import com.drivehub.academico.common.exception.ValidationException;
import com.drivehub.academico.features.horarios.dto.ResumenFranjasInstructorResponse;
import com.drivehub.academico.features.horarios.enums.EstadoFranja;
import com.drivehub.academico.features.horarios.model.Franja;
import com.drivehub.academico.features.horarios.repository.FranjaRepository;

import lombok.RequiredArgsConstructor;

/**
 * Cuántas franjas tiene cada instructor de la escuela en un día (CU11): la vista del Administrador, que
 * Escuela usa para decir quién está disponible.
 *
 * * La escuela sale del token, nunca de un parámetro. Sin ella no hay sobre qué resumir: `ERR_ACA_16`.
 * * Solo vienen los instructores con al menos una franja en la ventana; un instructor sin franjas no aparece.
 * * Una franja `LIBRE` cuenta como libre solo si todavía no empezó (mismo criterio que `disponibles`, CU14):
 * * «ahora» es el del reloj del servicio.
 * * Lectura: no se registra cada consulta (LOGGING_BACKEND.md).
 */
@Service
@RequiredArgsConstructor
public class FranjaResumenService {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final Pattern FORMATO_HORA = Pattern.compile("^([01]\\d|2[0-3]):[0-5]\\d$");

    private final FranjaRepository franjaRepository;
    private final Clock clock;

    /**
     * `fecha` nula es hoy. `horaDesde` es inclusiva y `horaHasta` exclusiva, ambas `HH:mm` y opcionales; con las
     * dos, la primera tiene que ser anterior a la segunda (si no, `ERR_SYS_01`).
     */
    @PreAuthorize("hasRole('ADMIN_ESCUELA')")
    public List<ResumenFranjasInstructorResponse> resumirDelDia(UUID escuelaCodigo, LocalDate fecha,
            String horaDesde, String horaHasta) {
        if (escuelaCodigo == null) {
            throw new ForbiddenException(AcademicoErrorCodes.TOKEN_SIN_ESCUELA,
                "Tu cuenta no pertenece a ninguna escuela");
        }
        validarHora(horaDesde);
        validarHora(horaHasta);
        if (horaDesde != null && horaHasta != null && horaDesde.compareTo(horaHasta) >= 0) {
            throw new ValidationException(AcademicoErrorCodes.VALIDACION,
                "La hora de inicio tiene que ser anterior a la hora de fin");
        }
        LocalDateTime ahora = LocalDateTime.now(clock);
        LocalDate dia = fecha == null ? ahora.toLocalDate() : fecha;
        String horaActual = ahora.toLocalTime().format(HORA);

        Map<UUID, List<Franja>> porInstructor = franjaRepository.delDia(escuelaCodigo, dia, horaDesde, horaHasta)
            .stream().collect(Collectors.groupingBy(Franja::getInstructorCodigo));

        return porInstructor.entrySet().stream()
            .map(e -> resumir(e.getKey(), e.getValue(), ahora.toLocalDate(), horaActual))
            .sorted(Comparator.comparing(ResumenFranjasInstructorResponse::instructorCodigo))
            .toList();
    }

    private static ResumenFranjasInstructorResponse resumir(UUID instructor, List<Franja> franjas, LocalDate hoy,
            String horaActual) {
        int libres = 0;
        int reservadas = 0;
        int bloqueadas = 0;
        for (Franja franja : franjas) {
            if (franja.getEstado() == EstadoFranja.RESERVADA) {
                reservadas++;
            } else if (franja.getEstado() == EstadoFranja.BLOQUEADA) {
                bloqueadas++;
            } else if (franja.getEstado() == EstadoFranja.LIBRE && !yaEmpezo(franja, hoy, horaActual)) {
                libres++;
            }
        }
        return new ResumenFranjasInstructorResponse(instructor, franjas.size(), libres, reservadas, bloqueadas);
    }

    // * Empezó si es de un día anterior a hoy, o de hoy con hora de inicio igual o anterior a la actual
    private static boolean yaEmpezo(Franja franja, LocalDate hoy, String horaActual) {
        return franja.getFecha().isBefore(hoy)
            || (franja.getFecha().isEqual(hoy) && franja.getHoraInicio().compareTo(horaActual) <= 0);
    }

    private static void validarHora(String hora) {
        if (hora != null && !FORMATO_HORA.matcher(hora).matches()) {
            throw new ValidationException(AcademicoErrorCodes.VALIDACION, "La hora debe tener el formato HH:mm");
        }
    }
}
