package com.drivehub.escuela.domain.gateway;

/**
 * El almacén de imágenes (Cloudinary), visto desde el negocio (CU07).
 */
public interface ImagenesGateway {

    /**
     * Firma una subida a `carpeta`. Falla con IllegalStateException si el almacén no está
     * configurado en este entorno.
     */
    FirmaSubidaImagen firmarSubida(String carpeta);
}
