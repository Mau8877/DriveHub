package com.drivehub.academico.features.reservas.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.springframework.stereotype.Component;

import com.drivehub.academico.config.ReservasProperties;
import com.drivehub.academico.features.reservas.model.Reserva;

import lombok.RequiredArgsConstructor;

/**
 * La regla de «a tiempo» de cancelar y reprogramar una clase (CU15): un solo lugar, con el reloj del servicio.
 *
 * * Es «a tiempo» si faltan `app.reservas.horas-para-cancelar` horas o más para el inicio de la clase; el límite
 * * exacto cuenta como a tiempo. Una clase que ya empezó nunca está a tiempo.
 * * Pagos no sabe de plazos: esta regla se decide en Académico ANTES de llamar a la billetera.
 */
@Component
@RequiredArgsConstructor
public class PlazoDeClase {

    private final ReservasProperties propiedades;
    private final Clock clock;

    /** La fecha y la hora de inicio de la clase ya pasaron en la zona del servicio. */
    public boolean haEmpezado(Reserva reserva) {
        return !inicio(reserva).isAfter(LocalDateTime.now(clock));
    }

    /** Faltan `horasParaCancelar` horas o más para el inicio de la clase. */
    public boolean aTiempo(Reserva reserva) {
        LocalDateTime ahora = LocalDateTime.now(clock);
        LocalDateTime inicio = inicio(reserva);
        return !inicio.isBefore(ahora.plusHours(propiedades.horasParaCancelar()));
    }

    private static LocalDateTime inicio(Reserva reserva) {
        return LocalDateTime.of(reserva.getFecha(), LocalTime.parse(reserva.getHoraInicio()));
    }
}
