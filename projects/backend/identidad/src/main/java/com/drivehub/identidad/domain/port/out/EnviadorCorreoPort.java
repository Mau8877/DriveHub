package com.drivehub.identidad.domain.port.out;

/**
 * Lo que el dominio necesita para mandar correos a las personas. Hoy se
 * implementa con SMTP (Mailpit en desarrollo); si falla el envío, lanza una
 * excepción y el caso de uso decide qué hacer.
 */
public interface EnviadorCorreoPort {

    /** Correo con el enlace para verificar la dirección (CU04b). */
    void enviarVerificacionCorreo(String destinatario, String nombres, String enlace);

    /** Correo con el enlace para elegir una contraseña nueva (CU04). */
    void enviarRecuperacionContrasena(String destinatario, String nombres, String enlace);

    /** Correo con el enlace para confirmar el cambio de correo; va a la dirección NUEVA (CU06). */
    void enviarCambioCorreo(String correoNuevo, String nombres, String enlace);

    /** Aviso informativo al correo ANTERIOR de que el correo de la cuenta cambió; sin enlaces (CU06). */
    void enviarAvisoCambioCorreo(String correoAnterior, String nombres, String correoNuevo);
}
