package com.drivehub.identidad.domain.model;

import java.util.Locale;

/**
 * Documento de identidad (CU08a): se guarda sin espacios y en mayúsculas, así "ab 123" y "AB123"
 * son el mismo CI para la regla de unicidad.
 */
public final class Ci {

    private Ci() {
    }

    public static String normalizar(String ci) {
        return ci == null ? null : ci.replaceAll("\s", "").toUpperCase(Locale.ROOT);
    }
}
