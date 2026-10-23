package com.drivehub.academico.features.horarios.model;

import java.time.LocalDate;
import java.util.UUID;

import com.drivehub.academico.common.model.BaseDocument;
import com.drivehub.academico.common.util.UuidV7;
import com.drivehub.academico.features.horarios.enums.EstadoFranja;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Horario concreto reservable (CU11, CU12, CU14, CU16).
 * * Reservar = findOneAndUpdate de estado LIBRE a RESERVADA: atómico, dos estudiantes
 * * no pueden quedarse con la misma franja. Los índices únicos impiden duplicar
 * * horario del instructor o del vehículo.
 * * El índice del vehículo es PARCIAL: solo aplica cuando la franja tiene vehículo. Mientras
 * * CU10 no asigne autos, las franjas no lo tienen y, sin el filtro, dos instructores
 * * chocarían en la misma fecha y hora.
 * ! Si tu base de desarrollo ya tiene el índice viejo, el arranque falla por conflicto de
 * ! opciones: se resuelve una vez con
 * ! `docker exec drivehub-mongo mongosh academico --eval "db.franjas.dropIndex('uq_franja_vehiculo_fecha_hora')"`.
 */
@Getter
@NoArgsConstructor
@Document(collection = "franjas")
@CompoundIndexes({
    @CompoundIndex(name = "uq_franja_instructor_fecha_hora", def = "{'instructorCodigo': 1, 'fecha': 1, 'horaInicio': 1}", unique = true),
    @CompoundIndex(name = "uq_franja_vehiculo_fecha_hora", def = "{'vehiculoCodigo': 1, 'fecha': 1, 'horaInicio': 1}",
        unique = true, partialFilter = "{'vehiculoCodigo': {$exists: true}}")
})
public class Franja extends BaseDocument {

    // * Identificador público (UUID v7). El _id Long es interno, ver BaseDocument.
    @Indexed(unique = true)
    private UUID codigoFranja;

    // * = escuela.escuela.codigo_escuela, tomado del claim `escuela` del JWT al crear
    private UUID escuelaCodigo;

    // * Opcional. _id de plantillas_horario
    private Long plantillaId;

    private UUID instructorCodigo;

    private UUID vehiculoCodigo;

    private LocalDate fecha;

    // * HH:mm
    private String horaInicio;

    // * HH:mm
    private String horaFin;

    @Setter
    private EstadoFranja estado;

    // * Opcional. _id de reservas
    @Setter
    private Long reservaId;

    /**
     * Franja nueva, libre. El vehículo no se asigna acá (CU10).
     */
    public Franja(UUID escuelaCodigo, Long plantillaId, UUID instructorCodigo, LocalDate fecha, String horaInicio,
            String horaFin) {
        this.escuelaCodigo = escuelaCodigo;
        this.plantillaId = plantillaId;
        this.instructorCodigo = instructorCodigo;
        this.fecha = fecha;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.estado = EstadoFranja.LIBRE;
    }

    @Override
    public void inicializarCodigo() {
        if (codigoFranja == null) {
            codigoFranja = UuidV7.generar();
        }
    }
}
