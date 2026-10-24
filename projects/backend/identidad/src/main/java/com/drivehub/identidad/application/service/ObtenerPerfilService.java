package com.drivehub.identidad.application.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.common.exception.UnauthorizedException;
import com.drivehub.identidad.domain.port.in.ObtenerPerfilUseCase;
import com.drivehub.identidad.domain.port.in.PerfilUsuario;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

/**
 * CU06: ver el perfil de la cuenta propia. Es una lectura: no se loguea (LOGGING_BACKEND.md).
 */
@Service
public class ObtenerPerfilService implements ObtenerPerfilUseCase {

    private final UsuarioRepositoryPort usuarioRepository;
    private final PerfilArmador perfilArmador;

    public ObtenerPerfilService(UsuarioRepositoryPort usuarioRepository, PerfilArmador perfilArmador) {
        this.usuarioRepository = usuarioRepository;
        this.perfilArmador = perfilArmador;
    }

    @Override
    @Transactional(readOnly = true)
    public PerfilUsuario obtener(UUID usuarioCodigo) {
        return perfilArmador.armar(usuarioRepository.buscarPorCodigo(usuarioCodigo)
            .orElseThrow(() -> new UnauthorizedException(IdentidadErrorCodes.SESION_INVALIDA, "Tu sesión terminó")));
    }
}
