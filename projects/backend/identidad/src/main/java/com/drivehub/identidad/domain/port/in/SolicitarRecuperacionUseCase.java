package com.drivehub.identidad.domain.port.in;

/**
 * Caso de uso CU04: la persona que olvidó su contraseña pide el enlace para elegir una nueva.
 * Nunca revela si el correo existe, está verificado o la cuenta está inactiva.
 */
public interface SolicitarRecuperacionUseCase {

    void solicitar(String email);
}
