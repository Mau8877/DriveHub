package com.drivehub.identidad.domain.model;

/**
 * Estado de la cuenta.
 *
 * * BLOQUEADO lo pone el login al superar el máximo de intentos fallidos
 * * (usuario.intentos_fallidos); un login correcto reinicia el contador.
 */
public enum EstadoUsuario {
    ACTIVO,
    INACTIVO,
    BLOQUEADO
}
