package com.drivehub.identidad.domain.port.out;

import java.time.Instant;
import java.util.Optional;

import com.drivehub.identidad.domain.model.SolicitudRegistroWhatsapp;

public interface SolicitudRegistroWhatsappRepositoryPort {

    SolicitudRegistroWhatsapp guardar(SolicitudRegistroWhatsapp solicitud);

    Optional<SolicitudRegistroWhatsapp> buscarPorTokenHash(String tokenHash);

    /** Borra de verdad las solicitudes pendientes de ese número (la nueva reemplaza a las anteriores). */
    void eliminarDelTelefono(String telefono);

    /** Borra de verdad la solicitud ya completada: sus datos personales no se conservan. */
    void eliminar(SolicitudRegistroWhatsapp solicitud);

    /** Borra de verdad las vencidas; devuelve cuántas. */
    int eliminarVencidas(Instant ahora);
}
