package com.drivehub.identidad.domain.port.out;

/**
 * Escuela no pudo responder una consulta (caída, timeout o una respuesta inesperada).
 *
 * * No es una excepción de negocio: no lleva código de error catalogado y el cliente la recibe como
 * * el 500 genérico, sin detalles.
 */
public class EscuelaNoDisponibleException extends RuntimeException {

    public EscuelaNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }

    public EscuelaNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
