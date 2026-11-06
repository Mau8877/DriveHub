package com.drivehub.identidad.infrastructure.adapter.out.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.identidad.domain.model.VinculoWhatsapp;
import com.drivehub.identidad.domain.port.out.VinculoWhatsappRepositoryPort;
import com.drivehub.identidad.infrastructure.adapter.out.persistence.model.UsuarioModel;
import com.drivehub.identidad.infrastructure.adapter.out.persistence.model.VinculoWhatsappModel;

import lombok.RequiredArgsConstructor;

/**
 * Vínculos número-cuenta de WhatsApp (`vinculo_whatsapp`). Traduce el `usuarioCodigo` del dominio al
 * `usuario_id` interno: el id Long no sale de acá.
 */
@Component
@RequiredArgsConstructor
public class VinculoWhatsappPersistenceAdapter implements VinculoWhatsappRepositoryPort {

    private final VinculoWhatsappJpaRepository vinculoJpaRepository;
    private final UsuarioJpaRepository usuarioJpaRepository;

    @Override
    public Optional<VinculoWhatsapp> buscarPorTelefonoHash(String telefonoHash) {
        return vinculoJpaRepository.findConUsuarioPorTelefonoHash(telefonoHash).map(this::aDominio);
    }

    @Override
    public Optional<VinculoWhatsapp> buscarPorUsuario(UUID usuarioCodigo) {
        return vinculoJpaRepository.findConUsuarioPorUsuarioCodigo(usuarioCodigo).map(this::aDominio);
    }

    @Override
    @Transactional
    public VinculoWhatsapp reemplazar(UUID usuarioCodigo, String telefonoHash) {
        UsuarioModel usuario = usuarioJpaRepository.findByCodigo(usuarioCodigo)
            .orElseThrow(() -> new IllegalStateException("No existe la cuenta del vínculo"));
        vinculoJpaRepository.borrarDelNumeroOdeLaCuenta(telefonoHash, usuarioCodigo);
        return aDominio(vinculoJpaRepository.save(new VinculoWhatsappModel(usuario, telefonoHash)));
    }

    private VinculoWhatsapp aDominio(VinculoWhatsappModel model) {
        return VinculoWhatsapp.builder()
            .codigo(model.getCodigo())
            .usuarioCodigo(model.getUsuario().getCodigo())
            .telefonoHash(model.getTelefonoHash())
            .build();
    }
}
