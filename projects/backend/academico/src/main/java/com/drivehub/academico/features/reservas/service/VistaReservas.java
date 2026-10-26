package com.drivehub.academico.features.reservas.service;

import java.util.Locale;

import com.drivehub.academico.common.exception.AcademicoErrorCodes;
import com.drivehub.academico.common.exception.ValidationException;

/**
 * Las dos listas de clases del estudiante (CU14): las que están por venir y el historial.
 */
public enum VistaReservas {
    PROXIMAS,
    HISTORIAL;

    /**
     * @param texto `proximas` o `historial` (sin distinguir mayúsculas); nulo o vacío es `proximas`
     * @throws ValidationException `ERR_SYS_01` si es cualquier otro valor
     */
    public static VistaReservas de(String texto) {
        if (texto == null || texto.isBlank()) {
            return PROXIMAS;
        }
        return switch (texto.trim().toLowerCase(Locale.ROOT)) {
            case "proximas" -> PROXIMAS;
            case "historial" -> HISTORIAL;
            default -> throw new ValidationException(AcademicoErrorCodes.VALIDACION,
                "La vista debe ser «proximas» o «historial»");
        };
    }
}
