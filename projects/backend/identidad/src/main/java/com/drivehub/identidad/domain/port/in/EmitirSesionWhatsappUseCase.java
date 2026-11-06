package com.drivehub.identidad.domain.port.in;

/** CU47: la sesión corta del estudiante cuyo número de WhatsApp está vinculado. */
public interface EmitirSesionWhatsappUseCase {

    SesionWhatsapp emitir(String telefono);
}
