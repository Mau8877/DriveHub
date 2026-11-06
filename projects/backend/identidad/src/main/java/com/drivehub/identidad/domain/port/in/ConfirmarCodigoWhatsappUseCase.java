package com.drivehub.identidad.domain.port.in;

/** CU47: comprobar el código del correo y vincular el número de WhatsApp a la cuenta. */
public interface ConfirmarCodigoWhatsappUseCase {

    void confirmar(ConfirmarCodigoWhatsappComando comando);
}
