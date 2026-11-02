package com.drivehub.academico.features.clases.dto;

/**
 * Los límites de los datos de una clase (CU17), en un solo lugar para los dos requests. Son constantes de compilación
 * porque las anotaciones de Bean Validation las exigen.
 */
public final class ReglasDeClaseLimites {

    public static final int MAX_TEMA = 120;
    public static final int MAX_OBSERVACIONES = 500;
    public static final int MAX_KILOMETRAJE = 9_999_999;

    private ReglasDeClaseLimites() {
    }
}
