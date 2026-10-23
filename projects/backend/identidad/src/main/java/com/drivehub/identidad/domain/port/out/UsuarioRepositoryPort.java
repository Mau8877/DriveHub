package com.drivehub.identidad.domain.port.out;

import java.util.Optional;
import java.util.UUID;

import com.drivehub.identidad.domain.model.Usuario;

public interface UsuarioRepositoryPort {

    /** El correo ya viene normalizado (sin espacios y en minúsculas). */
    boolean existePorEmail(String email);

    /** Si otra cuenta ya usa ese CI (CU08a). El CI ya viene normalizado (sin espacios y en mayúsculas). */
    boolean existePorCi(String ci);

    /** Si una cuenta distinta a la de ese código ya usa ese CI (CU08a: editar el perfil propio). */
    boolean existePorCiDeOtro(String ci, UUID usuarioCodigo);

    /** Guarda la cuenta y su perfil juntos, en la misma transacción (CU08a). */
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

    /**
     * Persiste los datos de la cuenta que cambian después de crearla: estado de acceso (intentos,
     * estado, bloqueo, último ingreso), contraseña, correo y su verificación, y los datos del
     * perfil (nombres, apellidos, teléfono, fecha de nacimiento, foto, CI, sexo, ubicación y categoría).
     * Los roles y la escuela no.
     */
    Usuario actualizar(Usuario usuario);
}
