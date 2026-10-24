package com.drivehub.identidad.application.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.common.exception.UnauthorizedException;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.in.PerfilUsuario;
import com.drivehub.identidad.domain.port.in.QuitarFotoPerfilUseCase;
import com.drivehub.identidad.domain.port.out.AlmacenFotosPort;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

/**
 * CU06 (F2, paso 4): quitar la foto. Idempotente: una cuenta sin foto responde bien y no cambia nada.
 *
 * * Sin @Transactional por lo mismo que al subirla: la cuenta se actualiza primero y recién
 * * después se borra el objeto; si el borrado falla queda un WARN y la foto ya no está en la cuenta.
 */
@Service
public class QuitarFotoPerfilService implements QuitarFotoPerfilUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(QuitarFotoPerfilService.class);
    private static final String MODULO = "perfil";

    private final UsuarioRepositoryPort usuarioRepository;
    private final AlmacenFotosPort almacenFotos;
    private final PerfilArmador perfilArmador;

    public QuitarFotoPerfilService(
            UsuarioRepositoryPort usuarioRepository, AlmacenFotosPort almacenFotos, PerfilArmador perfilArmador) {
        this.usuarioRepository = usuarioRepository;
        this.almacenFotos = almacenFotos;
        this.perfilArmador = perfilArmador;
    }

    @Override
    public PerfilUsuario quitar(UUID usuarioCodigo) {
        Usuario usuario = usuarioRepository.buscarPorCodigo(usuarioCodigo)
            .orElseThrow(() -> new UnauthorizedException(IdentidadErrorCodes.SESION_INVALIDA, "Tu sesión terminó"));
        if (usuario.getFotoUrl() == null) {
            return perfilArmador.armar(usuario);
        }

        Usuario sinFoto = usuarioRepository.actualizar(usuario.toBuilder().fotoUrl(null).build());
        try {
            almacenFotos.borrar(usuario.getFotoUrl());
        } catch (RuntimeException e) {
            LOG.warn("No se pudo borrar la foto del almacenamiento: queda un objeto huérfano",
                kv("module", MODULO), kv("usuarioCodigo", usuarioCodigo));
        }
        LOG.info("Foto de perfil quitada", kv("module", MODULO), kv("usuarioCodigo", usuarioCodigo));
        return perfilArmador.armar(sinFoto);
    }
}
