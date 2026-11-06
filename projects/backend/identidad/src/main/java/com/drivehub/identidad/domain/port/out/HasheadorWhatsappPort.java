package com.drivehub.identidad.domain.port.out;

/**
 * Hashes del chat de WhatsApp (CU47): el número, el correo y el código se guardan solo así.
 * Son HMAC con una clave secreta del servicio, no SHA simple: el código tiene solo un millón de valores
 * posibles y un hash sin clave se adivinaría de inmediato si se filtrara la base.
 */
public interface HasheadorWhatsappPort {

    /** Hash del número; acepta «+591 7123-4567» o «59171234567» y da el mismo resultado. */
    String hashearTelefono(String telefono);

    /** Hash del correo ya normalizado (sin espacios y en minúsculas). */
    String hashearCorreo(String correo);

    String hashearCodigo(String codigo);
}
