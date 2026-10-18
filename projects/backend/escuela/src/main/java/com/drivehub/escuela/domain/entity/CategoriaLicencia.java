package com.drivehub.escuela.domain.entity;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

/**
 * Categoría de licencia que enseña la escuela.
 *
 * * `sigla` (A, B, C...) es el valor que usan los demás servicios para
 * * referirse a la categoría. En el diseño original se llamaba "codigo";
 * * se renombró para no chocar con el `codigo` UUID público.
 */
@Getter
@Builder
public class CategoriaLicencia {

    private final UUID codigo;
    private final String sigla;
    private final String descripcion;
    private final Short edadMinima;
    private final Short clasesMinimas;
}
