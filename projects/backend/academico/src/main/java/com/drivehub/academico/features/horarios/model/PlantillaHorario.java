package com.drivehub.academico.features.horarios.model;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;

import com.drivehub.academico.common.model.BaseDocument;
import com.drivehub.academico.common.util.UuidV7;
import com.drivehub.academico.features.horarios.enums.EstadoPlantilla;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Horario semanal base del instructor (CU12): un nombre, uno o varios días y una ventana de horas
 * que es la misma para todos ellos.
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

    // * Hasta 60 caracteres. No es único
    private String nombre;

    // * 1 (lunes) a 7 (domingo), sin repetir y ordenado
    private List<Integer> diasSemana;

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
    private EstadoPlantilla estado;

    /**
     * Horario nuevo, activo. Los días se guardan sin repetir y ordenados. El vehículo no se asigna acá (CU10).
     */
    public PlantillaHorario(UUID escuelaCodigo, UUID instructorCodigo, String nombre,
            Collection<Integer> diasSemana, String horaInicio, String horaFin, int duracionClaseMin,
            LocalDate vigenteDesde, LocalDate vigenteHasta) {
        this.escuelaCodigo = escuelaCodigo;
        this.instructorCodigo = instructorCodigo;
        this.nombre = nombre;
        this.diasSemana = List.copyOf(new TreeSet<>(diasSemana));
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.duracionClaseMin = duracionClaseMin;
        this.vigenteDesde = vigenteDesde;
        this.vigenteHasta = vigenteHasta;
        this.estado = EstadoPlantilla.ACTIVA;
    }

    public boolean estaActiva() {
        return estado == EstadoPlantilla.ACTIVA;
    }

    @Override
    public void inicializarCodigo() {
        if (codigoPlantillaHorario == null) {
            codigoPlantillaHorario = UuidV7.generar();
        }
    }
}
