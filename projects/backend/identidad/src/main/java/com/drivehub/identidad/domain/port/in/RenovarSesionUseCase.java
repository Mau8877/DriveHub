package com.drivehub.identidad.domain.port.in;

/**
 * Caso de uso CU02: mantener la sesión renovándola (con rotación del token).
 */
public interface RenovarSesionUseCase {

    SesionEmitida renovar(RenovarSesionComando comando);
}
