package com.drivehub.escuela.presentation.controller;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Inyecta en un parámetro `UUID` del controller la escuela de la persona autenticada (el claim
 * `escuela` del JWT), sin parsear el header a mano ni recibirla del cliente.
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface EscuelaActual {
}
