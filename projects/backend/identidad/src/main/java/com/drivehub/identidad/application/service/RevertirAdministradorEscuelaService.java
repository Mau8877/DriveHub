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
import com.drivehub.identidad.domain.port.in.RevertirAdministradorEscuelaUseCase;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

import lombok.RequiredArgsConstructor;

/**
 * CU05: deshace la creación del Administrador de una escuela cuya creación falló a medias.
 *
 * * Es idempotente: sin Administrador para esa escuela, no hace nada.
 * * El borrado es FÍSICO a propósito: el correo es UNIQUE incluso entre cuentas con
 * * borrado lógico, y un deleted_at impediría repetir la creación con el mismo correo.
 * ! Solo aplica a una cuenta que nunca inició sesión; una cuenta usada no se borra nunca.
 */
@Service
@RequiredArgsConstructor
public class RevertirAdministradorEscuelaService implements RevertirAdministradorEscuelaUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(RevertirAdministradorEscuelaService.class);
    private static final String MODULO = "usuarios";

    private final UsuarioRepositoryPort usuarioRepository;

    @Override
    @Transactional
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public void revertir(UUID escuelaCodigo) {
        Optional<Usuario> administrador = usuarioRepository.buscarAdministradorDeEscuela(escuelaCodigo);
        if (administrador.isEmpty()) {
            LOG.info("Nada que revertir: la escuela no tiene administrador",
                kv("module", MODULO), kv("escuelaCodigo", escuelaCodigo));
            return;
        }
        Usuario usuario = administrador.get();
        if (usuario.getUltimoLogin() != null) {
            LOG.warn("Reversión rechazada: el administrador ya inició sesión",
                kv("module", MODULO), kv("errorCode", IdentidadErrorCodes.ADMIN_EN_USO),
                kv("usuarioCodigo", usuario.getCodigo()), kv("escuelaCodigo", escuelaCodigo));
            throw new ConflictException(IdentidadErrorCodes.ADMIN_EN_USO,
                "El administrador ya inició sesión y no se puede revertir");
        }
        usuarioRepository.eliminarDefinitivamente(usuario.getCodigo());
        LOG.info("Administrador de escuela revertido", kv("module", MODULO),
            kv("usuarioCodigo", usuario.getCodigo()), kv("escuelaCodigo", escuelaCodigo));
    }
}
