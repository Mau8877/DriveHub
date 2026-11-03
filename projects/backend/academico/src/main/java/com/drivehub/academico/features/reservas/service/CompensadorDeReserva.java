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
    private final AvisosReserva avisos;

    /** Deshace una reserva `PENDIENTE_PAGO` (CU14, CU44): devuelve la hora, libera la franja y la cancela. */
    public ResultadoOrquestacion deshacer(Reserva reserva, String motivo, ClienteDePagos cliente) {
        return deshacer(reserva, EstadoReserva.PENDIENTE_PAGO, true, motivo, cliente);
    }

    /**
     * Lo mismo para una reserva en cualquier estado de origen (CU15: una `CANCELACION_PENDIENTE`).
     *
     * * `devolverHora` falso es la cancelación tardía: la hora se pierde y NO se llama a Pagos; sin devolución
     * * tampoco hace falta la franja para conocer la escuela, así que si no existe igual se cancela.
     */
    public ResultadoOrquestacion deshacer(Reserva reserva, EstadoReserva desde, boolean devolverHora, String motivo,
            ClienteDePagos cliente) {
        return deshacer(reserva, desde, devolverHora, false, motivo, cliente);
    }

    /**
     * Lo mismo, pudiendo dejar la franja `BLOQUEADA` en vez de `LIBRE` (CU16: el instructor canceló porque no puede
     * atender, así que la franja no se vuelve a ofrecer).
     */
    public ResultadoOrquestacion deshacer(Reserva reserva, EstadoReserva desde, boolean devolverHora,
            boolean bloquearFranja, String motivo, ClienteDePagos cliente) {
        Franja franja = franjaRepository.findById(reserva.getFranjaId()).orElse(null);
        if (franja == null) {
            if (!devolverHora) {
                reservaRepository.cambiarEstado(reserva.getCodigoReserva(), reserva.getEstudianteCodigo(), desde,
                    EstadoReserva.CANCELADA, motivo, clock.instant())
                    .ifPresent(cancelada -> avisos.claseCancelada(cancelada, desde != EstadoReserva.PENDIENTE_PAGO));
                return ResultadoOrquestacion.CANCELADA;
            }
            // * Sin franja no se sabe la escuela de la billetera: queda para que la cuente como intento fallido
            LOG.warn("No se puede deshacer la reserva: su franja no existe", kv("module", MODULO),
                kv("reservaCodigo", reserva.getCodigoReserva()));
            return ResultadoOrquestacion.PENDIENTE;
        }
        boolean sigueApartada = franja.getEstado() == EstadoFranja.RESERVADA
            && Objects.equals(franja.getReservaId(), reserva.getId());
        return deshacer(reserva, franja, sigueApartada, desde, devolverHora, bloquearFranja, true, motivo, cliente);
    }

    /**
     * Lo mismo para el flujo en vivo de reservar: la franja se acaba de apartar para esta reserva (el objeto que se
     * tiene todavía la muestra `LIBRE`), así que no hace falta volver a leerla.
     */
    public ResultadoOrquestacion deshacerApartada(Reserva reserva, Franja franja, String motivo,
            ClienteDePagos cliente) {
        // * En vivo el estudiante recibe el resultado en la misma respuesta: no se le manda además un aviso
        return deshacer(reserva, franja, true, EstadoReserva.PENDIENTE_PAGO, true, false, false, motivo, cliente);
    }

    private ResultadoOrquestacion deshacer(Reserva reserva, Franja franja, boolean sigueApartada, EstadoReserva desde,
            boolean devolverHora, boolean bloquearFranja, boolean avisar, String motivo, ClienteDePagos cliente) {
        if (devolverHora) {
            try {
                cliente.devolver(franja.getEscuelaCodigo(), reserva.getEstudianteCodigo(),
                    reserva.getCodigoReserva(), OrquestadorClases.PREFIJO_CLAVE_DEVOLUCION + reserva.getCodigoReserva());
            } catch (ServicioNoDisponibleException e) {
                LOG.warn("No se pudo devolver la hora: Pagos no responde", kv("module", MODULO),
                    kv("reservaCodigo", reserva.getCodigoReserva()));
                return ResultadoOrquestacion.PAGOS_NO_RESPONDE;
            }
        }
        if (sigueApartada) {
            if (bloquearFranja) {
                franjaRepository.bloquearReservada(franja.getCodigoFranja(), reserva.getId());
            } else {
                franjaRepository.liberar(franja.getCodigoFranja(), reserva.getId());
            }
        }
        var cancelada = reservaRepository.cambiarEstado(reserva.getCodigoReserva(), reserva.getEstudianteCodigo(),
            desde, EstadoReserva.CANCELADA, motivo, clock.instant());
        if (avisar) {
            cancelada.ifPresent(r -> avisos.claseCancelada(r, desde != EstadoReserva.PENDIENTE_PAGO));
        }
        return ResultadoOrquestacion.CANCELADA;
    }
}
