package com.drivehub.identidad.application.service;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.identidad.domain.port.in.BuscarInstructoresComando;
import com.drivehub.identidad.domain.port.in.BuscarInstructoresUseCase;
import com.drivehub.identidad.domain.port.in.PaginaPersonasInstructor;
import com.drivehub.identidad.domain.port.out.PaginaUsuarios;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

import lombok.RequiredArgsConstructor;

/**
 * CU09: busca por nombre las personas instructor de la escuela del Administrador, de a páginas.
 *
 * * La escuela sale de la cuenta del Administrador. Sin log por lectura (LOGGING_BACKEND.md no pide
 * * registrar cada GET).
 */
@Service
@RequiredArgsConstructor
public class BuscarInstructoresService implements BuscarInstructoresUseCase {

    static final int TAMANO_POR_DEFECTO = 20;
    static final int TAMANO_MAXIMO = 100;

    private final UsuarioRepositoryPort usuarioRepository;
    private final EscuelaDelAdministrador escuelaDelAdministrador;
    private final PersonaInstructorMapeador mapeador;

    @Override
    @PreAuthorize("hasRole('ADMIN_ESCUELA')")
    @Transactional(readOnly = true)
    public PaginaPersonasInstructor buscar(BuscarInstructoresComando comando) {
        UUID escuela = escuelaDelAdministrador.de(comando.administradorCodigo());
        int pagina = comando.pagina() == null || comando.pagina() < 1 ? 1 : comando.pagina();
        int tamano = comando.tamano() == null || comando.tamano() < 1
            ? TAMANO_POR_DEFECTO : Math.min(comando.tamano(), TAMANO_MAXIMO);
        String busqueda = comando.busqueda() == null || comando.busqueda().isBlank()
            ? null : comando.busqueda().trim();

        PaginaUsuarios resultado = usuarioRepository.listarInstructoresDeEscuela(escuela, busqueda, pagina, tamano);

        int totalPaginas = (int) Math.ceil((double) resultado.total() / tamano);
        return new PaginaPersonasInstructor(
            resultado.elementos().stream().map(mapeador::aPersona).toList(),
            pagina, tamano, resultado.total(), totalPaginas);
    }
}
