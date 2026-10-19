package com.drivehub.identidad.domain.model;

import java.util.Set;

/**
 * Qué roles pueden usar cada plataforma (CU02).
 *
 * * Web: Super admin y Administrador de escuela. App móvil: Estudiante e
 * * Instructor. Con varios roles basta con que uno esté permitido.
 */
public final class AccesoPorPlataforma {

    private static final Set<String> ROLES_WEB = Set.of(RolNombres.SUPER_ADMIN, RolNombres.ADMIN_ESCUELA);
    private static final Set<String> ROLES_MOVIL = Set.of(RolNombres.ESTUDIANTE, RolNombres.INSTRUCTOR);

    private AccesoPorPlataforma() {
    }

    public static boolean permite(Dispositivo dispositivo, Set<Rol> roles) {
        Set<String> permitidos = dispositivo == Dispositivo.WEB ? ROLES_WEB : ROLES_MOVIL;
        return roles.stream().anyMatch(rol -> permitidos.contains(rol.getNombre()));
    }

    /** Nombre de la plataforma donde sí puede entrar quien no puede usar `dispositivo`. */
    public static String plataformaCorrecta(Dispositivo dispositivo) {
        return dispositivo == Dispositivo.WEB ? "la aplicación móvil" : "la web";
    }
}
