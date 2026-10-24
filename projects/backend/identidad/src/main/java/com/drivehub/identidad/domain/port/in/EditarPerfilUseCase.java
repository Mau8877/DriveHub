package com.drivehub.identidad.domain.port.in;

public interface EditarPerfilUseCase {

    PerfilUsuario editar(EditarPerfilComando comando);
}
