package com.drivehub.identidad.domain.port.in;

/**
 * CU47: enviar al correo de una cuenta de estudiante el código para vincular un número de WhatsApp.
 * No dice si el correo tiene cuenta: termina igual en todos los casos.
 */
public interface SolicitarCodigoWhatsappUseCase {

    void solicitar(SolicitarCodigoWhatsappComando comando);
}
