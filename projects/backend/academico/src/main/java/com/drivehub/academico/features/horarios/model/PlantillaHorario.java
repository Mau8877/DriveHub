package com.drivehub.academico.features.horarios.model;

import java.time.LocalDate;
import java.util.UUID;

import com.drivehub.academico.common.model.BaseDocument;
import com.drivehub.academico.common.util.UuidV7;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Horario semanal base del instructor (CU12).
 */
@Getter
@NoArgsConstructor
@Document(collection = "plantillas_horario")
public class PlantillaHorario extends BaseDocument {

    // * Identificador público (UUID v7). El _id Long es interno, ver BaseDocument.
    @Indexed(unique = true)
    private UUID codigoPlantillaHorario;

    // * = codigo de persona de Identidad (instructor), sin FK
    @Indexed
    private UUID instructorCodigo;

    // * = Escuela vehiculo.codigo_vehiculo, sin FK
    private UUID vehiculoCodigo;

    // * 1 (lunes) a 7 (domingo)
    @Setter
    private int diaSemana;

    // * HH:mm
    @Setter
    private String horaInicio;

    // * HH:mm
    @Setter
    private String horaFin;

    // * Ej: 60
    @Setter
    private int duracionClaseMin;

    @Setter
    private LocalDate vigenteDesde;

    // * Opcional
    @Setter
    private LocalDate vigenteHasta;

    @Setter
    private boolean activa;

    @Override
    public void inicializarCodigo() {
        if (codigoPlantillaHorario == null) {
            codigoPlantillaHorario = UuidV7.generar();
        }
    }
}
