package com.drivehub.identidad.infrastructure.adapter.in.web;

import java.time.LocalDate;
import java.util.UUID;

import com.drivehub.identidad.domain.model.EstadoEstudiante;
import com.drivehub.identidad.domain.port.in.EstudianteResumen;

/**
 * Una fila del listado de estudiantes (CU08). `fechaIncorporacion` viaja como `yyyy-MM-dd`.
 */
public record EstudianteResumenResponse(
        UUID codigo,
        String nombreCompleto,
        int edad,
        String categoriaSigla,
        EstadoEstudiante estado,
        LocalDate fechaIncorporacion
) {

    public static EstudianteResumenResponse de(EstudianteResumen e) {
        return new EstudianteResumenResponse(e.codigo(), e.nombreCompleto(), e.edad(), e.categoriaSigla(),
            e.estado(), e.fechaIncorporacion());
    }
}
