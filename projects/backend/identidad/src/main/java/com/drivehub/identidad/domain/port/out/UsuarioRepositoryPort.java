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

    /** Si la escuela ya tiene una cuenta con rol ADMIN_ESCUELA (CU05). */
    boolean existeAdministradorDeEscuela(UUID escuelaCodigo);

    /** La cuenta ADMIN_ESCUELA de esa escuela, si la hay (CU05). */
    Optional<Usuario> buscarAdministradorDeEscuela(UUID escuelaCodigo);

    /**
     * Borra la cuenta y todo lo que cuelga de ella (roles, sesiones, enlaces de correo), de forma
     * FÍSICA. Solo para revertir la creación de un Administrador sin uso (CU05).
     */
    void eliminarDefinitivamente(UUID usuarioCodigo);

    /** Persiste el estado de acceso: intentos fallidos, estado, bloqueo y último ingreso. */
    Usuario actualizar(Usuario usuario);
}
