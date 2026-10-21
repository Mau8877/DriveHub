package com.drivehub.identidad.application.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.identidad.common.exception.BusinessException;
import com.drivehub.identidad.common.exception.ForbiddenException;
import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.common.exception.LockedException;
import com.drivehub.identidad.common.exception.UnauthorizedException;
import com.drivehub.identidad.domain.model.EstadoUsuario;
import com.drivehub.identidad.domain.model.RefreshToken;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.in.RenovarSesionComando;
import com.drivehub.identidad.domain.port.in.RenovarSesionUseCase;
import com.drivehub.identidad.domain.port.in.SesionEmitida;
import com.drivehub.identidad.domain.port.out.GeneradorTokenOpacoPort;
import com.drivehub.identidad.domain.port.out.RefreshTokenRepositoryPort;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

/**
 * CU02: mantener la sesión. Cada renovación rota la sesión renovable: el token
 * presentado se revoca y se entrega uno nuevo; uno ya usado no sirve otra vez.
 *
 * * Se vuelve a comprobar que la cuenta, la escuela y la plataforma sigan
 * * permitiendo entrar; si no, se revoca el token presentado (la sesión termina).
 * * `noRollbackFor`: esa revocación debe quedar guardada aunque se lance la excepción.
 * ! Nunca se loguea el token.
 */
@Service
public class RenovarSesionService implements RenovarSesionUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(RenovarSesionService.class);
    private static final String MODULO = "auth";

    private final UsuarioRepositoryPort usuarioRepository;
    private final RefreshTokenRepositoryPort refreshTokenRepository;
    private final GeneradorTokenOpacoPort generadorTokenOpaco;
    private final SesionEmisora sesionEmisora;
    private final Clock reloj;

    public RenovarSesionService(
            UsuarioRepositoryPort usuarioRepository,
            RefreshTokenRepositoryPort refreshTokenRepository,
            GeneradorTokenOpacoPort generadorTokenOpaco,
            SesionEmisora sesionEmisora,
            Clock reloj) {
        this.usuarioRepository = usuarioRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.generadorTokenOpaco = generadorTokenOpaco;
        this.sesionEmisora = sesionEmisora;
        this.reloj = reloj;
    }

    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public SesionEmitida renovar(RenovarSesionComando comando) {
        Instant ahora = Instant.now(reloj);
        if (comando.refreshToken() == null || comando.refreshToken().isBlank()) {
            throw sesionInvalida();
        }

        RefreshToken token = refreshTokenRepository.buscarPorHash(generadorTokenOpaco.hashear(comando.refreshToken()))
            .orElseThrow(this::sesionInvalida);
        if (token.isRevocado() || !ahora.isBefore(token.getExpiraEn()) || token.getDispositivo() != comando.dispositivo()) {
            throw sesionInvalida();
        }
        Usuario usuario = usuarioRepository.buscarPorCodigo(token.getUsuarioCodigo()).orElseThrow(this::sesionInvalida);

        try {
            verificarCuenta(usuario, ahora);
            sesionEmisora.verificarEscuelaActiva(usuario);
            sesionEmisora.verificarPlataforma(usuario, comando.dispositivo());
        } catch (BusinessException e) {
            refreshTokenRepository.revocar(token);
            LOG.warn("Renovacion rechazada", kv("module", MODULO), kv("errorCode", e.getErrorCode()),
                kv("usuarioCodigo", usuario.getCodigo()));
            throw e;
        }

        refreshTokenRepository.revocar(token);
        SesionEmitida sesion = sesionEmisora.emitir(usuario, comando.dispositivo(), ahora);
        LOG.info("Sesion renovada", kv("module", MODULO), kv("usuarioCodigo", usuario.getCodigo()),
            kv("dispositivo", comando.dispositivo()));
        return sesion;
    }

    private void verificarCuenta(Usuario usuario, Instant ahora) {
        if (usuario.getEstado() == EstadoUsuario.INACTIVO) {
            throw new ForbiddenException(IdentidadErrorCodes.CUENTA_INACTIVA, "Tu cuenta está inactiva. Contacta a tu administrador");
        }
        if (usuario.getEstado() == EstadoUsuario.BLOQUEADO && usuario.getBloqueadoHasta() != null
                && ahora.isBefore(usuario.getBloqueadoHasta())) {
            long minutos = Math.max(1, Duration.between(ahora, usuario.getBloqueadoHasta()).toMinutes() + 1);
            throw new LockedException(IdentidadErrorCodes.CUENTA_BLOQUEADA,
                "Tu cuenta está bloqueada por demasiados intentos. Intenta de nuevo en " + minutos + " minuto(s)");
        }
    }

    private UnauthorizedException sesionInvalida() {
        LOG.warn("Renovacion rechazada: sesion invalida",
            kv("module", MODULO), kv("errorCode", IdentidadErrorCodes.SESION_INVALIDA));
        return new UnauthorizedException(IdentidadErrorCodes.SESION_INVALIDA, "Tu sesión terminó. Inicia sesión de nuevo");
    }
}
