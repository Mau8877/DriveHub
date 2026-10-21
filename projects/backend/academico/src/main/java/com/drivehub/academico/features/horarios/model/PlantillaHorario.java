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

    // * = escuela.escuela.codigo_escuela, tomado del claim `escuela` del JWT al crear. Es el dato,
    // * no el filtro por tenant (ese mecanismo está pendiente, ver DISENO_BASE_DE_DATOS.md)
    private UUID escuelaCodigo;

    // * = codigo de persona de Identidad (instructor), sin FK
    @Indexed
    private UUID instructorCodigo;

    // * = Escuela vehiculo.codigo_vehiculo, sin FK. Vacío hasta que CU10 asigne autos a instructores
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

    /**
     * Horario nuevo, activo. El vehículo no se asigna acá (CU10).
     */
    public PlantillaHorario(UUID escuelaCodigo, UUID instructorCodigo, int diaSemana, String horaInicio,
            String horaFin, int duracionClaseMin, LocalDate vigenteDesde, LocalDate vigenteHasta) {
        this.escuelaCodigo = escuelaCodigo;
        this.instructorCodigo = instructorCodigo;
        this.diaSemana = diaSemana;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.duracionClaseMin = duracionClaseMin;
        this.vigenteDesde = vigenteDesde;
        this.vigenteHasta = vigenteHasta;
        this.activa = true;
    }

    @Override
    public void inicializarCodigo() {
        if (codigoPlantillaHorario == null) {
            codigoPlantillaHorario = UuidV7.generar();
        }
    }
}
