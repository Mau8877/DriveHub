package com.drivehub.identidad.domain.port.in;

import java.util.UUID;

/**
 * Caso de uso CU04b: la persona con sesión pide el correo con el enlace para verificar su dirección.
 */
public interface SolicitarVerificacionCorreoUseCase {

    void solicitar(UUID usuarioCodigo);
}
