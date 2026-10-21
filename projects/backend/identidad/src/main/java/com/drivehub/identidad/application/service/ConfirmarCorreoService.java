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
import com.drivehub.identidad.domain.model.TipoTokenCorreo;
import com.drivehub.identidad.domain.model.TokenRecuperacion;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.in.ConfirmarCorreoUseCase;
import com.drivehub.identidad.domain.port.out.GeneradorTokenOpacoPort;
import com.drivehub.identidad.domain.port.out.TokenRecuperacionRepositoryPort;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

/**
 * CU04b: confirmar el enlace del correo. Un enlace que no existe, ya se usó,
 * venció o es de otro tipo responde siempre el mismo error.
 *
 * ! Nunca se loguea el token.
 */
@Service
public class ConfirmarCorreoService implements ConfirmarCorreoUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(ConfirmarCorreoService.class);
    private static final String MODULO = "auth";

    private final UsuarioRepositoryPort usuarioRepository;
    private final TokenRecuperacionRepositoryPort tokenRepository;
    private final GeneradorTokenOpacoPort generadorToken;
    private final Clock reloj;

    public ConfirmarCorreoService(
            UsuarioRepositoryPort usuarioRepository,
            TokenRecuperacionRepositoryPort tokenRepository,
            GeneradorTokenOpacoPort generadorToken,
            Clock reloj) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.generadorToken = generadorToken;
        this.reloj = reloj;
    }

    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public void confirmar(String token) {
        TokenRecuperacion enlace = tokenRepository.buscarPorHash(generadorToken.hashear(token))
            .filter(t -> t.getTipo() == TipoTokenCorreo.VERIFICACION
                && !t.isUsado()
                && Instant.now(reloj).isBefore(t.getExpiraEn()))
            .orElseThrow(this::enlaceInvalido);
        Usuario usuario = usuarioRepository.buscarPorCodigo(enlace.getUsuarioCodigo())
            .orElseThrow(this::enlaceInvalido);

        tokenRepository.marcarUsado(enlace);
        usuarioRepository.actualizar(usuario.toBuilder().correoVerificado(true).build());
        LOG.info("Correo verificado", kv("module", MODULO), kv("usuarioCodigo", usuario.getCodigo()));
    }

    private ValidationException enlaceInvalido() {
        LOG.warn("Enlace de verificacion rechazado", kv("module", MODULO), kv("errorCode", IdentidadErrorCodes.ENLACE_INVALIDO));
        return new ValidationException(IdentidadErrorCodes.ENLACE_INVALIDO, "El enlace no es válido o ya venció");
    }
}
