package com.drivehub.identidad.domain.port.out;

import java.util.List;

import com.drivehub.identidad.domain.model.Usuario;

/**
 * Una página de cuentas y el total de las que cumplen el filtro (no solo las de la página).
 */
public record PaginaUsuarios(List<Usuario> elementos, long total) {
}
