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
import com.drivehub.identidad.domain.port.in.ConfirmarCambioCorreoUseCase;
import com.drivehub.identidad.domain.port.out.EnviadorCorreoPort;
import com.drivehub.identidad.domain.port.out.GeneradorTokenOpacoPort;
import com.drivehub.identidad.domain.port.out.TokenRecuperacionRepositoryPort;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

/**
 * CU06 (F4, confirmar): abrir el enlace del correo nuevo. Cambia el correo de la cuenta, lo deja
 * verificado (abrir el enlace demuestra que es de la persona), consume el enlace y avisa al correo
 * anterior. Las sesiones no se tocan.
 *
 * * Un enlace que no existe, ya se usó, venció, fue reemplazado, es de otro tipo, cuya cuenta
 * * está inactiva o cuyo correo ya es de otra cuenta responde siempre ERR_IDE_11.
 * * Si el aviso al correo anterior falla, el cambio ya hecho se conserva y solo se registra.
 * ! Nunca se loguea el token ni las direcciones.
 */
@Service
public class ConfirmarCambioCorreoService implements ConfirmarCambioCorreoUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(ConfirmarCambioCorreoService.class);
    private static final String MODULO = "auth";

    private final UsuarioRepositoryPort usuarioRepository;
    private final TokenRecuperacionRepositoryPort tokenRepository;
    private final GeneradorTokenOpacoPort generadorToken;
    private final EnviadorCorreoPort enviadorCorreo;
    private final Clock reloj;

    public ConfirmarCambioCorreoService(
            UsuarioRepositoryPort usuarioRepository,
            TokenRecuperacionRepositoryPort tokenRepository,
            GeneradorTokenOpacoPort generadorToken,
            EnviadorCorreoPort enviadorCorreo,
            Clock reloj) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.generadorToken = generadorToken;
        this.enviadorCorreo = enviadorCorreo;
        this.reloj = reloj;
    }

    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public void confirmar(String token) {
        TokenRecuperacion enlace = tokenRepository.buscarPorHash(generadorToken.hashear(token))
            .filter(t -> t.getTipo() == TipoTokenCorreo.CAMBIO_CORREO
                && t.getCorreoNuevo() != null
                && !t.isUsado()
                && Instant.now(reloj).isBefore(t.getExpiraEn()))
            .orElseThrow(this::enlaceInvalido);
        Usuario usuario = usuarioRepository.buscarPorCodigo(enlace.getUsuarioCodigo())
            .filter(u -> u.getEstado() != EstadoUsuario.INACTIVO)
            .orElseThrow(this::enlaceInvalido);
        // * Otra persona pudo registrar ese correo mientras el enlace esperaba
        if (usuarioRepository.existePorEmail(enlace.getCorreoNuevo())) {
            throw enlaceInvalido();
        }

        String correoAnterior = usuario.getEmail();
        tokenRepository.marcarUsado(enlace);
        tokenRepository.invalidarPendientes(usuario.getCodigo(), TipoTokenCorreo.CAMBIO_CORREO);
        usuarioRepository.actualizar(usuario.toBuilder().email(enlace.getCorreoNuevo()).correoVerificado(true).build());
        LOG.info("Correo cambiado", kv("module", MODULO), kv("usuarioCodigo", usuario.getCodigo()));

        try {
            enviadorCorreo.enviarAvisoCambioCorreo(correoAnterior, usuario.getNombres(), enlace.getCorreoNuevo());
        } catch (RuntimeException e) {
            LOG.warn("No se pudo avisar al correo anterior del cambio", kv("module", MODULO),
                kv("usuarioCodigo", usuario.getCodigo()));
        }
    }

    private ValidationException enlaceInvalido() {
        LOG.warn("Enlace de cambio de correo rechazado", kv("module", MODULO),
            kv("errorCode", IdentidadErrorCodes.ENLACE_INVALIDO));
        return new ValidationException(IdentidadErrorCodes.ENLACE_INVALIDO, "El enlace no es válido o ya venció");
    }
}
