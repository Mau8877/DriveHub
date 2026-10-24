package com.drivehub.identidad.domain.port.in;

import java.util.UUID;

/**
 * CU09: deshacer la creación de la cuenta de un instructor cuya ficha no se pudo guardar.
 */
public interface RevertirCuentaInstructorUseCase {

    /**
     * Idempotente: si la cuenta no existe (o no es un instructor de la escuela del Administrador),
     * no hace nada.
     *
     * @throws com.drivehub.identidad.common.exception.ConflictException `ERR_IDE_23` si la cuenta ya inició sesión
     */
    void revertir(UUID administradorCodigo, UUID instructorCodigo);
}
