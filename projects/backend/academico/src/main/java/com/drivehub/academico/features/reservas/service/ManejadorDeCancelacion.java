package com.drivehub.academico.features.reservas.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.drivehub.academico.features.reservas.client.ClienteDePagos;
import com.drivehub.academico.features.reservas.enums.EstadoReserva;
import com.drivehub.academico.features.reservas.model.Reserva;

import lombok.RequiredArgsConstructor;

/**
 * Termina las cancelaciones (CU15) de las reservas `CANCELACION_PENDIENTE`: devuelve la hora (si corresponde),
 * libera la franja y deja la reserva `CANCELADA`.
 *
 * * Sirve igual en el flujo en vivo (con el Bearer del estudiante) y en el proceso programado de CU44 (con la
 * * credencial de servicio): usa el cliente que recibe, nunca uno propio.
 * * `sinDevolucion` es la cancelación tardía: la hora se pierde y no se llama a Pagos. La regla de plazo se decide
 * * ANTES, al iniciar la cancelación (Pagos no sabe de plazos).
 * * No escribe otra copia de «devolver la hora y liberar la franja»: usa el {@link CompensadorDeReserva}, el mismo
 * * que usa CU44. Pagos caído: devuelve `PAGOS_NO_RESPONDE` y la reserva sigue `CANCELACION_PENDIENTE`.
 * ! Nunca toca una reserva que no esté en sus estados.
 */
@Component
@RequiredArgsConstructor
public class ManejadorDeCancelacion implements ManejadorDeOperacion {

    static final String MOTIVO = "Cancelada por el estudiante";

    private static final Logger LOG = LoggerFactory.getLogger(ManejadorDeCancelacion.class);
    private static final String MODULO = "reservas";

    private final CompensadorDeReserva compensador;

    @Override
    public Set<EstadoReserva> estados() {
        return Set.of(EstadoReserva.CANCELACION_PENDIENTE);
    }

    @Override
    public ResultadoOrquestacion resolver(Reserva reserva, ClienteDePagos cliente) {
        ResultadoOrquestacion resultado = compensador.deshacer(reserva, EstadoReserva.CANCELACION_PENDIENTE,
            !reserva.isSinDevolucion(), MOTIVO, cliente);
        if (resultado == ResultadoOrquestacion.CANCELADA) {
            LOG.info("Cancelación terminada", kv("module", MODULO), kv("reservaCodigo", reserva.getCodigoReserva()),
                kv("estudianteCodigo", reserva.getEstudianteCodigo()), kv("sinDevolucion", reserva.isSinDevolucion()));
        } else {
            LOG.warn("La cancelación quedó pendiente", kv("module", MODULO),
                kv("reservaCodigo", reserva.getCodigoReserva()), kv("resultado", resultado));
        }
        return resultado;
    }
}
