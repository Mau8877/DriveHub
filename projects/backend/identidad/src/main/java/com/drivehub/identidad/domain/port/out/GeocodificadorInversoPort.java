package com.drivehub.identidad.domain.port.out;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Convierte un punto del mapa en una dirección legible (CU08a).
 *
 * ! Nunca lanza: ante cualquier falla (servicio caído, tiempo agotado, punto sin dirección)
 * ! devuelve vacío, y quien lo usa guarda el punto con la dirección en blanco.
 */
public interface GeocodificadorInversoPort {

    Optional<String> direccionDe(BigDecimal latitud, BigDecimal longitud);
}
