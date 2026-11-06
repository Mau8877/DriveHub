package com.drivehub.identidad.domain.port.in;

/** CU48: con el enlace y la contraseña, crear la cuenta y vincular el número si está libre. */
public interface CompletarRegistroWhatsappUseCase {

    /** @return si el número quedó vinculado a la cuenta nueva (no lo queda si ya era de otra) */
    boolean completar(String token, String password);
}
