package com.drivehub.identidad.infrastructure.adapter.out.persistence.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.identidad.domain.model.DesafioWhatsapp;
import com.drivehub.identidad.domain.port.out.DesafioWhatsappRepositoryPort;
import com.drivehub.identidad.infrastructure.adapter.out.persistence.model.DesafioWhatsappModel;
import com.drivehub.identidad.infrastructure.adapter.out.persistence.model.UsuarioModel;

import lombok.RequiredArgsConstructor;

/** Códigos enviados al correo para vincular un WhatsApp (`desafio_whatsapp`). Solo se guarda el hash. */
@Component
@RequiredArgsConstructor
public class DesafioWhatsappPersistenceAdapter implements DesafioWhatsappRepositoryPort {

    private final DesafioWhatsappJpaRepository desafioJpaRepository;
    private final UsuarioJpaRepository usuarioJpaRepository;

    @Override
    @Transactional
    public DesafioWhatsapp guardar(DesafioWhatsapp desafio) {
        UsuarioModel usuario = null;
        if (desafio.getUsuarioCodigo() != null) {
            usuario = usuarioJpaRepository.findByCodigo(desafio.getUsuarioCodigo())
                .orElseThrow(() -> new IllegalStateException("No existe la cuenta del código"));
        }
        DesafioWhatsappModel guardado = desafioJpaRepository.save(new DesafioWhatsappModel(usuario,
            desafio.getTelefonoHash(), desafio.getCorreoHash(), desafio.getCodigoHash(), desafio.getExpiraEn(),
            desafio.isUsado()));
        return aDominio(guardado, desafio.getUsuarioCodigo());
    }

    @Override
    @Transactional
    public void invalidarPendientes(UUID usuarioCodigo) {
        desafioJpaRepository.invalidarPendientes(usuarioCodigo);
    }

    @Override
    public Optional<DesafioWhatsapp> buscarVigente(UUID usuarioCodigo, String telefonoHash, Instant ahora) {
        return desafioJpaRepository.vigentes(usuarioCodigo, telefonoHash, ahora, PageRequest.of(0, 1)).stream()
            .findFirst()
            .map(m -> aDominio(m, usuarioCodigo));
    }

    @Override
    @Transactional
    public void registrarIntentoFallido(UUID desafioCodigo) {
        desafioJpaRepository.findConUsuarioPorCodigo(desafioCodigo).ifPresent(model -> {
            model.setIntentos(model.getIntentos() + 1);
            desafioJpaRepository.save(model);
        });
    }

    @Override
    @Transactional
    public void marcarUsado(UUID desafioCodigo) {
        desafioJpaRepository.findConUsuarioPorCodigo(desafioCodigo).ifPresent(model -> {
            model.setUsado(true);
            desafioJpaRepository.save(model);
        });
    }

    @Override
    public long contarPorTelefonoDesde(String telefonoHash, Instant desde) {
        return desafioJpaRepository.countByTelefonoHashAndCreatedAtGreaterThanEqual(telefonoHash, desde);
    }

    @Override
    public long contarPorCorreoDesde(String correoHash, Instant desde) {
        return desafioJpaRepository.countByCorreoHashAndCreatedAtGreaterThanEqual(correoHash, desde);
    }

    private DesafioWhatsapp aDominio(DesafioWhatsappModel model, UUID usuarioCodigo) {
        return DesafioWhatsapp.builder()
            .codigo(model.getCodigo())
            .usuarioCodigo(usuarioCodigo)
            .telefonoHash(model.getTelefonoHash())
            .correoHash(model.getCorreoHash())
            .codigoHash(model.getCodigoHash())
            .intentos(model.getIntentos())
            .expiraEn(model.getExpiraEn())
            .usado(model.isUsado())
            .build();
    }
}
