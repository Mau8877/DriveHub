package com.drivehub.identidad.application.service;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.identidad.common.exception.ValidationException;
import com.drivehub.identidad.domain.port.in.ConsultarNombresInstructoresUseCase;
import com.drivehub.identidad.domain.port.in.NombreInstructor;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

import lombok.RequiredArgsConstructor;

/**
 * CU14: el nombre de un lote de instructores de la escuela del estudiante o del instructor que consulta.
 *
 * * La escuela sale de la cuenta de quien consulta. Un código de otra escuela, de alguien que no es
 * * instructor o inexistente se omite sin error.
 * * Devuelve solo el nombre: la consulta del Administrador (`ConsultarInstructoresService`) trae además CI,
 * * correo y teléfono, que un estudiante no debe ver.
 * * Sin log por lectura (LOGGING_BACKEND.md no pide registrar cada GET).
 */
@Service
@RequiredArgsConstructor
public class ConsultarNombresInstructoresService implements ConsultarNombresInstructoresUseCase {

    static final int MAXIMO_CODIGOS = 100;

    private final UsuarioRepositoryPort usuarioRepository;
    private final EscuelaDelAdministrador escuelaDelSolicitante;

    @Override
    @PreAuthorize("hasAnyRole('ESTUDIANTE', 'INSTRUCTOR')")
    @Transactional(readOnly = true)
    public List<NombreInstructor> consultar(UUID solicitanteCodigo, List<UUID> codigos) {
        UUID escuela = escuelaDelSolicitante.de(solicitanteCodigo);
        if (codigos == null || codigos.isEmpty() || codigos.size() > MAXIMO_CODIGOS) {
            throw new ValidationException("ERR_SYS_01", "Debes enviar entre 1 y " + MAXIMO_CODIGOS + " códigos");
        }
        List<UUID> sinRepetidos = codigos.stream().distinct().toList();
        return usuarioRepository.buscarInstructoresPorCodigos(escuela, sinRepetidos).stream()
            .map(u -> new NombreInstructor(u.getCodigo(),
                (u.getPerfil().getNombres() + " " + u.getPerfil().getApellidos()).trim()))
            .toList();
    }
}
