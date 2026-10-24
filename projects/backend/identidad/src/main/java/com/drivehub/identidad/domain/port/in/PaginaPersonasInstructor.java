package com.drivehub.identidad.domain.port.in;

import java.util.List;

/**
 * Una página de la búsqueda de instructores (CU09).
 *
 * * `pagina` es 1-indexada. Sin resultados: 0 elementos y 0 páginas.
 */
public record PaginaPersonasInstructor(
        List<PersonaInstructor> elementos, int pagina, int tamano, long totalElementos, int totalPaginas) {
}
