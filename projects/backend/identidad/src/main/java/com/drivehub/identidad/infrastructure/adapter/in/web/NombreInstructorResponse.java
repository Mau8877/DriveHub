package com.drivehub.identidad.infrastructure.adapter.in.web;

import java.util.UUID;

import com.drivehub.identidad.domain.port.in.NombreInstructor;

/**
 * El nombre de un instructor (CU14). Nada más: sin id interno, correo, CI ni teléfono.
 */
public record NombreInstructorResponse(UUID codigo, String nombreCompleto) {

    public static NombreInstructorResponse de(NombreInstructor n) {
        return new NombreInstructorResponse(n.codigo(), n.nombreCompleto());
    }
}
