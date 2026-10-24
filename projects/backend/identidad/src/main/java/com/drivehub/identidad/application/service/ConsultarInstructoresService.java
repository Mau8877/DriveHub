package com.drivehub.identidad.application.service;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.identidad.common.exception.ValidationException;
import com.drivehub.identidad.domain.port.in.ConsultarInstructoresUseCase;
import com.drivehub.identidad.domain.port.in.PersonaInstructor;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

import lombok.RequiredArgsConstructor;

/**
 * CU09: los datos de persona de un lote de instructores de la escuela del Administrador.
 *
 * * La escuela sale de la cuenta del Administrador. Solo devuelve instructores de esa escuela: un
 * * código de un estudiante o de otra escuela se omite sin error.
 * * Sin log por lectura (LOGGING_BACKEND.md no pide registrar cada GET).
 */
@Service
@RequiredArgsConstructor
public class ConsultarInstructoresService implements ConsultarInstructoresUseCase {

    static final int MAXIMO_CODIGOS = 100;

    private final UsuarioRepositoryPort usuarioRepository;
    private final EscuelaDelAdministrador escuelaDelAdministrador;
    private final PersonaInstructorMapeador mapeador;

    @Override
    @PreAuthorize("hasRole('ADMIN_ESCUELA')")
    @Transactional(readOnly = true)
    public List<PersonaInstructor> consultar(UUID administradorCodigo, List<UUID> codigos) {
        UUID escuela = escuelaDelAdministrador.de(administradorCodigo);
        if (codigos == null || codigos.isEmpty() || codigos.size() > MAXIMO_CODIGOS) {
            throw new ValidationException("ERR_SYS_01", "Debes enviar entre 1 y " + MAXIMO_CODIGOS + " códigos");
        }
        List<UUID> sinRepetidos = codigos.stream().distinct().toList();
        return usuarioRepository.buscarInstructoresPorCodigos(escuela, sinRepetidos).stream()
            .map(mapeador::aPersona).toList();
    }
}
