package com.drivehub.escuela.application.usecase;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.escuela.application.dto.CategoriaLicenciaOutput;
import com.drivehub.escuela.common.exception.EscuelaErrorCodes;
import com.drivehub.escuela.common.exception.NotFoundException;
import com.drivehub.escuela.domain.repository.CategoriaLicenciaRepository;

import lombok.RequiredArgsConstructor;

/**
 * CU08: consulta una categoría por su sigla. Es lo que usa Identidad para comprobar que la
 * categoría que eligió el Administrador existe.
 */
@Service
@RequiredArgsConstructor
public class ObtenerCategoriaLicenciaUseCase {

    private final CategoriaLicenciaRepository categoriaRepository;

    @PreAuthorize("hasRole('ADMIN_ESCUELA')")
    @Transactional(readOnly = true)
    public CategoriaLicenciaOutput ejecutar(String sigla) {
        return categoriaRepository.buscarPorSigla(sigla)
            .map(c -> new CategoriaLicenciaOutput(c.getSigla(), c.getDescripcion()))
            .orElseThrow(() -> new NotFoundException(EscuelaErrorCodes.CATEGORIA_NO_ENCONTRADA,
                "La categoría de licencia no existe"));
    }
}
