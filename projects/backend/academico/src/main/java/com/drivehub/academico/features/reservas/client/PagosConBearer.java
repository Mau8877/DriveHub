package com.drivehub.academico.features.reservas.client;

import java.util.UUID;

/**
 * {@link ClienteDePagos} con el Bearer del estudiante (CU44): envuelve al {@link PagosClient} de CU14 sin cambiarlo.
 *
 * * Solo sirve dentro de una petición del estudiante: Pagos vuelve a validar el token y solo le deja gastar sus
 * * propias horas.
 * * Ignora `escuelaCodigo`: la sesión del estudiante ya determina su escuela.
 * * NO es un bean: se construye por petición con el Bearer recibido (ver {@link ClientesDePagos}).
 */
public class PagosConBearer implements ClienteDePagos {

    private final PagosClient pagosClient;
    private final String bearer;

    public PagosConBearer(PagosClient pagosClient, String bearer) {
        this.pagosClient = pagosClient;
        this.bearer = bearer;
    }

    @Override
    public PagosClient.ResultadoConsumo descontar(UUID escuelaCodigo, UUID estudianteCodigo, UUID instructorCodigo,
            UUID claseCodigo, int horas, String idempotencyKey) {
        return pagosClient.descontar(bearer, estudianteCodigo, instructorCodigo, claseCodigo, horas, idempotencyKey);
    }

    @Override
    public void devolver(UUID escuelaCodigo, UUID estudianteCodigo, UUID claseCodigo, String idempotencyKey) {
        pagosClient.devolver(bearer, estudianteCodigo, claseCodigo, idempotencyKey);
    }
}
