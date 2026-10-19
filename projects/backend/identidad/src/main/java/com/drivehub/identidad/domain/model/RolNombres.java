package com.drivehub.identidad.domain.model;

/**
 * Nombres de los 4 roles del sistema.
 *
 * * Los roles son datos de la tabla rol (administrables sin desplegar), no un
 * * enum cerrado: acá solo viven como constantes de texto para no escribir
 * * strings sueltos en el código.
 */
public final class RolNombres {

    public static final String SUPER_ADMIN = "SUPER_ADMIN";
    public static final String ADMIN_ESCUELA = "ADMIN_ESCUELA";
    public static final String INSTRUCTOR = "INSTRUCTOR";
    public static final String ESTUDIANTE = "ESTUDIANTE";

    private RolNombres() {
    }
}
