package com.drivehub.identidad.domain.port.in;

public interface ConfirmarCambioCorreoUseCase {

    /** Confirma el enlace recibido en el correo nuevo; un enlace que no sirve responde siempre lo mismo. */
    void confirmar(String token);
}
