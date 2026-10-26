package com.drivehub.identidad.domain.port.in;

import java.util.List;
import java.util.UUID;

/**
 * CU14: el nombre de varios instructores de la escuela de quien consulta, por código.
 */
public interface ConsultarNombresInstructoresUseCase {

    /**
     * Devuelve solo los que existen en la escuela del solicitante con rol INSTRUCTOR; los demás se omiten
     * sin error.
     *
     * @param codigos de 1 a 100 códigos (los repetidos se consultan una vez)
     * @throws com.drivehub.identidad.common.exception.ValidationException `ERR_SYS_01` si llegan 0 o más de 100
     */
    List<NombreInstructor> consultar(UUID solicitanteCodigo, List<UUID> codigos);
}
