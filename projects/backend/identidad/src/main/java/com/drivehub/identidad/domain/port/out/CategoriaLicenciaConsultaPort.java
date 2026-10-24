package com.drivehub.identidad.domain.port.out;

/**
 * Pregunta a Escuela, que es la dueña del catálogo, si una categoría de licencia existe (CU08).
 */
public interface CategoriaLicenciaConsultaPort {

    /**
     * Si la categoría con esa sigla existe en el catálogo. La sigla "sin categoría" existe siempre
     * y no se consulta a nadie.
     *
     * @throws EscuelaNoDisponibleException si Escuela no puede responder
     */
    boolean existe(String sigla);
}
