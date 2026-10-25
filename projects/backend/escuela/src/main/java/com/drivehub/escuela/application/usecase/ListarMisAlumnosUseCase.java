package com.drivehub.escuela.application.usecase;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.drivehub.escuela.application.dto.PaginaOutput;
import com.drivehub.escuela.domain.repository.AsignacionAlumnoRepository;
import com.drivehub.escuela.domain.repository.Pagina;

import lombok.RequiredArgsConstructor;

/**
 * CU29: el instructor lista los códigos de sus alumnos con asignación vigente.
 *
 * * El instructor es quien hace la petición (el `sub` del token), nunca un parámetro del cliente.
 * * Solo devuelve códigos: los nombres son de Identidad y las horas, de Pagos.
 * * Sin log por lectura: LOGGING_BACKEND.md no pide registrar cada GET. No abre una transacción propia: la
 * * abre el repositorio.
 */
@Service
@RequiredArgsConstructor
public class ListarMisAlumnosUseCase {

    static final int TAMANO_POR_DEFECTO = 20;
    static final int TAMANO_MAXIMO = 100;

    private final AsignacionAlumnoRepository asignacionRepository;

    @PreAuthorize("hasRole('INSTRUCTOR')")
    public PaginaOutput<UUID> ejecutar(UUID escuelaCodigo, UUID instructorUsuarioCodigo, Integer pagina,
            Integer tamano) {
        int paginaPedida = pagina == null || pagina < 1 ? 1 : pagina;
        int tamanoPedido = tamano == null || tamano < 1 ? TAMANO_POR_DEFECTO : Math.min(tamano, TAMANO_MAXIMO);

        Pagina<UUID> resultado = asignacionRepository.buscarEstudiantesDeInstructor(escuelaCodigo,
            instructorUsuarioCodigo, paginaPedida, tamanoPedido);

        int totalPaginas = (int) Math.ceil((double) resultado.total() / tamanoPedido);
        return new PaginaOutput<>(resultado.elementos(), paginaPedida, tamanoPedido, resultado.total(),
            totalPaginas);
    }
}
