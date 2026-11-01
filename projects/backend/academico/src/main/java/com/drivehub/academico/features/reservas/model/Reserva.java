package com.drivehub.academico.features.reservas.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
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

    // * Horas de la billetera que costó (CU14: 1 por clase). CU15 devuelve exactamente esto al cancelar
    @Setter
    private int horasCobradas;

    // * Veces que el orquestador (CU44) intentó retomar la reserva y falló por una causa propia. Sin setter: se suma con
    // * `registrarIntentoFallido` del repository (atómico), nunca con `save()`. Una reserva vieja sin el campo lee 0
    private int intentosRetomar;

    // * true cuando el orquestador se rindió y una persona tiene que mirarla; deja de retomarse sola. Sin setter: la
    // * marca `marcarParaRevision` del repository. Una reserva vieja sin el campo lee false
    private boolean requiereRevision;

    /**
     * Reserva nueva, `PENDIENTE_PAGO` (CU14): la franja ya se apartó pero la hora todavía no se descontó.
     *
     * * `fecha`, `horaInicio` y `horaFin` son una copia de la franja (diseño de la colección), para consultar
     * * las clases del estudiante sin cruzar con `franjas`.
     * * El vehículo, el pago, la saga, el vencimiento y la solicitud quedan vacíos: no los usa el CU14.
     */
    public Reserva(UUID estudianteCodigo, UUID instructorCodigo, Long franjaId, LocalDate fecha, String horaInicio,
            String horaFin, int horasCobradas, Instant ahora) {
        this.estudianteCodigo = estudianteCodigo;
        this.instructorCodigo = instructorCodigo;
        this.franjaId = franjaId;
        this.fecha = fecha;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.horasCobradas = horasCobradas;
        this.estado = EstadoReserva.PENDIENTE_PAGO;
        this.historialEstados = new ArrayList<>(List.of(new HistorialEstado(EstadoReserva.PENDIENTE_PAGO, ahora, null)));
    }

    @Override
    public void inicializarCodigo() {
        if (codigoReserva == null) {
            codigoReserva = UuidV7.generar();
        }
    }
}
