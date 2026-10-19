package com.drivehub.identidad.domain.port.out;

import java.util.Optional;

import com.drivehub.identidad.domain.model.Rol;

public interface RolRepositoryPort {

    Optional<Rol> buscarPorNombre(String nombre);
}
