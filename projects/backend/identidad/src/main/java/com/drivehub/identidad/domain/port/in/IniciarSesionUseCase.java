package com.drivehub.identidad.domain.port.in;

/**
 * Caso de uso CU02: iniciar sesión.
 */
public interface IniciarSesionUseCase {

    SesionEmitida iniciar(IniciarSesionComando comando);
}
