package com.drivehub.identidad.domain.port.in;

/** Token de acceso del estudiante para el chat; no lleva sesión renovable. */
public record SesionWhatsapp(String accessToken, long expiraEnSegundos) {
}
