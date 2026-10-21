package com.drivehub.identidad.application.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.identidad.common.exception.ConflictException;
import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.common.exception.UnauthorizedException;
import com.drivehub.identidad.domain.model.TipoTokenCorreo;
import com.drivehub.identidad.domain.model.TokenRecuperacion;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.in.SolicitarVerificacionCorreoUseCase;
import com.drivehub.identidad.domain.port.out.EnviadorCorreoPort;
import com.drivehub.identidad.domain.port.out.GeneradorTokenOpacoPort;
import com.drivehub.identidad.domain.port.out.TokenRecuperacionRepositoryPort;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

/**
 * CU04b: pedir el correo de verificación. Genera un enlace de un solo uso que
 * reemplaza a los anteriores y lo envía siempre a la dirección de la cuenta.
 *
 * * Si el envío falla, la excepción se propaga y la transacción se revierte:
 * * el enlace no queda guardado (la persona está presente y puede reintentar).
 * ! Nunca se loguea el token ni el enlace.
 */
@Service
public class SolicitarVerificacionCorreoService implements SolicitarVerificacionCorreoUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(SolicitarVerificacionCorreoService.class);
    private static final String MODULO = "auth";

    private final UsuarioRepositoryPort usuarioRepository;
    private final TokenRecuperacionRepositoryPort tokenRepository;
    private final GeneradorTokenOpacoPort generadorToken;
    private final EnviadorCorreoPort enviadorCorreo;
    private final Clock reloj;
    private final String urlWeb;
    private final Duration vigencia;

    public SolicitarVerificacionCorreoService(
            UsuarioRepositoryPort usuarioRepository,
            TokenRecuperacionRepositoryPort tokenRepository,
            GeneradorTokenOpacoPort generadorToken,
            EnviadorCorreoPort enviadorCorreo,
            Clock reloj,
            @Value("${app.web.url}") String urlWeb,
            @Value("${app.auth.verificacion-ttl-horas:24}") long vigenciaHoras) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.generadorToken = generadorToken;
        this.enviadorCorreo = enviadorCorreo;
        this.reloj = reloj;
        this.urlWeb = urlWeb.endsWith("/") ? urlWeb.substring(0, urlWeb.length() - 1) : urlWeb;
        this.vigencia = Duration.ofHours(vigenciaHoras);
    }

    @Override
    @Transactional
    public void solicitar(UUID usuarioCodigo) {
        Usuario usuario = usuarioRepository.buscarPorCodigo(usuarioCodigo)
            .orElseThrow(() -> new UnauthorizedException(IdentidadErrorCodes.SESION_INVALIDA, "Tu sesión terminó"));
        if (usuario.isCorreoVerificado()) {
            throw new ConflictException(IdentidadErrorCodes.CORREO_YA_VERIFICADO, "Tu correo ya está verificado");
        }

        String token = generadorToken.generar();
        tokenRepository.invalidarPendientes(usuario.getCodigo(), TipoTokenCorreo.VERIFICACION);
        tokenRepository.guardar(TokenRecuperacion.builder()
            .usuarioCodigo(usuario.getCodigo())
            .tipo(TipoTokenCorreo.VERIFICACION)
            .tokenHash(generadorToken.hashear(token))
            .expiraEn(Instant.now(reloj).plus(vigencia))
            .build());

        enviadorCorreo.enviarVerificacionCorreo(
            usuario.getEmail(), usuario.getNombres(), urlWeb + "/account/verify-email?token=" + token);
        LOG.info("Correo de verificacion enviado", kv("module", MODULO), kv("usuarioCodigo", usuario.getCodigo()));
    }
}
