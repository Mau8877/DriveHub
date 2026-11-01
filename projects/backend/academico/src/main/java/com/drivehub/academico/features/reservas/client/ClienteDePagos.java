package com.drivehub.academico.features.reservas.client;

import java.util.UUID;

/**
 * Lo que Académico le pide a la billetera de horas de Pagos, sin importar con qué credencial habla (CU44).
 *
 * * Dos implementaciones: {@link PagosConBearer} (el Bearer del estudiante, para lo que ocurre dentro de su petición)
 * * y {@link PagosServicioClient} (la credencial de servicio, para el proceso programado y para lo que hace otra
 * * persona o el sistema). El orquestador recibe el cliente que corresponde en cada llamada.
 * * Las dos dan el mismo resultado ante los mismos errores: sin horas es {@link SinHorasException}, devolver una clase
 * * que no descontó nada (`404 ERR_PAG_16`) no es un error, y cualquier otra falla es `ServicioNoDisponibleException`.
 * * Siempre con clave de idempotencia: repetir la llamada no descuenta ni devuelve dos veces.
 */
public interface ClienteDePagos {

    /**
     * Descuenta horas de la billetera del estudiante por una clase.
     *
     * @param claseCodigo el código de la reserva: de ahí sale cuántas horas devolver si se cancela
     * @throws SinHorasException si no hay horas suficientes (no se descontó nada)
     */
    PagosClient.ResultadoConsumo descontar(UUID escuelaCodigo, UUID estudianteCodigo, UUID instructorCodigo,
            UUID claseCodigo, int horas, String idempotencyKey);

    /**
     * Devuelve lo que descontó la clase. Que no haya descontado nada no es un error.
     */
    void devolver(UUID escuelaCodigo, UUID estudianteCodigo, UUID claseCodigo, String idempotencyKey);
}
