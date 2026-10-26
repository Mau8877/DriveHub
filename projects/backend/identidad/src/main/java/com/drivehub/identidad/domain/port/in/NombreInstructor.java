package com.drivehub.identidad.domain.port.in;

import java.util.UUID;

/**
 * Lo que un estudiante o un instructor ve de otro instructor de su escuela (CU14): solo su código y su nombre.
 *
 * * Nada más: ni correo, ni CI, ni teléfono, ni foto. Quien necesite otro dato lo pide con su propio caso de uso.
 */
public record NombreInstructor(UUID codigo, String nombreCompleto) {
}
