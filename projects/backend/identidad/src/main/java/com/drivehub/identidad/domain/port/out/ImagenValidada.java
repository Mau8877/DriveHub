package com.drivehub.identidad.domain.port.out;

import com.drivehub.identidad.domain.model.FormatoImagen;

/** Lo que se comprobó de una foto que pasó la revisión: su formato real y sus dimensiones. */
public record ImagenValidada(FormatoImagen formato, int ancho, int alto) {
}
