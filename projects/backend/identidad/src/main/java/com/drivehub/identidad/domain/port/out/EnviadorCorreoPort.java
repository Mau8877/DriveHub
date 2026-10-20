package com.drivehub.identidad.domain.port.out;

/**
 * Lo que el dominio necesita para mandar correos a las personas. Hoy se
 * implementa con SMTP (Mailpit en desarrollo); si falla el envío, lanza una
 * excepción y el caso de uso decide qué hacer.
 */
public interface EnviadorCorreoPort {

    /** Correo con el enlace para verificar la dirección (CU04b). */
    void enviarVerificacionCorreo(String destinatario, String nombres, String enlace);
}
