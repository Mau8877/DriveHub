package com.drivehub.identidad.domain.port.in;

import java.util.List;
import java.util.UUID;

import com.drivehub.identidad.domain.model.PersonaEstudiante;

/**
 * CU29: el nombre de varios estudiantes de la escuela de quien consulta, por código.
 */
public interface ConsultarEstudiantesUseCase {

    /**
     * Devuelve solo los que existen en la escuela del solicitante con rol ESTUDIANTE; los demás se omiten
     * sin error.
     *
     * @param codigos de 1 a 100 códigos (los repetidos se consultan una vez)
     * @throws com.drivehub.identidad.common.exception.ValidationException `ERR_SYS_01` si llegan 0 o más de 100
     */
    List<PersonaEstudiante> consultar(UUID solicitanteCodigo, List<UUID> codigos);
}
