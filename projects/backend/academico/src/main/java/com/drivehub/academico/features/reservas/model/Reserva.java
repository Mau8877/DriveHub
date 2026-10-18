package com.drivehub.academico.features.reservas.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.drivehub.academico.common.model.BaseDocument;
import com.drivehub.academico.common.util.UuidV7;
import com.drivehub.academico.features.reservas.enums.EstadoReserva;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Reserva de una franja (CU14, CU15, CU16).
 */
@Getter
@NoArgsConstructor
@Document(collection = "reservas")
@CompoundIndexes({
    @CompoundIndex(name = "idx_reserva_estudiante_fecha", def = "{'estudianteCodigo': 1, 'fecha': 1}"),
    @CompoundIndex(name = "idx_reserva_instructor_fecha", def = "{'instructorCodigo': 1, 'fecha': 1}"),
    @CompoundIndex(name = "idx_reserva_estado_expira", def = "{'estado': 1, 'expiraPagoEn': 1}")
})
public class Reserva extends BaseDocument {

    // * Identificador público (UUID v7). El _id Long es interno, ver BaseDocument.
    @Indexed(unique = true)
    private UUID codigoReserva;

    private UUID estudianteCodigo;

    private UUID instructorCodigo;

    private UUID vehiculoCodigo;

    // * _id de franjas
    private Long franjaId;

    // * Opcional. _id de solicitudes_clase
    private Long solicitudId;

    // * Copia de la franja, para consultar sin join
    private LocalDate fecha;

    private String horaInicio;

    private String horaFin;

    @Setter
    private EstadoReserva estado;

    // * Opcional. Pagos pago.codigo_pago
    @Setter
    private UUID pagoCodigo;

    // * Saga reserva-pago (CU44)
    @Setter
    private UUID sagaCodigo;

    // * Opcional. _id de la reserva original si es una reprogramación
    private Long reprogramadaDeId;

    @Setter
    private List<HistorialEstado> historialEstados;

    // * Si no se paga a tiempo, la saga la cancela y libera la franja
    @Setter
    private Instant expiraPagoEn;

    @Override
    public void inicializarCodigo() {
        if (codigoReserva == null) {
            codigoReserva = UuidV7.generar();
        }
    }
}
