package com.drivehub.academico.features.reservas.client;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * La fábrica de clientes de Pagos (CU44): entrega el cliente del estudiante para lo que ocurre dentro de su petición.
 * El cliente de servicio es un bean aparte (`clienteDeServicio`).
 */
@Component
@RequiredArgsConstructor
public class ClientesDePagos {

    private final PagosClient pagosClient;

    /** El cliente que habla con Pagos como el estudiante, con la cabecera `Authorization` completa de su petición. */
    public ClienteDePagos conBearer(String bearer) {
        return new PagosConBearer(pagosClient, bearer);
    }
}
