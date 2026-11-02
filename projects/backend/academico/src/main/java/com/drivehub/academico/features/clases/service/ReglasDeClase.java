package com.drivehub.academico.features.clases.service;

import com.drivehub.academico.common.exception.AcademicoErrorCodes;
import com.drivehub.academico.common.exception.ValidationException;
import com.drivehub.academico.features.clases.dto.ActualizarClaseRequest;
import com.drivehub.academico.features.clases.enums.Asistencia;
import com.drivehub.academico.features.clases.model.Clase;

/**
 * Las reglas de negocio de los datos de una clase (CU17), las mismas al cerrarla y al corregirla. Los tamaños y rangos
 * los comprueba Bean Validation en los requests (`ERR_SYS_01`); acá van las que dependen de varios campos o de la
 * asistencia (`ERR_ACA_38`).
 */
public final class ReglasDeClase {

    static final String SOLO_OBSERVACIONES = "Una clase a la que el estudiante no asistió solo admite observaciones";
    static final String KILOMETRAJE_FINAL_MENOR = "El kilometraje final no puede ser menor que el inicial";

    private ReglasDeClase() {
    }

    /** Valida los datos de un cierre. */
    public static void validarCierre(Asistencia asistencia, String tema, Integer kmInicio, Integer kmFin) {
        if (asistencia == Asistencia.AUSENTE && (hayTexto(tema) || kmInicio != null || kmFin != null)) {
            throw datosInvalidos(SOLO_OBSERVACIONES);
        }
        validarKilometrajes(kmInicio, kmFin);
    }

    /**
     * Valida una corrección sobre {@code clase}: con una clase `AUSENTE` solo se admiten observaciones y el
     * kilometraje que queda (lo enviado, o lo que la clase ya tenía) no puede ir al revés.
     */
    public static void validarCorreccion(Clase clase, ActualizarClaseRequest request) {
        if (clase.getAsistencia() == Asistencia.AUSENTE
                && (hayTexto(request.tema()) || request.kmInicio() != null || request.kmFin() != null)) {
            throw datosInvalidos(SOLO_OBSERVACIONES);
        }
        Integer inicio = request.kmInicio() != null ? request.kmInicio() : clase.getKmInicio();
        Integer fin = request.kmFin() != null ? request.kmFin() : clase.getKmFin();
        validarKilometrajes(inicio, fin);
    }

    /** Recorta los espacios y devuelve {@code null} si queda en blanco: así un texto en blanco borra el valor. */
    public static String limpiar(String texto) {
        if (texto == null) {
            return null;
        }
        String recortado = texto.strip();
        return recortado.isEmpty() ? null : recortado;
    }

    private static void validarKilometrajes(Integer inicio, Integer fin) {
        if (inicio != null && fin != null && fin < inicio) {
            throw datosInvalidos(KILOMETRAJE_FINAL_MENOR);
        }
    }

    private static boolean hayTexto(String texto) {
        return texto != null && !texto.isBlank();
    }

    private static ValidationException datosInvalidos(String mensaje) {
        return new ValidationException(AcademicoErrorCodes.DATOS_DE_CLASE_INVALIDOS, mensaje);
    }
}
