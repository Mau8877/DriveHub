package com.drivehub.identidad.domain.port.in;

import com.drivehub.identidad.domain.model.Usuario;

/**
 * CU05: crear la cuenta del Administrador de una escuela (solo el Super admin).
 */
public interface CrearAdministradorEscuelaUseCase {

    Usuario crear(CrearAdministradorEscuelaComando comando);
}
