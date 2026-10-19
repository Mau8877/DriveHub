package com.drivehub.identidad.infrastructure.adapter.out.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.identidad.domain.model.Dispositivo;
import com.drivehub.identidad.domain.model.RefreshToken;
import com.drivehub.identidad.domain.port.out.RefreshTokenRepositoryPort;
import com.drivehub.identidad.infrastructure.adapter.out.persistence.model.RefreshTokenModel;
import com.drivehub.identidad.infrastructure.adapter.out.persistence.model.UsuarioModel;

import lombok.RequiredArgsConstructor;

/**
 * Sesiones renovables (`refresh_token`). Solo se guarda el hash del token.
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenPersistenceAdapter implements RefreshTokenRepositoryPort {

    private final RefreshTokenJpaRepository refreshTokenJpaRepository;
    private final UsuarioJpaRepository usuarioJpaRepository;

    @Override
    public RefreshToken guardar(RefreshToken token) {
        UsuarioModel usuario = usuarioJpaRepository.findByCodigo(token.getUsuarioCodigo())
            .orElseThrow(() -> new IllegalStateException("No existe el usuario de la sesion"));
        RefreshTokenModel guardado = refreshTokenJpaRepository.save(
            new RefreshTokenModel(usuario, token.getTokenHash(), token.getDispositivo(), token.getExpiraEn()));
        return aDominio(guardado, usuario.getCodigo());
    }

    @Override
    public Optional<RefreshToken> buscarPorHash(String tokenHash) {
        return refreshTokenJpaRepository.findConUsuarioPorHash(tokenHash)
            .map(m -> aDominio(m, m.getUsuario().getCodigo()));
    }

    // * @Transactional: una actualización masiva necesita transacción; dentro de un caso de uso se une a la suya
    @Override
    @Transactional
    public void revocarActivos(UUID usuarioCodigo, Dispositivo dispositivo) {
        refreshTokenJpaRepository.revocarActivos(usuarioCodigo, dispositivo);
    }

    @Override
    @Transactional
    public void revocar(RefreshToken token) {
        refreshTokenJpaRepository.findConUsuarioPorCodigo(token.getCodigo()).ifPresent(model -> {
            model.setRevocado(true);
            refreshTokenJpaRepository.save(model);
        });
    }

    private RefreshToken aDominio(RefreshTokenModel model, UUID usuarioCodigo) {
        return RefreshToken.builder()
            .codigo(model.getCodigo())
            .usuarioCodigo(usuarioCodigo)
            .tokenHash(model.getTokenHash())
            .dispositivo(model.getDispositivo())
            .expiraEn(model.getExpiraEn())
            .revocado(model.isRevocado())
            .build();
    }
}
