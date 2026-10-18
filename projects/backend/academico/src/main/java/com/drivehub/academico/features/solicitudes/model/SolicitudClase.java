package com.drivehub.academico.features.solicitudes.model;

import java.time.Instant;
import java.util.UUID;

import com.drivehub.academico.common.model.BaseDocument;
import com.drivehub.academico.common.util.UuidV7;
import com.drivehub.academico.features.solicitudes.enums.EstadoSolicitud;
import com.drivehub.academico.features.solicitudes.enums.TipoClase;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Solicitud de clase del estudiante (CU13).
 */
@Getter
@NoArgsConstructor
@Document(collection = "solicitudes_clase")
public class SolicitudClase extends BaseDocument {

    // * Identificador público (UUID v7). El _id Long es interno, ver BaseDocument.
    @Indexed(unique = true)
    private UUID codigoSolicitudClase;

    // * = codigo de persona de Identidad (estudiante), sin FK
    @Indexed
    private UUID estudianteCodigo;

    // * Sigla de categoría (Escuela categoria_licencia)
    private String categoria;

    private TipoClase tipo;

    private int cantidadClases;

    // * Opcional
    private PreferenciasSolicitud preferencias;

    @Setter
    private EstadoSolicitud estado;

    // * Opcional
    @Setter
    private String motivoRechazo;

    // * Opcional. Persona (admin/instructor) que resolvió
    @Setter
    private UUID resueltaPorCodigo;

    @Setter
    private Instant resueltaEn;

    @Override
    public void inicializarCodigo() {
        if (codigoSolicitudClase == null) {
            codigoSolicitudClase = UuidV7.generar();
        }
    }
}
