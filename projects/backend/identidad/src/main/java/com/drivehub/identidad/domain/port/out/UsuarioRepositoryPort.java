package com.drivehub.identidad.domain.port.out;

import java.util.Optional;
import java.util.UUID;

import com.drivehub.identidad.domain.model.Usuario;

public interface UsuarioRepositoryPort {

    /** El correo ya viene normalizado (sin espacios y en minúsculas). */
    boolean existePorEmail(String email);

    Usuario guardar(Usuario usuario);

    /** El correo ya viene normalizado. Devuelve la cuenta con sus roles. */
    Optional<Usuario> buscarPorEmail(String email);

    Optional<Usuario> buscarPorCodigo(UUID codigo);

    /** Persiste el estado de acceso: intentos fallidos, estado, bloqueo y último ingreso. */
    Usuario actualizar(Usuario usuario);
}
