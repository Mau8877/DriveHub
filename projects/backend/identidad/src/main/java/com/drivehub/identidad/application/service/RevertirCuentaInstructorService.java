package com.drivehub.identidad.application.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.identidad.common.exception.ConflictException;
import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.in.RevertirCuentaInstructorUseCase;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

import lombok.RequiredArgsConstructor;

/**
 * CU09: deshace el alta de la cuenta de un instructor cuya ficha no se pudo guardar.
 *
 * * Es idempotente: si la cuenta no existe, no es un instructor o es de otra escuela, no hace nada.
 * * El borrado es FÍSICO a propósito, igual que en CU05: el correo es UNIQUE incluso entre cuentas
 * * con borrado lógico, y un deleted_at impediría repetir el alta con el mismo correo.
 * ! Solo aplica a una cuenta que nunca inició sesión; una cuenta usada no se borra nunca.
 */
@Service
@RequiredArgsConstructor
public class RevertirCuentaInstructorService implements RevertirCuentaInstructorUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(RevertirCuentaInstructorService.class);
    private static final String MODULO = "instructores";

    private final EscuelaDelAdministrador escuelaDelAdministrador;
    private final UsuarioRepositoryPort usuarioRepository;

    @Override
    @PreAuthorize("hasRole('ADMIN_ESCUELA')")
    @Transactional
    public void revertir(UUID administradorCodigo, UUID instructorCodigo) {
        UUID escuela = escuelaDelAdministrador.de(administradorCodigo);

        Optional<Usuario> instructor = usuarioRepository.buscarInstructorDeEscuela(escuela, instructorCodigo);
        if (instructor.isEmpty()) {
            LOG.info("Nada que revertir: la cuenta del instructor no existe",
                kv("module", MODULO), kv("usuarioCodigo", instructorCodigo));
            return;
        }
        if (instructor.get().getUltimoLogin() != null) {
            LOG.warn("Reversión rechazada: el instructor ya inició sesión",
                kv("module", MODULO), kv("errorCode", IdentidadErrorCodes.CUENTA_YA_USADA),
                kv("usuarioCodigo", instructorCodigo));
            throw new ConflictException(IdentidadErrorCodes.CUENTA_YA_USADA,
                "La cuenta del instructor ya se usó y no se puede revertir");
        }
        usuarioRepository.eliminarDefinitivamente(instructorCodigo);
        LOG.warn("Cuenta de instructor revertida", kv("module", MODULO), kv("usuarioCodigo", instructorCodigo));
    }
}
