package com.drivehub.identidad.domain.port.out;

import java.util.Optional;
import java.util.UUID;

public interface EscuelaConsultaPort {

    /** Vacío si la escuela no existe o no está activa. */
    Optional<EscuelaRef> buscarActivaPorSlug(String slug);

    /** La escuela con ese codigo en cualquier estado; vacío solo si no existe. */
    Optional<EscuelaRef> buscarPorCodigo(UUID codigo);
}
