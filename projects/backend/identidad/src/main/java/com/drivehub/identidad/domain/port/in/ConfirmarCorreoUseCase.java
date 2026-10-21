package com.drivehub.identidad.domain.port.in;

/**
 * Caso de uso CU04b: la persona abre el enlace del correo y la dirección queda verificada.
 */
public interface ConfirmarCorreoUseCase {

    /** El token en claro que viaja en el enlace. */
    void confirmar(String token);
}
