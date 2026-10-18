package com.drivehub.academico.features.simulacros.model;

import java.util.List;
import java.util.UUID;

import com.drivehub.academico.common.model.BaseDocument;
import com.drivehub.academico.common.util.UuidV7;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Configuración de un simulacro escrito (CU20).
 */
@Getter
@NoArgsConstructor
@Document(collection = "simulacros_config")
public class SimulacroConfig extends BaseDocument {

    // * Identificador público (UUID v7). El _id Long es interno, ver BaseDocument.
    @Indexed(unique = true)
    private UUID codigoSimulacro;

    @Setter
    private String nombre;

    @Setter
    private String categoria;

    @Setter
    private int duracionMin;

    @Setter
    private int nroPreguntas;

    @Setter
    private double puntajeAprobacion;

    // * Cuántas preguntas de cada tema para armar cada intento
    @Setter
    private List<ReglaSimulacro> reglas;

    @Setter
    private boolean activo;

    @Override
    public void inicializarCodigo() {
        if (codigoSimulacro == null) {
            codigoSimulacro = UuidV7.generar();
        }
    }
}
