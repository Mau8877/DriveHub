package com.drivehub.identidad.domain.port.in;

/** CU48: guardar los datos dictados por el chat y entregar el enlace para fijar la contraseña. */
public interface CrearSolicitudRegistroWhatsappUseCase {

    EnlaceRegistro crear(CrearSolicitudRegistroWhatsappComando comando);
}
