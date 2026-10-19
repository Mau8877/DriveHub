package com.drivehub.identidad.domain.port.out;

import java.util.Optional;
import java.util.UUID;

import com.drivehub.identidad.domain.model.Dispositivo;
import com.drivehub.identidad.domain.model.RefreshToken;

public interface RefreshTokenRepositoryPort {

    RefreshToken guardar(RefreshToken token);

    Optional<RefreshToken> buscarPorHash(String tokenHash);

    /** Revoca las sesiones activas de ese usuario en ese dispositivo (no las del otro dispositivo). */
    void revocarActivos(UUID usuarioCodigo, Dispositivo dispositivo);

    void revocar(RefreshToken token);
}
