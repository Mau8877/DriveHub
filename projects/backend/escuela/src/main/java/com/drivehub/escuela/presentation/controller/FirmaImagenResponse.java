package com.drivehub.escuela.presentation.controller;

/**
 * Datos para subir una imagen directo a Cloudinary (CU07). El API secret no está acá.
 */
public record FirmaImagenResponse(String cloudName, String apiKey, long timestamp, String folder, String signature) {
}
