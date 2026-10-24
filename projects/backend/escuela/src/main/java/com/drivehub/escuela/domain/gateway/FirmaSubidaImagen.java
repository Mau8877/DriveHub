package com.drivehub.escuela.domain.gateway;

/**
 * Lo que el navegador necesita para subir una imagen directo a Cloudinary (CU07): la cuenta
 * pública, la carpeta, la hora y la firma que los respalda. El API secret nunca viaja.
 */
public record FirmaSubidaImagen(String cloudName, String apiKey, long timestamp, String carpeta, String firma) {
}
