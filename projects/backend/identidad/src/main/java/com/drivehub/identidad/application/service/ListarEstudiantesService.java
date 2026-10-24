package com.drivehub.identidad.application.service;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.identidad.domain.port.in.ListarEstudiantesComando;
import com.drivehub.identidad.domain.port.in.ListarEstudiantesUseCase;
import com.drivehub.identidad.domain.port.in.PaginaEstudiantes;
import com.drivehub.identidad.domain.port.out.FiltroEstudiantes;
import com.drivehub.identidad.domain.port.out.PaginaUsuarios;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

import lombok.RequiredArgsConstructor;

/**
 * CU08: el Administrador de escuela lista los estudiantes de SU escuela, de la incorporación más
 * reciente a la más antigua, con búsqueda por nombre y filtro de estado.
 *
 * * La escuela sale de la cuenta del Administrador, nunca de un parámetro.
 * * Sin log por lectura: LOGGING_BACKEND.md no pide registrar cada GET.
 */
@Service
@RequiredArgsConstructor
public class ListarEstudiantesService implements ListarEstudiantesUseCase {

    static final int TAMANO_POR_DEFECTO = 20;
    static final int TAMANO_MAXIMO = 100;

    private final UsuarioRepositoryPort usuarioRepository;
    private final EscuelaDelAdministrador escuelaDelAdministrador;
    private final EstudianteMapeador mapeador;

    @Override
    @PreAuthorize("hasRole('ADMIN_ESCUELA')")
    @Transactional(readOnly = true)
    public PaginaEstudiantes listar(ListarEstudiantesComando comando) {
        UUID escuela = escuelaDelAdministrador.de(comando.administradorCodigo());
        int pagina = comando.pagina() == null || comando.pagina() < 1 ? 1 : comando.pagina();
        int tamano = comando.tamano() == null || comando.tamano() < 1
            ? TAMANO_POR_DEFECTO : Math.min(comando.tamano(), TAMANO_MAXIMO);
        String busqueda = comando.busqueda() == null || comando.busqueda().isBlank()
            ? null : comando.busqueda().trim();

        PaginaUsuarios resultado = usuarioRepository.listarEstudiantesDeEscuela(
            escuela, new FiltroEstudiantes(busqueda, comando.estado()), pagina, tamano);

        int totalPaginas = (int) Math.ceil((double) resultado.total() / tamano);
        return new PaginaEstudiantes(
            resultado.elementos().stream().map(mapeador::aResumen).toList(),
            pagina, tamano, resultado.total(), totalPaginas);
    }
}
