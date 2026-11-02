package com.drivehub.academico.features.clases.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.drivehub.academico.common.model.BaseDocument;
import com.drivehub.academico.common.util.UuidV7;
import com.drivehub.academico.features.clases.enums.Asistencia;
import com.drivehub.academico.features.clases.enums.EstadoClase;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Clase dictada (CU17).
 */
@Getter
@NoArgsConstructor
@Document(collection = "clases")
public class Clase extends BaseDocument {

    // * Identificador público (UUID v7). El _id Long es interno, ver BaseDocument.
    @Indexed(unique = true)
    private UUID codigoClase;

    // * _id de reservas
    @Indexed(unique = true)
    private Long reservaId;

    @Indexed
    private UUID estudianteCodigo;

    @Indexed
    private UUID instructorCodigo;

    private UUID vehiculoCodigo;

    private LocalDate fecha;

    // * Opcional
    @Setter
    private Instant inicioReal;

    // * Opcional
    @Setter
    private Instant finReal;

    @Setter
    private Asistencia asistencia;

    @Setter
    private String tema;

    @Setter
    private String observaciones;

    @Setter
    private Integer kmInicio;

    @Setter
    private Integer kmFin;

    @Setter
    private EstadoClase estado;

    /**
     * Una clase dictada que se cierra (CU17): `CERRADA` desde el primer momento (no hay paso de «iniciar»). El
     * vehículo, la hora real de inicio y la de fin quedan vacíos.
     */
    public Clase(Long reservaId, UUID estudianteCodigo, UUID instructorCodigo, LocalDate fecha,
            Asistencia asistencia) {
        this.reservaId = reservaId;
        this.estudianteCodigo = estudianteCodigo;
        this.instructorCodigo = instructorCodigo;
        this.fecha = fecha;
        this.asistencia = asistencia;
        this.estado = EstadoClase.CERRADA;
    }

    @Override
    public void inicializarCodigo() {
        if (codigoClase == null) {
            codigoClase = UuidV7.generar();
        }
    }
}
