package com.drivehub.identidad.domain.port.out;

import java.util.Optional;
import java.util.UUID;

import com.drivehub.identidad.domain.model.TipoTokenCorreo;
import com.drivehub.identidad.domain.model.TokenRecuperacion;

/**
 * Enlaces enviados por correo (verificación del correo y recuperación de contraseña).
 * Solo se guarda el hash del token.
 */
public interface TokenRecuperacionRepositoryPort {

    TokenRecuperacion guardar(TokenRecuperacion token);

    Optional<TokenRecuperacion> buscarPorHash(String tokenHash);

    /** Marca como usados los enlaces de ese usuario y tipo que seguían sirviendo (los reemplaza uno nuevo). */
    void invalidarPendientes(UUID usuarioCodigo, TipoTokenCorreo tipo);

    void marcarUsado(TokenRecuperacion token);
}
