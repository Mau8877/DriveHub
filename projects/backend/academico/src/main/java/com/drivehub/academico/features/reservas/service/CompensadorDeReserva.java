package com.drivehub.academico.features.reservas.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Clock;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.drivehub.academico.common.exception.ServicioNoDisponibleException;
import com.drivehub.academico.features.horarios.enums.EstadoFranja;
import com.drivehub.academico.features.horarios.model.Franja;
import com.drivehub.academico.features.horarios.repository.FranjaRepository;
import com.drivehub.academico.features.reservas.client.ClienteDePagos;
import com.drivehub.academico.features.reservas.enums.EstadoReserva;
import com.drivehub.academico.features.reservas.model.Reserva;
import com.drivehub.academico.features.reservas.repository.ReservaRepository;

import lombok.RequiredArgsConstructor;

/**
 * Deshace una reserva a medias (CU44): devuelve la hora, libera la franja y cancela la reserva.
 *
 * * Vive aparte del orquestador para que {@link ManejadorDeReserva} y {@link OrquestadorClases} la compartan sin
 * * depender uno del otro. Quien la expone al resto es `OrquestadorClases.deshacerReserva`.
 * * Orden: primero la hora (si no se puede devolver no se toca nada y se reintenta), después la franja, al final el
 * * estado. Cada paso se puede repetir: devolver lleva clave de idempotencia y liberar/cambiar de estado son
 * * condicionales.
 */
@Component
@RequiredArgsConstructor
public class CompensadorDeReserva {

    private static final Logger LOG = LoggerFactory.getLogger(CompensadorDeReserva.class);
    private static final String MODULO = "reservas";

    private final ReservaRepository reservaRepository;
    private final FranjaRepository franjaRepository;
    private final Clock clock;

    public ResultadoOrquestacion deshacer(Reserva reserva, String motivo, ClienteDePagos cliente) {
        Franja franja = franjaRepository.findById(reserva.getFranjaId()).orElse(null);
        if (franja == null) {
            // * Sin franja no se sabe la escuela de la billetera: queda para que la cuente como intento fallido
            LOG.warn("No se puede deshacer la reserva: su franja no existe", kv("module", MODULO),
                kv("reservaCodigo", reserva.getCodigoReserva()));
            return ResultadoOrquestacion.PENDIENTE;
        }
        try {
            cliente.devolver(franja.getEscuelaCodigo(), reserva.getEstudianteCodigo(), reserva.getCodigoReserva(),
                ReservaService.PREFIJO_CLAVE_DEVOLUCION + reserva.getCodigoReserva());
        } catch (ServicioNoDisponibleException e) {
            LOG.warn("No se pudo devolver la hora: Pagos no responde", kv("module", MODULO),
                kv("reservaCodigo", reserva.getCodigoReserva()));
            return ResultadoOrquestacion.PAGOS_NO_RESPONDE;
        }
        boolean sigueApartada = franja.getEstado() == EstadoFranja.RESERVADA
            && Objects.equals(franja.getReservaId(), reserva.getId());
        if (sigueApartada) {
            franjaRepository.liberar(franja.getCodigoFranja(), reserva.getId());
        }
        reservaRepository.cambiarEstado(reserva.getCodigoReserva(), reserva.getEstudianteCodigo(),
            EstadoReserva.PENDIENTE_PAGO, EstadoReserva.CANCELADA, motivo, clock.instant());
        return ResultadoOrquestacion.CANCELADA;
    }
}
