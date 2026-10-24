package com.drivehub.escuela.domain.entity;

import java.util.Locale;
import java.util.regex.Pattern;

import com.drivehub.escuela.common.exception.EscuelaErrorCodes;
import com.drivehub.escuela.common.exception.ValidationException;

/**
 * Número de licencia de conducir de un instructor (CU09): sin espacios, en mayúsculas, de 3 a 30
 * caracteres entre letras, números y guion. Es único en toda la plataforma.
 */
public final class NroLicencia {

    public static final int MINIMO = 3;
    public static final int MAXIMO = 30;
    private static final Pattern FORMATO = Pattern.compile("[A-Z0-9-]+");

    private NroLicencia() {
    }

    /** Recorta, quita los espacios internos y pasa a mayúsculas. Nulo es texto vacío. */
    public static String normalizar(String texto) {
        return texto == null ? "" : texto.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    public static boolean esValido(String normalizado) {
        return normalizado.length() >= MINIMO && normalizado.length() <= MAXIMO
            && FORMATO.matcher(normalizado).matches();
    }

    /**
     * Normaliza y valida el número.
     *
     * @throws ValidationException `ERR_SYS_01` si no cumple el formato
     */
    public static String normalizarYValidar(String texto) {
        String normalizado = normalizar(texto);
        if (!esValido(normalizado)) {
            throw new ValidationException(EscuelaErrorCodes.VALIDACION, "El número de licencia debe tener entre "
                + MINIMO + " y " + MAXIMO + " caracteres: letras, números y guion");
        }
        return normalizado;
    }
}
