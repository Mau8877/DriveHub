package com.drivehub.academico.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Inyecta en un parámetro `UUID` del controller el código de la escuela de la
 * persona autenticada (el claim `escuela` del JWT). Es `null` si el token no lo
 * trae: el Super admin no pertenece a ninguna escuela.
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentEscuelaId {
}
