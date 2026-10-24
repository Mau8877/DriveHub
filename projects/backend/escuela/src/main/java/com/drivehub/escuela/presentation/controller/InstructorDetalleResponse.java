package com.drivehub.escuela.presentation.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.drivehub.escuela.application.dto.InstructorDetalleOutput;
import com.drivehub.escuela.domain.entity.EstadoInstructor;

/**
 * La ficha completa de un instructor (CU09). Sin `usuarioCodigo`, `escuelaCodigo` ni ids internos.
 * Las fechas viajan como `yyyy-MM-dd`.
 */
public record InstructorDetalleResponse(
        UUID codigo,
        String nombres,
        String apellidos,
        String nombreCompleto,
        String fotoUrl,
        Integer edad,
        LocalDate fechaNacimiento,
        String sexo,
        String ci,
        String email,
        String telefono,
        String direccion,
        BigDecimal latitud,
        BigDecimal longitud,
        String nroLicencia,
        LocalDate licenciaVence,
        LocalDate fechaContratacion,
        List<InstructorResumenResponse.CategoriaResponse> categorias,
        EstadoInstructor estado,
        LocalDate fechaAlta
) {

    public static InstructorDetalleResponse de(InstructorDetalleOutput o) {
        return new InstructorDetalleResponse(o.codigo(), o.nombres(), o.apellidos(), o.nombreCompleto(), o.fotoUrl(),
            o.edad(), o.fechaNacimiento(), o.sexo(), o.ci(), o.email(), o.telefono(), o.direccion(), o.latitud(),
            o.longitud(), o.nroLicencia(), o.licenciaVence(), o.fechaContratacion(),
            o.categorias().stream().map(c -> new InstructorResumenResponse.CategoriaResponse(c.sigla(), c.descripcion())).toList(),
            o.estado(), o.fechaAlta());
    }
}
