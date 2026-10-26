package com.drivehub.academico.features.reservas.client;

/**
 * Pagos respondió que el estudiante no tiene horas suficientes (`ERR_PAG_09`). No es una falla del servicio: es un
 * resultado esperado de descontar, por eso es distinta de `ServicioNoDisponibleException`.
 */
public class SinHorasException extends RuntimeException {

    public SinHorasException() {
        super("El estudiante no tiene horas suficientes");
    }
}
