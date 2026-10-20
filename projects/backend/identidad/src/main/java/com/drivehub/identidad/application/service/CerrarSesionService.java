package com.drivehub.identidad.application.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Clock;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.identidad.domain.port.in.CerrarSesionComando;
import com.drivehub.identidad.domain.port.in.CerrarSesionUseCase;
import com.drivehub.identidad.domain.port.out.GeneradorTokenOpacoPort;
import com.drivehub.identidad.domain.port.out.RefreshTokenRepositoryPort;

/**
 * CU03: cerrar sesión. Revoca la sesión renovable presentada.
 *
 * * Idempotente: un token ausente, inexistente, vencido, ya revocado o de otro
 * * dispositivo no hace nada y no lanza error, para no confirmar si un token es válido.
 * ! Nunca se loguea el token.
 */
@Service
public class CerrarSesionService implements CerrarSesionUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(CerrarSesionService.class);
    private static final String MODULO = "auth";

    private final RefreshTokenRepositoryPort refreshTokenRepository;
    private final GeneradorTokenOpacoPort generadorTokenOpaco;
    private final Clock reloj;

    public CerrarSesionService(
            RefreshTokenRepositoryPort refreshTokenRepository,
            GeneradorTokenOpacoPort generadorTokenOpaco,
            Clock reloj) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.generadorTokenOpaco = generadorTokenOpaco;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public void cerrar(CerrarSesionComando comando) {
        if (comando.refreshToken() == null || comando.refreshToken().isBlank()) {
            return;
        }
        Instant ahora = Instant.now(reloj);
        refreshTokenRepository.buscarPorHash(generadorTokenOpaco.hashear(comando.refreshToken()))
            .filter(token -> !token.isRevocado()
                && ahora.isBefore(token.getExpiraEn())
                && token.getDispositivo() == comando.dispositivo())
            .ifPresent(token -> {
                refreshTokenRepository.revocar(token);
                LOG.info("Sesion cerrada", kv("module", MODULO), kv("usuarioCodigo", token.getUsuarioCodigo()),
                    kv("dispositivo", token.getDispositivo()));
            });
    }
}
