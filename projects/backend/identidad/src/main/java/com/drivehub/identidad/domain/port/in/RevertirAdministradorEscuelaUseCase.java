package com.drivehub.identidad.domain.port.in;

import java.util.UUID;

/**
 * CU05: deshacer la creación del Administrador de una escuela que no llegó a crearse
 * (solo el Super admin). Es idempotente: si no hay nada que revertir, no hace nada.
 */
public interface RevertirAdministradorEscuelaUseCase {

    void revertir(UUID escuelaCodigo);
}
