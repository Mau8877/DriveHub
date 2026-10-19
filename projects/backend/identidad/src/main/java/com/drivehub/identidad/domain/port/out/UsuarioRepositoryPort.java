package com.drivehub.identidad.domain.port.out;

import com.drivehub.identidad.domain.model.Usuario;

public interface UsuarioRepositoryPort {

    /** El correo ya viene normalizado (sin espacios y en minúsculas). */
    boolean existePorEmail(String email);

    Usuario guardar(Usuario usuario);
}
