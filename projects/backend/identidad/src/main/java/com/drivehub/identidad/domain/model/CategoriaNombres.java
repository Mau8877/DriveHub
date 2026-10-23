package com.drivehub.identidad.domain.model;

/**
 * Categorías de licencia que Identidad necesita nombrar (CU08a). El catálogo completo vive en
 * Escuela; acá solo está la categoría con la que arranca todo estudiante.
 */
public final class CategoriaNombres {

    /** "Sin categoría": la sigla con la que se registra un estudiante; solo la escuela la cambia. */
    public static final String SIN_CATEGORIA = "SIN";

    private CategoriaNombres() {
    }
}
