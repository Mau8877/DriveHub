package com.drivehub.escuela.application.usecase;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.escuela.application.dto.EscuelaOutput;
import com.drivehub.escuela.application.dto.ListarEscuelasInput;
import com.drivehub.escuela.application.dto.PaginaOutput;
import com.drivehub.escuela.domain.entity.Escuela;
import com.drivehub.escuela.domain.repository.EscuelaRepository;
import com.drivehub.escuela.domain.repository.FiltroEscuelas;
import com.drivehub.escuela.domain.repository.Pagina;

import lombok.RequiredArgsConstructor;

/**
 * CU05: el Super admin lista las escuelas, activas e inactivas, de la más reciente a la más
 * antigua, con búsqueda por nombre o slug y filtro de estado.
 *
 * * Sin log por lectura: LOGGING_BACKEND.md no pide registrar cada GET.
 */
@Service
@RequiredArgsConstructor
public class ListarEscuelasUseCase {

    static final int TAMANO_POR_DEFECTO = 20;
    static final int TAMANO_MAXIMO = 100;

    private final EscuelaRepository escuelaRepository;

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional(readOnly = true)
    public PaginaOutput<EscuelaOutput> ejecutar(ListarEscuelasInput input) {
        int pagina = input.pagina() == null || input.pagina() < 1 ? 1 : input.pagina();
        int tamano = input.tamano() == null || input.tamano() < 1
            ? TAMANO_POR_DEFECTO : Math.min(input.tamano(), TAMANO_MAXIMO);

        Pagina<Escuela> resultado = escuelaRepository.listar(
            new FiltroEscuelas(input.busqueda(), input.estado()), pagina, tamano);

        int totalPaginas = (int) Math.ceil((double) resultado.total() / tamano);
        return new PaginaOutput<>(
            resultado.elementos().stream().map(ListarEscuelasUseCase::aSalida).toList(),
            pagina, tamano, resultado.total(), totalPaginas);
    }

    static EscuelaOutput aSalida(Escuela escuela) {
        return new EscuelaOutput(escuela.getCodigo(), escuela.getSlug(), escuela.getNombre(),
            escuela.estado(), escuela.getCreadoEn());
    }
}
