package com.drivehub.academico.features.reservas.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.drivehub.academico.common.exception.ServicioNoDisponibleException;
import com.drivehub.academico.config.OrquestadorProperties;
import com.drivehub.academico.features.horarios.enums.EstadoFranja;
import com.drivehub.academico.features.horarios.model.Franja;
import com.drivehub.academico.features.horarios.repository.FranjaRepository;
import com.drivehub.academico.features.reservas.client.ClienteDePagos;
import com.drivehub.academico.features.reservas.client.SinHorasException;
import com.drivehub.academico.features.reservas.enums.EstadoReserva;
import com.drivehub.academico.features.reservas.model.Reserva;
import com.drivehub.academico.features.reservas.repository.ReservaRepository;

import lombok.RequiredArgsConstructor;

/**
 * Resuelve las reservas `PENDIENTE_PAGO` (CU14) que quedaron a medias: la franja se apartó pero la confirmación no
 * terminó (CU44).
 *
 * * La franja ya no está apartada por la reserva: se deshace, sin importar la edad.
 * * Lleva `minutosParaDeshacer` o más: se deshace sin intentar completarla (el estudiante ya la dio por perdida).
 * * Es más reciente: repite el descuento con la MISMA clave `reserva-{codigo}` (si Pagos ya había descontado devuelve
 * * lo anterior, no cobra dos veces) y la confirma. Sin horas se deshace.
 * * Si Pagos no responde no se toca nada.
 */
@Component
@RequiredArgsConstructor
public class ManejadorDeReserva implements ManejadorDeOperacion {

    private static final Logger LOG = LoggerFactory.getLogger(ManejadorDeReserva.class);
    private static final String MODULO = "reservas";

    private final ReservaRepository reservaRepository;
    private final FranjaRepository franjaRepository;
    private final CompensadorDeReserva compensador;
    private final OrquestadorProperties propiedades;
    private final Clock clock;

    @Override
    public Set<EstadoReserva> estados() {
        return Set.of(EstadoReserva.PENDIENTE_PAGO);
    }

    @Override
    public ResultadoOrquestacion resolver(Reserva reserva, ClienteDePagos cliente) {
        Franja franja = franjaRepository.findById(reserva.getFranjaId()).orElse(null);
        if (franja == null) {
            LOG.warn("La franja de la reserva a medias no existe", kv("module", MODULO),
                kv("reservaCodigo", reserva.getCodigoReserva()));
            return ResultadoOrquestacion.PENDIENTE;
        }
        boolean sigueApartada = franja.getEstado() == EstadoFranja.RESERVADA
            && Objects.equals(franja.getReservaId(), reserva.getId());
        if (!sigueApartada) {
            return compensador.deshacer(reserva, "La franja ya no estaba apartada", cliente);
        }
        Duration edad = Duration.between(reserva.getCreatedAt(), clock.instant());
        if (edad.compareTo(Duration.ofMinutes(propiedades.minutosParaDeshacer())) >= 0) {
            return compensador.deshacer(reserva, "Reserva a medias vencida", cliente);
        }
        try {
            cliente.descontar(franja.getEscuelaCodigo(), reserva.getEstudianteCodigo(),
                reserva.getInstructorCodigo(), reserva.getCodigoReserva(), reserva.getHorasCobradas(),
                ReservaService.PREFIJO_CLAVE_DESCUENTO + reserva.getCodigoReserva());
        } catch (SinHorasException e) {
            return compensador.deshacer(reserva, "Sin horas suficientes", cliente);
        } catch (ServicioNoDisponibleException e) {
            LOG.warn("No se pudo retomar la reserva: Pagos no responde", kv("module", MODULO),
                kv("reservaCodigo", reserva.getCodigoReserva()));
            return ResultadoOrquestacion.PAGOS_NO_RESPONDE;
        }
        reservaRepository.cambiarEstado(reserva.getCodigoReserva(), reserva.getEstudianteCodigo(),
            EstadoReserva.PENDIENTE_PAGO, EstadoReserva.CONFIRMADA, "Reserva retomada por el sistema",
            clock.instant());
        return ResultadoOrquestacion.CONFIRMADA;
    }
}
