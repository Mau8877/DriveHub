package com.drivehub.identidad.domain.port.in;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Lo único que el Administrador puede cambiar de un estudiante (CU08, F3): su categoría y su
 * ubicación. Los tres campos son opcionales; las coordenadas van juntas o ninguna.
 *
 * * No hay campo para ningún otro dato a propósito: los nombres, el CI, el correo... son del propio
 * * estudiante (CU06).
 */
public record EditarEstudianteComando(
        UUID administradorCodigo,
        UUID estudianteCodigo,
        String categoriaSigla,
        BigDecimal latitud,
        BigDecimal longitud
) {
}
