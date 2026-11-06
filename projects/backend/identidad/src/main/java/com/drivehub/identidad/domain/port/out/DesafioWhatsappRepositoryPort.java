package com.drivehub.identidad.domain.port.out;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.drivehub.identidad.domain.model.DesafioWhatsapp;

public interface DesafioWhatsappRepositoryPort {

    DesafioWhatsapp guardar(DesafioWhatsapp desafio);

    /** Marca como usados los códigos que seguían sirviendo para esa cuenta (los reemplaza uno nuevo). */
    void invalidarPendientes(UUID usuarioCodigo);

    /** El código más reciente de esa cuenta y ese número que no se usó ni venció. */
    Optional<DesafioWhatsapp> buscarVigente(UUID usuarioCodigo, String telefonoHash, Instant ahora);

    void registrarIntentoFallido(UUID desafioCodigo);

    void marcarUsado(UUID desafioCodigo);

    /** Pedidos de código desde ese número, tengan o no cuenta detrás. */
    long contarPorTelefonoDesde(String telefonoHash, Instant desde);

    /** Pedidos de código para ese correo, tenga o no cuenta detrás. */
    long contarPorCorreoDesde(String correoHash, Instant desde);
}
