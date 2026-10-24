package com.drivehub.identidad.domain.port.in;

import java.util.UUID;

/**
 * CU08 (F1): el Administrador de escuela consulta la ficha de un estudiante de su escuela.
 */
public interface ObtenerEstudianteUseCase {

    /**
     * @throws com.drivehub.identidad.common.exception.NotFoundException `ERR_IDE_20` si el estudiante no
     *         existe o es de otra escuela (no se distingue)
     */
    EstudianteDetalle obtener(UUID administradorCodigo, UUID estudianteCodigo);
}
