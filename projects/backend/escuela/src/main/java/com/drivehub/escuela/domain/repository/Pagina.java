package com.drivehub.escuela.domain.repository;

import java.util.List;

/**
 * Una página de resultados y el total de elementos que cumplen el filtro.
 *
 * * Tipo propio del dominio: el dominio no depende de Spring Data.
 */
public record Pagina<T>(List<T> elementos, long total) {
}
