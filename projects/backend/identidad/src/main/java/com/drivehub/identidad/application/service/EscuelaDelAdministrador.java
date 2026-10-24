package com.drivehub.identidad.application.service;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.drivehub.identidad.common.exception.ForbiddenException;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

import lombok.RequiredArgsConstructor;

/**
 * La escuela de quien hace la petición (CU08): se toma de su cuenta, nunca de un parámetro del
 * cliente, así un Administrador no puede tocar estudiantes de otra escuela.
 */
@Component
@RequiredArgsConstructor
public class EscuelaDelAdministrador {

    // * Mismo código que usa el sistema para un 403 por falta de permiso (ver GlobalExceptionHandler)
    private static final String SIN_PERMISO = "ERR_SYS_05";

    private final UsuarioRepositoryPort usuarioRepository;

    /**
     * @throws ForbiddenException si la cuenta no existe o no pertenece a ninguna escuela
     */
    public UUID de(UUID administradorCodigo) {
        return usuarioRepository.buscarPorCodigo(administradorCodigo)
            .map(cuenta -> cuenta.getEscuelaCodigo())
            .orElseThrow(() -> new ForbiddenException(SIN_PERMISO, "No tienes permiso para realizar esta acción"));
    }
}
