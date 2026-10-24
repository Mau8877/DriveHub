package com.drivehub.identidad.domain.port.in;

import java.util.List;

/**
 * Una página del listado de estudiantes (CU08).
 *
 * * `pagina` es 1-indexada. Sin resultados: 0 elementos y 0 páginas.
 */
public record PaginaEstudiantes(
        List<EstudianteResumen> elementos, int pagina, int tamano, long totalElementos, int totalPaginas) {
}
