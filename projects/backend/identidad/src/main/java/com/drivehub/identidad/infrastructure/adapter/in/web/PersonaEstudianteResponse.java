package com.drivehub.identidad.infrastructure.adapter.in.web;

import java.util.UUID;

import com.drivehub.identidad.domain.model.PersonaEstudiante;

/**
 * El nombre de un estudiante (CU29). Nada más: sin id interno, correo, CI ni teléfono.
 */
public record PersonaEstudianteResponse(UUID codigo, String nombres, String apellidos) {

    public static PersonaEstudianteResponse de(PersonaEstudiante p) {
        return new PersonaEstudianteResponse(p.codigo(), p.nombres(), p.apellidos());
    }
}
