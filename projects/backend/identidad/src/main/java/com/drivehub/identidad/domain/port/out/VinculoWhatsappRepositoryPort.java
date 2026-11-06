package com.drivehub.identidad.domain.port.out;

import java.util.Optional;
import java.util.UUID;

import com.drivehub.identidad.domain.model.VinculoWhatsapp;

public interface VinculoWhatsappRepositoryPort {

    Optional<VinculoWhatsapp> buscarPorTelefonoHash(String telefonoHash);

    Optional<VinculoWhatsapp> buscarPorUsuario(UUID usuarioCodigo);

    /**
     * Deja un único vínculo entre ese número y esa cuenta: borra el que tuviera el número y el que
     * tuviera la cuenta, y guarda el nuevo (un número, una cuenta; una cuenta, un número).
     */
    VinculoWhatsapp reemplazar(UUID usuarioCodigo, String telefonoHash);
}
