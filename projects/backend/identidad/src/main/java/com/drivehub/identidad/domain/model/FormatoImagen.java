package com.drivehub.identidad.domain.model;

/**
 * Formatos de imagen que se aceptan como foto de perfil (CU06). El formato sale del contenido del
 * archivo, nunca de su nombre ni del tipo que declara el cliente.
 */
public enum FormatoImagen {
    JPEG("jpg", "image/jpeg"),
    PNG("png", "image/png"),
    WEBP("webp", "image/webp");

    private final String extension;
    private final String contentType;

    FormatoImagen(String extension, String contentType) {
        this.extension = extension;
        this.contentType = contentType;
    }

    public String extension() {
        return extension;
    }

    public String contentType() {
        return contentType;
    }
}
