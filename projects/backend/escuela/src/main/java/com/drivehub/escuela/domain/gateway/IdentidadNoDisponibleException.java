package com.drivehub.escuela.domain.gateway;

/**
 * Identidad no respondió, venció el tiempo de espera o contestó con un error de servidor.
 *
 * * No es una excepción de negocio: el resultado en Identidad es INCIERTO (la cuenta pudo
 * * crearse o no), así que el caso de uso compensa antes de relanzarla. Cae en el 500
 * * genérico (falla de infraestructura), como dice EXCEPCIONES_BACKEND.md.
 */
public class IdentidadNoDisponibleException extends RuntimeException {

    public IdentidadNoDisponibleException(String mensaje) {
        super(mensaje);
    }

    public IdentidadNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
