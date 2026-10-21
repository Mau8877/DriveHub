package com.drivehub.identidad.domain.port.in;

/**
 * Caso de uso CU04: fijar una contraseña nueva con el enlace recibido por correo.
 */
public interface RestablecerContrasenaUseCase {

    /** El token en claro que viaja en el enlace y la contraseña nueva en claro. */
    void restablecer(String token, String nuevaPassword);
}
