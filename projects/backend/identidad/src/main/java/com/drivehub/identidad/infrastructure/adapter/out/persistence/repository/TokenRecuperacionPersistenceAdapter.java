package com.drivehub.identidad.infrastructure.adapter.out.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.identidad.domain.model.TipoTokenCorreo;
import com.drivehub.identidad.domain.model.TokenRecuperacion;
import com.drivehub.identidad.domain.port.out.TokenRecuperacionRepositoryPort;
import com.drivehub.identidad.infrastructure.adapter.out.persistence.model.TokenRecuperacionModel;
import com.drivehub.identidad.infrastructure.adapter.out.persistence.model.UsuarioModel;

import lombok.RequiredArgsConstructor;

/**
 * Enlaces de verificación y recuperación (`token_recuperacion`). Solo se guarda el hash.
 *
 * * Traduce el `usuarioCodigo` del dominio al `usuario_id` interno: el id Long no sale de acá.
 */
@Component
@RequiredArgsConstructor
public class TokenRecuperacionPersistenceAdapter implements TokenRecuperacionRepositoryPort {

    private final TokenRecuperacionJpaRepository tokenJpaRepository;
    private final UsuarioJpaRepository usuarioJpaRepository;

    @Override
    public TokenRecuperacion guardar(TokenRecuperacion token) {
        UsuarioModel usuario = usuarioJpaRepository.findByCodigo(token.getUsuarioCodigo())
            .orElseThrow(() -> new IllegalStateException("No existe el usuario del enlace"));
        TokenRecuperacionModel guardado = tokenJpaRepository.save(
            new TokenRecuperacionModel(usuario, token.getTipo(), token.getTokenHash(), token.getExpiraEn(),
                token.getCorreoNuevo()));
        return aDominio(guardado, usuario.getCodigo());
    }

    @Override
    public Optional<TokenRecuperacion> buscarPorHash(String tokenHash) {
        return tokenJpaRepository.findConUsuarioPorHash(tokenHash)
            .map(m -> aDominio(m, m.getUsuario().getCodigo()));
    }

    // * @Transactional: una actualización masiva necesita transacción; dentro de un caso de uso se une a la suya
    @Override
    @Transactional
    public void invalidarPendientes(UUID usuarioCodigo, TipoTokenCorreo tipo) {
        tokenJpaRepository.invalidarPendientes(usuarioCodigo, tipo);
    }

    @Override
    @Transactional
    public void marcarUsado(TokenRecuperacion token) {
        tokenJpaRepository.findConUsuarioPorCodigo(token.getCodigo()).ifPresent(model -> {
            model.setUsado(true);
            tokenJpaRepository.save(model);
        });
    }

    private TokenRecuperacion aDominio(TokenRecuperacionModel model, UUID usuarioCodigo) {
        return TokenRecuperacion.builder()
            .codigo(model.getCodigo())
            .usuarioCodigo(usuarioCodigo)
            .tipo(model.getTipo())
            .tokenHash(model.getTokenHash())
            .expiraEn(model.getExpiraEn())
            .usado(model.isUsado())
            .correoNuevo(model.getCorreoNuevo())
            .build();
    }
}
