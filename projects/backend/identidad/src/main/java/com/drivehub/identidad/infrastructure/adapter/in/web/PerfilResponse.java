package com.drivehub.identidad.infrastructure.adapter.in.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.drivehub.identidad.domain.model.SexoPersona;
import com.drivehub.identidad.domain.port.in.PerfilUsuario;

/**
 * Perfil de la cuenta propia (CU06, CU08a). Sin ningún id interno ni el hash de la contraseña.
 *
 * * `escuelaCodigo` y `escuelaNombre` son null en el Super admin; `fotoUrl` es una URL prefirmada
 * * (vale 60 minutos) o null si la cuenta no tiene foto.
 * * `categoriaSigla` es null salvo en el estudiante; `direccion` la calcula el servidor.
 */
public record PerfilResponse(
        UUID codigo,
        String nombres,
        String apellidos,
        LocalDate fechaNacimiento,
        String telefono,
        String email,
        boolean correoVerificado,
        List<String> roles,
        UUID escuelaCodigo,
        String escuelaNombre,
        String fotoUrl,
        String ci,
        SexoPersona sexo,
        String direccion,
        BigDecimal latitud,
        BigDecimal longitud,
        String categoriaSigla
) {

    public static PerfilResponse de(PerfilUsuario perfil) {
        return new PerfilResponse(perfil.codigo(), perfil.nombres(), perfil.apellidos(), perfil.fechaNacimiento(),
            perfil.telefono(), perfil.email(), perfil.correoVerificado(), perfil.roles(), perfil.escuelaCodigo(),
            perfil.escuelaNombre(), perfil.fotoUrl(), perfil.ci(), perfil.sexo(), perfil.direccion(),
            perfil.latitud(), perfil.longitud(), perfil.categoriaSigla());
    }
}
