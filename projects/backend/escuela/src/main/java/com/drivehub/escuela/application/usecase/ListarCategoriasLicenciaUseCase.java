package com.drivehub.escuela.application.usecase;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.escuela.application.dto.CategoriaLicenciaOutput;
import com.drivehub.escuela.application.dto.PaginaOutput;
import com.drivehub.escuela.domain.entity.CategoriaLicencia;
import com.drivehub.escuela.domain.repository.CategoriaLicenciaRepository;
import com.drivehub.escuela.domain.repository.Pagina;

import lombok.RequiredArgsConstructor;

/**
 * CU08: el Administrador de escuela lista el catálogo de categorías de licencia para elegir la de
 * un estudiante, en el orden del catálogo.
 *
 * * Sin log por lectura: LOGGING_BACKEND.md no pide registrar cada GET.
 */
@Service
@RequiredArgsConstructor
public class ListarCategoriasLicenciaUseCase {

    static final int TAMANO_POR_DEFECTO = 20;
    static final int TAMANO_MAXIMO = 100;

    private final CategoriaLicenciaRepository categoriaRepository;

    @PreAuthorize("hasRole('ADMIN_ESCUELA')")
    @Transactional(readOnly = true)
    public PaginaOutput<CategoriaLicenciaOutput> ejecutar(Integer pagina, Integer tamano) {
        int paginaEfectiva = pagina == null || pagina < 1 ? 1 : pagina;
        int tamanoEfectivo = tamano == null || tamano < 1 ? TAMANO_POR_DEFECTO : Math.min(tamano, TAMANO_MAXIMO);

        Pagina<CategoriaLicencia> resultado = categoriaRepository.listar(paginaEfectiva, tamanoEfectivo);

        int totalPaginas = (int) Math.ceil((double) resultado.total() / tamanoEfectivo);
        return new PaginaOutput<>(
            resultado.elementos().stream()
                .map(c -> new CategoriaLicenciaOutput(c.getSigla(), c.getDescripcion())).toList(),
            paginaEfectiva, tamanoEfectivo, resultado.total(), totalPaginas);
    }
}
