package com.drivehub.escuela.application.dto;

import java.util.List;

/**
 * Una página de resultados con su posición y los totales.
 *
 * * `pagina` es 1-indexada. Sin resultados: 0 elementos y 0 páginas.
 */
public record PaginaOutput<T>(List<T> elementos, int pagina, int tamano, long totalElementos, int totalPaginas) {
}
