package com.drivehub.identidad.domain.port.in;

/**
 * CU08 (F3): el Administrador de escuela corrige la categoría y/o la ubicación de un estudiante
 * de su escuela.
 */
public interface EditarEstudianteUseCase {

    /**
     * @throws com.drivehub.identidad.common.exception.NotFoundException `ERR_IDE_20` si el estudiante no
     *         existe o es de otra escuela
     * @throws com.drivehub.identidad.common.exception.ValidationException `ERR_IDE_21` si la categoría no
     *         existe; `ERR_SYS_01` si no llega nada que cambiar o llega solo una coordenada
     */
    EstudianteDetalle editar(EditarEstudianteComando comando);
}
