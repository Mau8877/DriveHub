package com.drivehub.identidad.domain.model;

import java.util.UUID;

/**
 * Lo que otros servicios ven de la persona de un estudiante (CU29): solo su código y su nombre.
 *
 * * Nada más: ni correo, ni CI, ni teléfono. Quien necesite otro dato lo pide con su propio caso de uso.
 */
public record PersonaEstudiante(UUID codigo, String nombres, String apellidos) {
}
