package com.drivehub.identidad.domain.port.in;

/**
 * Caso de uso CU03: cerrar la sesión del dispositivo actual. Es idempotente:
 * nunca falla por una sesión inexistente, vencida o ya cerrada.
 */
public interface CerrarSesionUseCase {

    void cerrar(CerrarSesionComando comando);
}
