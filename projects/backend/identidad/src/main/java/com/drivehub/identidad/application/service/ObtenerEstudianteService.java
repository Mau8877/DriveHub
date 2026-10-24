package com.drivehub.identidad.application.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.common.exception.NotFoundException;
import com.drivehub.identidad.domain.port.in.EstudianteDetalle;
import com.drivehub.identidad.domain.port.in.ObtenerEstudianteUseCase;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

import lombok.RequiredArgsConstructor;

/**
 * CU08 (F1): el Administrador de escuela consulta la ficha de un estudiante de SU escuela.
 *
 * * Un estudiante inexistente y uno de otra escuela responden igual (ERR_IDE_20).
 * * Sin log por lectura: LOGGING_BACKEND.md no pide registrar cada GET.
 */
@Service
@RequiredArgsConstructor
public class ObtenerEstudianteService implements ObtenerEstudianteUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(ObtenerEstudianteService.class);
    private static final String MODULO = "estudiantes";

    private final UsuarioRepositoryPort usuarioRepository;
    private final EscuelaDelAdministrador escuelaDelAdministrador;
    private final EstudianteMapeador mapeador;

    @Override
    @PreAuthorize("hasRole('ADMIN_ESCUELA')")
    @Transactional(readOnly = true)
    public EstudianteDetalle obtener(UUID administradorCodigo, UUID estudianteCodigo) {
        UUID escuela = escuelaDelAdministrador.de(administradorCodigo);
        return usuarioRepository.buscarEstudianteDeEscuela(escuela, estudianteCodigo)
            .map(mapeador::aDetalle)
            .orElseThrow(() -> {
                LOG.warn("Estudiante no encontrado en la escuela",
                    kv("module", MODULO), kv("errorCode", IdentidadErrorCodes.ESTUDIANTE_NO_ENCONTRADO));
                return new NotFoundException(IdentidadErrorCodes.ESTUDIANTE_NO_ENCONTRADO,
                    "El estudiante no existe");
            });
    }
}
