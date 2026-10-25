package com.drivehub.identidad.application.service;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.identidad.common.exception.ValidationException;
import com.drivehub.identidad.domain.model.PersonaEstudiante;
import com.drivehub.identidad.domain.port.in.ConsultarEstudiantesUseCase;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

import lombok.RequiredArgsConstructor;

/**
 * CU29: el nombre de un lote de estudiantes de la escuela del instructor o del Administrador.
 *
 * * La escuela sale de la cuenta de quien consulta. Un código de otra escuela, de alguien que no es
 * * estudiante o inexistente se omite sin error.
 * * Sin log por lectura (LOGGING_BACKEND.md no pide registrar cada GET).
 */
@Service
@RequiredArgsConstructor
public class ConsultarEstudiantesService implements ConsultarEstudiantesUseCase {

    static final int MAXIMO_CODIGOS = 100;

    private final UsuarioRepositoryPort usuarioRepository;
    private final EscuelaDelAdministrador escuelaDelSolicitante;

    @Override
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN_ESCUELA')")
    @Transactional(readOnly = true)
    public List<PersonaEstudiante> consultar(UUID solicitanteCodigo, List<UUID> codigos) {
        UUID escuela = escuelaDelSolicitante.de(solicitanteCodigo);
        if (codigos == null || codigos.isEmpty() || codigos.size() > MAXIMO_CODIGOS) {
            throw new ValidationException("ERR_SYS_01", "Debes enviar entre 1 y " + MAXIMO_CODIGOS + " códigos");
        }
        List<UUID> sinRepetidos = codigos.stream().distinct().toList();
        return usuarioRepository.buscarEstudiantesPorCodigos(escuela, sinRepetidos).stream()
            .map(u -> new PersonaEstudiante(u.getCodigo(), u.getPerfil().getNombres(), u.getPerfil().getApellidos()))
            .toList();
    }
}
