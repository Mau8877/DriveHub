package com.drivehub.identidad.application.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.identidad.common.exception.BusinessException;
import com.drivehub.identidad.common.exception.ForbiddenException;
import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.common.exception.LockedException;
import com.drivehub.identidad.common.exception.UnauthorizedException;
import com.drivehub.identidad.domain.model.EstadoUsuario;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.in.IniciarSesionComando;
import com.drivehub.identidad.domain.port.in.IniciarSesionUseCase;
import com.drivehub.identidad.domain.port.in.SesionEmitida;
import com.drivehub.identidad.domain.port.out.PasswordHasherPort;
import com.drivehub.identidad.domain.port.out.RefreshTokenRepositoryPort;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

import java.time.Clock;

/**
 * CU02: iniciar sesión.
 *
 * * `noRollbackFor`: el contador de intentos fallidos y el bloqueo se guardan
 * * aunque el login termine en una excepción de negocio; sin esto la
 * * transacción se desharía junto con la excepción y nunca se bloquearía nada.
 * * Orden de comprobaciones: el estado de la cuenta (inactiva, escuela, plataforma)
 * * solo se revela con la contraseña correcta.
 * ! Nunca se loguea la contraseña, el hash ni los tokens.
 */
@Service
public class IniciarSesionService implements IniciarSesionUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(IniciarSesionService.class);
    private static final String MODULO = "auth";

    private final UsuarioRepositoryPort usuarioRepository;
    private final RefreshTokenRepositoryPort refreshTokenRepository;
    private final PasswordHasherPort passwordHasher;
    private final SesionEmisora sesionEmisora;
    private final Clock reloj;
    private final int maxIntentos;
    private final Duration bloqueo;

    public IniciarSesionService(
            UsuarioRepositoryPort usuarioRepository,
            RefreshTokenRepositoryPort refreshTokenRepository,
            PasswordHasherPort passwordHasher,
            SesionEmisora sesionEmisora,
            Clock reloj,
            @Value("${app.auth.max-intentos:5}") int maxIntentos,
            @Value("${app.auth.bloqueo-minutos:15}") long bloqueoMinutos) {
        this.usuarioRepository = usuarioRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordHasher = passwordHasher;
        this.sesionEmisora = sesionEmisora;
        this.reloj = reloj;
        this.maxIntentos = maxIntentos;
        this.bloqueo = Duration.ofMinutes(bloqueoMinutos);
    }

    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public SesionEmitida iniciar(IniciarSesionComando comando) {
        String email = comando.email().trim().toLowerCase(Locale.ROOT);
        Instant ahora = Instant.now(reloj);

        Usuario usuario = usuarioRepository.buscarPorEmail(email)
            .orElseThrow(() -> rechazoPorCredenciales());

        if (usuario.getEstado() == EstadoUsuario.BLOQUEADO) {
            if (usuario.getBloqueadoHasta() != null && ahora.isBefore(usuario.getBloqueadoHasta())) {
                throw cuentaBloqueada(usuario, ahora);
            }
            usuario = usuarioRepository.actualizar(usuario.toBuilder()
                .estado(EstadoUsuario.ACTIVO).intentosFallidos(0).bloqueadoHasta(null).build());
        }

        if (!passwordHasher.coincide(comando.password(), usuario.getPasswordHash())) {
            throw registrarIntentoFallido(usuario, ahora);
        }

        if (usuario.getEstado() == EstadoUsuario.INACTIVO) {
            LOG.warn("Inicio de sesion rechazado: cuenta inactiva",
                kv("module", MODULO), kv("errorCode", IdentidadErrorCodes.CUENTA_INACTIVA));
            throw new ForbiddenException(IdentidadErrorCodes.CUENTA_INACTIVA,
                "Tu cuenta está inactiva. Contacta a tu administrador");
        }
        try {
            sesionEmisora.verificarEscuelaActiva(usuario);
            sesionEmisora.verificarPlataforma(usuario, comando.dispositivo());
        } catch (BusinessException e) {
            LOG.warn("Inicio de sesion rechazado", kv("module", MODULO), kv("errorCode", e.getErrorCode()));
            throw e;
        }

        Usuario conAcceso = usuarioRepository.actualizar(usuario.toBuilder()
            .intentosFallidos(0).bloqueadoHasta(null).ultimoLogin(ahora).build());
        refreshTokenRepository.revocarActivos(conAcceso.getCodigo(), comando.dispositivo());
        SesionEmitida sesion = sesionEmisora.emitir(conAcceso, comando.dispositivo(), ahora);

        LOG.info("Sesion iniciada", kv("module", MODULO), kv("usuarioCodigo", conAcceso.getCodigo()),
            kv("dispositivo", comando.dispositivo()));
        return sesion;
    }

    private UnauthorizedException rechazoPorCredenciales() {
        LOG.warn("Inicio de sesion rechazado: credenciales incorrectas",
            kv("module", MODULO), kv("errorCode", IdentidadErrorCodes.CREDENCIALES_INCORRECTAS));
        return new UnauthorizedException(IdentidadErrorCodes.CREDENCIALES_INCORRECTAS, "Correo o contraseña incorrectos");
    }

    private BusinessException registrarIntentoFallido(Usuario usuario, Instant ahora) {
        int intentos = usuario.getIntentosFallidos() + 1;
        if (intentos >= maxIntentos) {
            Usuario bloqueado = usuarioRepository.actualizar(usuario.toBuilder()
                .estado(EstadoUsuario.BLOQUEADO).intentosFallidos(intentos).bloqueadoHasta(ahora.plus(bloqueo)).build());
            return cuentaBloqueada(bloqueado, ahora);
        }
        usuarioRepository.actualizar(usuario.toBuilder().intentosFallidos(intentos).build());
        return rechazoPorCredenciales();
    }

    private LockedException cuentaBloqueada(Usuario usuario, Instant ahora) {
        long minutos = Math.max(1, Duration.between(ahora, usuario.getBloqueadoHasta()).toMinutes() + 1);
        LOG.warn("Inicio de sesion rechazado: cuenta bloqueada",
            kv("module", MODULO), kv("errorCode", IdentidadErrorCodes.CUENTA_BLOQUEADA),
            kv("usuarioCodigo", usuario.getCodigo()));
        return new LockedException(IdentidadErrorCodes.CUENTA_BLOQUEADA,
            "Tu cuenta está bloqueada por demasiados intentos. Intenta de nuevo en " + minutos + " minuto(s)");
    }
}
