package com.drivehub.identidad.application.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.identidad.common.exception.BusinessException;
import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.common.exception.UnauthorizedException;
import com.drivehub.identidad.common.exception.ValidationException;
import com.drivehub.identidad.domain.model.Dispositivo;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.in.CambiarContrasenaComando;
import com.drivehub.identidad.domain.port.in.CambiarContrasenaUseCase;
import com.drivehub.identidad.domain.port.out.PasswordHasherPort;
import com.drivehub.identidad.domain.port.out.RefreshTokenRepositoryPort;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

/**
 * CU06 (F3): cambiar la contraseña con sesión.
 *
 * * Una contraseña actual incorrecta es 400 (ERR_IDE_14) y no 401: un 401 haría que los clientes
 * * intenten renovar la sesión. No suma intentos fallidos ni bloquea la cuenta.
 * * Se cierra la sesión del OTRO dispositivo y se conserva la del que pidió el cambio.
 * * `noRollbackFor`, como en el resto de los casos de uso de contraseñas: los rechazos no
 * * deshacen nada porque no escribieron nada antes de lanzar.
 * ! Nunca se loguea ninguna de las dos contraseñas.
 */
@Service
public class CambiarContrasenaService implements CambiarContrasenaUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(CambiarContrasenaService.class);
    private static final String MODULO = "auth";

    private final UsuarioRepositoryPort usuarioRepository;
    private final RefreshTokenRepositoryPort refreshTokenRepository;
    private final PasswordHasherPort passwordHasher;

    public CambiarContrasenaService(
            UsuarioRepositoryPort usuarioRepository,
            RefreshTokenRepositoryPort refreshTokenRepository,
            PasswordHasherPort passwordHasher) {
        this.usuarioRepository = usuarioRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordHasher = passwordHasher;
    }

    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public void cambiar(CambiarContrasenaComando comando) {
        Usuario usuario = usuarioRepository.buscarPorCodigo(comando.usuarioCodigo())
            .orElseThrow(() -> new UnauthorizedException(IdentidadErrorCodes.SESION_INVALIDA, "Tu sesión terminó"));

        if (!passwordHasher.coincide(comando.passwordActual(), usuario.getPasswordHash())) {
            throw rechazo(usuario, IdentidadErrorCodes.PASSWORD_ACTUAL_INCORRECTA, "La contraseña actual no es correcta");
        }
        if (passwordHasher.coincide(comando.passwordNueva(), usuario.getPasswordHash())) {
            throw rechazo(usuario, IdentidadErrorCodes.PASSWORD_NUEVA_IGUAL_A_ACTUAL,
                "La contraseña nueva debe ser distinta de la actual");
        }

        usuarioRepository.actualizar(usuario.toBuilder().passwordHash(passwordHasher.hashear(comando.passwordNueva())).build());
        Dispositivo otro = comando.dispositivo() == Dispositivo.WEB ? Dispositivo.MOVIL : Dispositivo.WEB;
        refreshTokenRepository.revocarActivos(usuario.getCodigo(), otro);
        LOG.info("Contrasena cambiada", kv("module", MODULO), kv("usuarioCodigo", usuario.getCodigo()));
    }

    private ValidationException rechazo(Usuario usuario, String codigo, String mensaje) {
        LOG.warn("Cambio de contrasena rechazado", kv("module", MODULO), kv("errorCode", codigo),
            kv("usuarioCodigo", usuario.getCodigo()));
        return new ValidationException(codigo, mensaje);
    }
}
