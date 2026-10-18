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
 */
@Getter
@NoArgsConstructor
@Document(collection = "franjas")
@CompoundIndexes({
    @CompoundIndex(name = "uq_franja_instructor_fecha_hora", def = "{'instructorCodigo': 1, 'fecha': 1, 'horaInicio': 1}", unique = true),
    @CompoundIndex(name = "uq_franja_vehiculo_fecha_hora", def = "{'vehiculoCodigo': 1, 'fecha': 1, 'horaInicio': 1}", unique = true)
})
public class Franja extends BaseDocument {

    // * Identificador público (UUID v7). El _id Long es interno, ver BaseDocument.
    @Indexed(unique = true)
    private UUID codigoFranja;

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

    @Override
    public void inicializarCodigo() {
        if (codigoFranja == null) {
            codigoFranja = UuidV7.generar();
        }
    }
}
