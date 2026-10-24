package com.drivehub.escuela.presentation.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.drivehub.escuela.application.dto.InstructorResumenOutput;
import com.drivehub.escuela.domain.entity.EstadoInstructor;

/**
 * Un instructor en el listado (CU09). Sin `usuarioCodigo`, `escuelaCodigo` ni ids internos.
 * `licenciaVence` viaja como `yyyy-MM-dd`.
 */
public record InstructorResumenResponse(
        UUID codigo,
        String nombreCompleto,
        String fotoUrl,
        String nroLicencia,
        LocalDate licenciaVence,
        List<CategoriaResponse> categorias,
        EstadoInstructor estado
) {

    public static InstructorResumenResponse de(InstructorResumenOutput o) {
        return new InstructorResumenResponse(o.codigo(), o.nombreCompleto(), o.fotoUrl(), o.nroLicencia(),
            o.licenciaVence(), o.categorias().stream().map(c -> new CategoriaResponse(c.sigla(), c.descripcion())).toList(),
            o.estado());
    }

    public record CategoriaResponse(String sigla, String descripcion) {
    }
}
