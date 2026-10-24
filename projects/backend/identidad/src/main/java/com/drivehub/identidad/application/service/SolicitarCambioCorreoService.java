package com.drivehub.identidad.application.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.identidad.common.exception.BusinessException;
import com.drivehub.identidad.common.exception.ConflictException;
import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.common.exception.UnauthorizedException;
import com.drivehub.identidad.common.exception.ValidationException;
import com.drivehub.identidad.domain.model.TipoTokenCorreo;
import com.drivehub.identidad.domain.model.TokenRecuperacion;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.in.SolicitarCambioCorreoComando;
import com.drivehub.identidad.domain.port.in.SolicitarCambioCorreoUseCase;
import com.drivehub.identidad.domain.port.out.EnviadorCorreoPort;
import com.drivehub.identidad.domain.port.out.GeneradorTokenOpacoPort;
import com.drivehub.identidad.domain.port.out.PasswordHasherPort;
import com.drivehub.identidad.domain.port.out.TokenRecuperacionRepositoryPort;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

/**
 * CU06 (F4, pedir): cambiar el correo. Genera un enlace de un solo uso que reemplaza a los
 * anteriores y lo envía al correo NUEVO; el correo de la cuenta no cambia hasta confirmarlo.
 *
 * * Si el envío falla, la excepción se propaga y la transacción se revierte: el enlace no queda
 * * guardado (mismo criterio que la verificación del correo, CU04b).
 * * `noRollbackFor`: un rechazo de negocio no escribió nada antes de lanzar.
 * ! Nunca se loguea el token, el enlace, la contraseña ni la dirección nueva.
 */
@Service
public class SolicitarCambioCorreoService implements SolicitarCambioCorreoUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(SolicitarCambioCorreoService.class);
    private static final String MODULO = "auth";

    private final UsuarioRepositoryPort usuarioRepository;
    private final TokenRecuperacionRepositoryPort tokenRepository;
    private final GeneradorTokenOpacoPort generadorToken;
    private final PasswordHasherPort passwordHasher;
    private final EnviadorCorreoPort enviadorCorreo;
    private final Clock reloj;
    private final String urlWeb;
    private final Duration vigencia;

    public SolicitarCambioCorreoService(
            UsuarioRepositoryPort usuarioRepository,
            TokenRecuperacionRepositoryPort tokenRepository,
            GeneradorTokenOpacoPort generadorToken,
            PasswordHasherPort passwordHasher,
            EnviadorCorreoPort enviadorCorreo,
            Clock reloj,
            @Value("${app.web.url}") String urlWeb,
            @Value("${app.auth.cambio-correo-ttl-horas:24}") long vigenciaHoras) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.generadorToken = generadorToken;
        this.passwordHasher = passwordHasher;
        this.enviadorCorreo = enviadorCorreo;
        this.reloj = reloj;
        this.urlWeb = urlWeb.endsWith("/") ? urlWeb.substring(0, urlWeb.length() - 1) : urlWeb;
        this.vigencia = Duration.ofHours(vigenciaHoras);
    }

    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public void solicitar(SolicitarCambioCorreoComando comando) {
        Usuario usuario = usuarioRepository.buscarPorCodigo(comando.usuarioCodigo())
            .orElseThrow(() -> new UnauthorizedException(IdentidadErrorCodes.SESION_INVALIDA, "Tu sesión terminó"));

        if (!passwordHasher.coincide(comando.passwordActual(), usuario.getPasswordHash())) {
            LOG.warn("Cambio de correo rechazado", kv("module", MODULO),
                kv("errorCode", IdentidadErrorCodes.PASSWORD_ACTUAL_INCORRECTA), kv("usuarioCodigo", usuario.getCodigo()));
            throw new ValidationException(IdentidadErrorCodes.PASSWORD_ACTUAL_INCORRECTA,
                "La contraseña actual no es correcta");
        }

        String correoNuevo = comando.correoNuevo().trim().toLowerCase(Locale.ROOT);
        if (correoNuevo.equals(usuario.getEmail())) {
            throw conflicto(usuario, IdentidadErrorCodes.CORREO_IGUAL_AL_ACTUAL, "Ese ya es el correo de tu cuenta");
        }
        if (usuarioRepository.existePorEmail(correoNuevo)) {
            throw conflicto(usuario, IdentidadErrorCodes.CORREO_EN_USO, "Ya existe una cuenta con ese correo");
        }

        String token = generadorToken.generar();
        tokenRepository.invalidarPendientes(usuario.getCodigo(), TipoTokenCorreo.CAMBIO_CORREO);
        tokenRepository.guardar(TokenRecuperacion.builder()
            .usuarioCodigo(usuario.getCodigo())
            .tipo(TipoTokenCorreo.CAMBIO_CORREO)
            .tokenHash(generadorToken.hashear(token))
            .expiraEn(Instant.now(reloj).plus(vigencia))
            .correoNuevo(correoNuevo)
            .build());

        enviadorCorreo.enviarCambioCorreo(correoNuevo, usuario.getNombres(), urlWeb + "/account/change-email?token=" + token);
        LOG.info("Cambio de correo solicitado", kv("module", MODULO), kv("usuarioCodigo", usuario.getCodigo()));
    }

    private ConflictException conflicto(Usuario usuario, String codigo, String mensaje) {
        LOG.warn("Cambio de correo rechazado", kv("module", MODULO), kv("errorCode", codigo),
            kv("usuarioCodigo", usuario.getCodigo()));
        return new ConflictException(codigo, mensaje);
    }
}
