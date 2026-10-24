package com.drivehub.identidad.domain.model;

/**
 * Una foto no pasó la revisión básica (CU06). Excepción de dominio, sin Spring: el caso de uso la
 * traduce al código de error que ve el cliente.
 */
public class ImagenInvalidaException extends RuntimeException {

    /** Por qué se rechazó. */
    public enum Motivo {
        /** El contenido no corresponde a JPG, PNG ni WebP. */
        FORMATO,
        /** Es del formato pero no se puede leer completa como imagen. */
        ILEGIBLE,
        /** Tiene bytes después del final de la imagen (un archivo pegado a una imagen). */
        DATOS_EXTRA,
        /** Supera el lado máximo en píxeles. */
        DIMENSIONES
    }

    private final Motivo motivo;

    public ImagenInvalidaException(Motivo motivo) {
        super("Imagen rechazada: " + motivo);
        this.motivo = motivo;
    }

    public Motivo getMotivo() {
        return motivo;
    }
}
