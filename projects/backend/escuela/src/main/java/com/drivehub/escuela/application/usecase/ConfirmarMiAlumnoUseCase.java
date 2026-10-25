package com.drivehub.escuela.application.usecase;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.drivehub.escuela.common.exception.EscuelaErrorCodes;
import com.drivehub.escuela.common.exception.NotFoundException;
import com.drivehub.escuela.domain.repository.AsignacionAlumnoRepository;

import lombok.RequiredArgsConstructor;

/**
 * CU29: confirma que un estudiante es alumno vigente del instructor que pregunta.
 *
 * * Un estudiante de otro instructor, de otra escuela o inexistente responde igual (`ERR_ESC_15`): no se
 * * distingue "no existe" de "no es tuyo".
 * * Sin log por lectura, salvo el WARN del caso anómalo.
 */
@Service
@RequiredArgsConstructor
public class ConfirmarMiAlumnoUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(ConfirmarMiAlumnoUseCase.class);

    private final AsignacionAlumnoRepository asignacionRepository;

    @PreAuthorize("hasRole('INSTRUCTOR')")
    public UUID ejecutar(UUID escuelaCodigo, UUID instructorUsuarioCodigo, UUID estudianteCodigo) {
        if (!asignacionRepository.existeAsignacionActiva(escuelaCodigo, instructorUsuarioCodigo, estudianteCodigo)) {
            LOG.warn("Alumno no asignado al instructor",
                kv("module", "asignaciones"), kv("errorCode", EscuelaErrorCodes.ALUMNO_NO_ASIGNADO));
            throw new NotFoundException(EscuelaErrorCodes.ALUMNO_NO_ASIGNADO,
                "El alumno no está asignado a este instructor");
        }
        return estudianteCodigo;
    }
}
