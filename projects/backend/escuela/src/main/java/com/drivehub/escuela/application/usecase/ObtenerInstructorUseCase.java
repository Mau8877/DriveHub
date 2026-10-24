package com.drivehub.escuela.application.usecase;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.drivehub.escuela.application.dto.InstructorDetalleOutput;
import com.drivehub.escuela.common.exception.EscuelaErrorCodes;
import com.drivehub.escuela.common.exception.NotFoundException;
import com.drivehub.escuela.domain.entity.Instructor;
import com.drivehub.escuela.domain.gateway.InstructorCuentaGateway;
import com.drivehub.escuela.domain.gateway.PersonaInstructor;
import com.drivehub.escuela.domain.repository.InstructorRepository;

import lombok.RequiredArgsConstructor;

/**
 * CU09: el Administrador de escuela abre la ficha completa de un instructor de SU escuela.
 *
 * * Un instructor de otra escuela y uno inexistente responden igual (`ERR_ESC_13`): no se distinguen.
 * * Los datos personales los trae Identidad; si no devuelve a la persona, esos datos van en `null`.
 * * Sin log por lectura: LOGGING_BACKEND.md no pide registrar cada GET. No abre una transacción
 * * propia (ver {@link ListarInstructoresUseCase}).
 */
@Service
@RequiredArgsConstructor
public class ObtenerInstructorUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(ObtenerInstructorUseCase.class);

    private final InstructorRepository instructorRepository;
    private final InstructorCuentaGateway cuentaGateway;
    private final InstructorMapeador mapeador;

    @PreAuthorize("hasRole('ADMIN_ESCUELA')")
    public InstructorDetalleOutput ejecutar(UUID escuelaCodigo, UUID instructorCodigo) {
        Instructor ficha = instructorRepository.buscarPorCodigo(escuelaCodigo, instructorCodigo)
            .orElseThrow(() -> {
                LOG.warn("Instructor no encontrado en la escuela",
                    kv("module", "instructores"), kv("errorCode", EscuelaErrorCodes.INSTRUCTOR_NO_ENCONTRADO));
                return new NotFoundException(EscuelaErrorCodes.INSTRUCTOR_NO_ENCONTRADO, "El instructor no existe");
            });

        PersonaInstructor persona = cuentaGateway.consultar(java.util.List.of(ficha.getUsuarioCodigo())).stream()
            .findFirst().orElse(null);
        if (persona == null) {
            LOG.warn("Identidad no devolvió a la persona de un instructor",
                kv("module", "instructores"), kv("instructorCodigo", ficha.getCodigo()));
        }
        return mapeador.aDetalle(ficha, persona);
    }
}
