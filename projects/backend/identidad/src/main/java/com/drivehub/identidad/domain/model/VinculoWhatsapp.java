package com.drivehub.identidad.domain.model;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

/**
 * El número de WhatsApp de un estudiante (CU47). Del número solo se conserva el hash: con él se
 * reconoce a la persona cuando escribe, pero no se puede leer el teléfono.
 */
@Getter
@Builder
public class VinculoWhatsapp {

    private final UUID codigo;
    private final UUID usuarioCodigo;
    private final String telefonoHash;
}
