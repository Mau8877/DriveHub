package com.drivehub.academico.features.simulacros.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.drivehub.academico.common.model.BaseDocument;
import com.drivehub.academico.common.util.UuidV7;
import com.drivehub.academico.features.simulacros.enums.EstadoIntento;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Intento de un estudiante en un simulacro (CU21).
 * * Las preguntas se copian dentro del intento para que editar el banco no altere intentos pasados.
 */
@Getter
@NoArgsConstructor
@Document(collection = "intentos_simulacro")
@CompoundIndex(name = "idx_intento_estudiante_iniciado", def = "{'estudianteCodigo': 1, 'iniciadoEn': -1}")
public class IntentoSimulacro extends BaseDocument {

    // * Identificador público (UUID v7). El _id Long es interno, ver BaseDocument.
    @Indexed(unique = true)
    private UUID codigoIntentoSimulacro;

    // * _id de simulacros_config
    private Long simulacroId;

    private UUID estudianteCodigo;

    @Setter
    private EstadoIntento estado;

    private Instant iniciadoEn;

    // * Opcional
    @Setter
    private Instant finalizadoEn;

    @Setter
    private List<PreguntaIntento> preguntas;

    @Setter
    private int aciertos;

    @Setter
    private double puntaje;

    @Setter
    private boolean aprobado;

    @Override
    public void inicializarCodigo() {
        if (codigoIntentoSimulacro == null) {
            codigoIntentoSimulacro = UuidV7.generar();
        }
    }
}
