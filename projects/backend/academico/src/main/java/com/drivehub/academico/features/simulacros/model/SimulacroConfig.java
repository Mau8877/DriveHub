package com.drivehub.academico.features.simulacros.model;

import java.util.List;
import java.util.UUID;

import com.drivehub.academico.common.model.BaseDocument;
import com.drivehub.academico.common.util.UuidV7;
import org.springframework.data.mongodb.core.index.CompoundIndex;
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
// * Escuela primero: los simulacros se consultan siempre dentro de una escuela (CU20) y CU21 busca el activo por categoría
@CompoundIndex(name = "idx_simulacro_escuela_categoria_activo",
        def = "{'escuelaCodigo': 1, 'categoria': 1, 'activo': 1}")
public class SimulacroConfig extends BaseDocument {

    // * Identificador público (UUID v7). El _id Long es interno, ver BaseDocument.
    @Indexed(unique = true)
    private UUID codigoSimulacro;

    // * Escuela (tenant) dueña del simulacro: el claim `escuela` del JWT, sin relación física
    @Setter
    private UUID escuelaCodigo;

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

    // * Persona de Identidad que lo creó
    @Setter
    private UUID creadoPorCodigo;

    @Override
    public void inicializarCodigo() {
        if (codigoSimulacro == null) {
            codigoSimulacro = UuidV7.generar();
        }
    }
}
