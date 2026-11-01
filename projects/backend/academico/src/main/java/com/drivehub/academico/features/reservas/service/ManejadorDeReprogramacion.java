package com.drivehub.academico.features.reservas.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.drivehub.academico.features.horarios.enums.EstadoFranja;
import com.drivehub.academico.features.horarios.model.Franja;
import com.drivehub.academico.features.horarios.repository.FranjaRepository;
import com.drivehub.academico.features.reservas.client.ClienteDePagos;
import com.drivehub.academico.features.reservas.enums.EstadoReserva;
import com.drivehub.academico.features.reservas.model.Reserva;
import com.drivehub.academico.features.reservas.repository.ReservaRepository;

import lombok.RequiredArgsConstructor;

/**
 * Termina o deshace las reprogramaciones (CU15) de las reservas `REPROGRAMACION_PENDIENTE`: la clase se mueve a otra
 * franja sin tocar las horas.
 *
 * * Orden de los pasos, para que nadie se quede sin franja ni con dos: (1) la franja destino es de la reserva,
 * * (2) se libera la anterior, (3) la reserva pasa a la destino y vuelve a `CONFIRMADA`. La anterior solo se
 * * libera DESPUÉS de asegurar la nueva: si algo se corta antes, la clase sigue en su franja original.
 * * Cada paso es repetible (`liberar` y las operaciones del repository son condicionales), así que correr esto dos
 * * veces, o a la vez, no libera de más ni deja a la reserva en dos franjas.
 * * No usa el cliente de Pagos: reprogramar no mueve horas.
 * ! Nunca toca una reserva que no esté en sus estados.
 */
@Component
@RequiredArgsConstructor
public class ManejadorDeReprogramacion implements ManejadorDeOperacion {

    static final String MOTIVO_REVERTIDA = "La franja elegida ya no estaba disponible";

    private static final Logger LOG = LoggerFactory.getLogger(ManejadorDeReprogramacion.class);
    private static final String MODULO = "reservas";
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final ReservaRepository reservaRepository;
    private final FranjaRepository franjaRepository;
    private final Clock clock;

    @Override
    public Set<EstadoReserva> estados() {
        return Set.of(EstadoReserva.REPROGRAMACION_PENDIENTE);
    }

    /** El cliente de Pagos no se usa: reprogramar no toca las horas. */
    @Override
    public ResultadoOrquestacion resolver(Reserva reserva, ClienteDePagos cliente) {
        return terminar(reserva);
    }

    /**
     * La secuencia completa, que también usa el flujo en vivo de `ReservaService` al final de reprogramar.
     *
     * @return `CONFIRMADA` si la clase quedó confirmada, ya sea en la franja nueva o (si no se pudo) en la original;
     *         `PENDIENTE` si no pudo avanzar por una causa propia (cuenta como intento fallido)
     */
    public ResultadoOrquestacion terminar(Reserva reserva) {
        Long destinoId = reserva.getFranjaDestinoId();
        Franja destino = destinoId == null ? null : franjaRepository.findById(destinoId).orElse(null);
        if (destino == null) {
            return revertir(reserva, "La franja destino no existe");
        }
        if (!esDeLaReserva(destino, reserva)) {
            if (destino.getEstado() != EstadoFranja.LIBRE || !apartarDeNuevo(destino, reserva)) {
                return revertir(reserva, MOTIVO_REVERTIDA);
            }
        }
        Franja anterior = franjaRepository.findById(reserva.getFranjaId()).orElse(null);
        if (anterior == null) {
            LOG.warn("La franja anterior de la reserva no existe: no hay nada que liberar", kv("module", MODULO),
                kv("reservaCodigo", reserva.getCodigoReserva()));
        } else {
            franjaRepository.liberar(anterior.getCodigoFranja(), reserva.getId());
        }
        reservaRepository.completarReprogramacion(reserva.getCodigoReserva(), reserva.getEstudianteCodigo(),
            destino.getId(), destino.getFecha(), destino.getHoraInicio(), destino.getHoraFin(),
            destino.getInstructorCodigo(), clock.instant());
        LOG.info("Reprogramación terminada", kv("module", MODULO), kv("reservaCodigo", reserva.getCodigoReserva()),
            kv("franjaDestinoCodigo", destino.getCodigoFranja()));
        return ResultadoOrquestacion.CONFIRMADA;
    }

    private boolean apartarDeNuevo(Franja destino, Reserva reserva) {
        LocalDateTime ahora = LocalDateTime.now(clock);
        return franjaRepository.apartar(destino.getCodigoFranja(), destino.getEscuelaCodigo(), reserva.getId(),
            ahora.toLocalDate(), ahora.toLocalTime().format(HORA)).isPresent();
    }

    private ResultadoOrquestacion revertir(Reserva reserva, String motivo) {
        LOG.warn("Reprogramación deshecha: la clase sigue en su franja original", kv("module", MODULO),
            kv("reservaCodigo", reserva.getCodigoReserva()), kv("motivo", motivo));
        var revertida = reservaRepository.revertirReprogramacion(reserva.getCodigoReserva(),
            reserva.getEstudianteCodigo(), motivo, clock.instant());
        return revertida.isPresent() ? ResultadoOrquestacion.CONFIRMADA : ResultadoOrquestacion.PENDIENTE;
    }

    private static boolean esDeLaReserva(Franja franja, Reserva reserva) {
        return franja.getEstado() == EstadoFranja.RESERVADA && Objects.equals(franja.getReservaId(), reserva.getId());
    }
}
