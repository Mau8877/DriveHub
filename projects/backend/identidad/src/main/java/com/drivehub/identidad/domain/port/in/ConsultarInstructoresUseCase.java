package com.drivehub.identidad.domain.port.in;

import java.util.List;
import java.util.UUID;

/**
 * CU09: los datos de persona de varios instructores de la escuela del Administrador, por código.
 */
public interface ConsultarInstructoresUseCase {

    /**
     * Devuelve solo los que existen en esa escuela con rol INSTRUCTOR; los demás se omiten sin error.
     *
     * @param codigos de 1 a 100 códigos (los repetidos se consultan una vez)
     * @throws com.drivehub.identidad.common.exception.ValidationException `ERR_SYS_01` si llegan 0 o más de 100
     */
    List<PersonaInstructor> consultar(UUID administradorCodigo, List<UUID> codigos);
}
