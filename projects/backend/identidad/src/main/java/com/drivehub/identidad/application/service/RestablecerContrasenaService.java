package com.drivehub.identidad.application.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Clock;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.identidad.common.exception.BusinessException;
import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.common.exception.ValidationException;
import com.drivehub.identidad.domain.model.EstadoUsuario;
import com.drivehub.identidad.domain.model.TipoTokenCorreo;
import com.drivehub.identidad.domain.model.TokenRecuperacion;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.in.RestablecerContrasenaUseCase;
import com.drivehub.identidad.domain.port.out.GeneradorTokenOpacoPort;
import com.drivehub.identidad.domain.port.out.PasswordHasherPort;
import com.drivehub.identidad.domain.port.out.RefreshTokenRepositoryPort;
import com.drivehub.identidad.domain.port.out.TokenRecuperacionRepositoryPort;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

/**
 * CU04: restablecer la contraseña con el enlace del correo.
 *
 * * Un enlace que no existe, ya se usó, venció, es de verificación o cuya cuenta quedó
 * * inactiva responde siempre ERR_IDE_11 (el mismo de CU04b).
 * * Al restablecer: contraseña nueva (BCrypt), contador de intentos en 0, sin bloqueo
 * * (BLOQUEADO pasa a ACTIVO) y TODAS las sesiones de la cuenta revocadas. No inicia sesión.
 * ! Nunca se loguea la contraseña ni el token.
 */
@Service
public class RestablecerContrasenaService implements RestablecerContrasenaUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(RestablecerContrasenaService.class);
    private static final String MODULO = "auth";

    private final UsuarioRepositoryPort usuarioRepository;
    private final TokenRecuperacionRepositoryPort tokenRepository;
    private final RefreshTokenRepositoryPort refreshTokenRepository;
    private final GeneradorTokenOpacoPort generadorToken;
    private final PasswordHasherPort passwordHasher;
    private final Clock reloj;

    public RestablecerContrasenaService(
            UsuarioRepositoryPort usuarioRepository,
            TokenRecuperacionRepositoryPort tokenRepository,
            RefreshTokenRepositoryPort refreshTokenRepository,
            GeneradorTokenOpacoPort generadorToken,
            PasswordHasherPort passwordHasher,
            Clock reloj) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.generadorToken = generadorToken;
        this.passwordHasher = passwordHasher;
        this.reloj = reloj;
    }

    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public void restablecer(String token, String nuevaPassword) {
        TokenRecuperacion enlace = tokenRepository.buscarPorHash(generadorToken.hashear(token))
            .filter(t -> t.getTipo() == TipoTokenCorreo.RECUPERACION
                && !t.isUsado()
                && Instant.now(reloj).isBefore(t.getExpiraEn()))
            .orElseThrow(this::enlaceInvalido);
        Usuario usuario = usuarioRepository.buscarPorCodigo(enlace.getUsuarioCodigo())
            .filter(u -> u.getEstado() != EstadoUsuario.INACTIVO)
            .orElseThrow(this::enlaceInvalido);

        tokenRepository.marcarUsado(enlace);
        tokenRepository.invalidarPendientes(usuario.getCodigo(), TipoTokenCorreo.RECUPERACION);
        usuarioRepository.actualizar(usuario.toBuilder()
            .passwordHash(passwordHasher.hashear(nuevaPassword))
            .intentosFallidos(0)
            .bloqueadoHasta(null)
            .estado(usuario.getEstado() == EstadoUsuario.BLOQUEADO ? EstadoUsuario.ACTIVO : usuario.getEstado())
            .build());
        refreshTokenRepository.revocarTodos(usuario.getCodigo());
        LOG.info("Contrasena restablecida", kv("module", MODULO), kv("usuarioCodigo", usuario.getCodigo()));
    }

    private ValidationException enlaceInvalido() {
        LOG.warn("Enlace de recuperacion rechazado", kv("module", MODULO), kv("errorCode", IdentidadErrorCodes.ENLACE_INVALIDO));
        return new ValidationException(IdentidadErrorCodes.ENLACE_INVALIDO, "El enlace no es válido o ya venció");
    }
}
