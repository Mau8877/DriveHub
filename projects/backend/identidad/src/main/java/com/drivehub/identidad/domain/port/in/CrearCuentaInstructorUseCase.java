package com.drivehub.identidad.domain.port.in;

/**
 * CU09: crear la cuenta (rol INSTRUCTOR) y el perfil de un instructor en la escuela del Administrador.
 */
public interface CrearCuentaInstructorUseCase {

    /**
     * @throws com.drivehub.identidad.common.exception.ConflictException `ERR_IDE_01` correo en uso,
     *         `ERR_IDE_19` CI en uso
     * @throws com.drivehub.identidad.common.exception.ValidationException `ERR_IDE_02` menor de edad,
     *         `ERR_SYS_01` si el código ya existe
     */
    PersonaInstructor crear(CrearCuentaInstructorComando comando);
}
