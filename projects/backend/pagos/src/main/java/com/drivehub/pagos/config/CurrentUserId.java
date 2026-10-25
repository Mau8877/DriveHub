package com.drivehub.pagos.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Inyecta en un parámetro `UUID` del controller el código de la persona autenticada: el `sub` del JWT, que es
 * su `codigo_usuario` de Identidad (ver SEGURIDAD_AUTH_BACKEND.md). Nunca se parsea el header a mano.
 *
 * * Las rutas que la usan ya exigen autenticación; si aun así el token no trae un `sub` válido, se responde
 * * como una sesión inválida (401).
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUserId {
}
