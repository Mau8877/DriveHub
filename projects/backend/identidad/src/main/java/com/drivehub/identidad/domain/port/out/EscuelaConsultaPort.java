package com.drivehub.identidad.domain.port.out;

import java.util.Optional;

public interface EscuelaConsultaPort {

    /** Vacío si la escuela no existe o no está activa. */
    Optional<EscuelaRef> buscarActivaPorSlug(String slug);
}
