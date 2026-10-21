package com.drivehub.academico.features.horarios.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Calcula qué franjas corresponden a un horario semanal (uno o varios días) en una ventana de fechas.
 *
 * * Es lógica pura: no toca la base ni valida el horario (eso lo hace PlantillaHorarioService).
 * * Asume una ventana de horas dentro del mismo día y múltiplo exacto de la duración.
 * * La ventana de fechas cuenta el día de hoy: con 28 días, cada día de la semana aparece 4 veces.
 * * Solo genera franjas futuras: un bloque que empieza exactamente ahora ya no se genera.
 */
@Component
public class GeneradorFranjas {

    public List<FranjaCalculada> calcular(Collection<Integer> diasSemana, LocalTime horaInicio, LocalTime horaFin,
            int duracionClaseMin, LocalDate vigenteDesde, LocalDate vigenteHasta, LocalDateTime ahora,
            int horizonteDias) {
        if (duracionClaseMin <= 0) {
            throw new IllegalArgumentException("La duración de la clase debe ser positiva");
        }
        LocalDate hoy = ahora.toLocalDate();
        LocalDate desde = vigenteDesde.isAfter(hoy) ? vigenteDesde : hoy;
        LocalDate topeHorizonte = hoy.plusDays(horizonteDias - 1L);
        LocalDate hasta = vigenteHasta != null && vigenteHasta.isBefore(topeHorizonte) ? vigenteHasta : topeHorizonte;

        // * Se itera en minutos del día y no con LocalTime.plusMinutes, que da la vuelta a medianoche
        int inicioMin = horaInicio.getHour() * 60 + horaInicio.getMinute();
        int finMin = horaFin.getHour() * 60 + horaFin.getMinute();

        List<FranjaCalculada> franjas = new ArrayList<>();
        for (LocalDate fecha = desde; !fecha.isAfter(hasta); fecha = fecha.plusDays(1)) {
            if (!diasSemana.contains(fecha.getDayOfWeek().getValue())) {
                continue;
            }
            for (int minuto = inicioMin; minuto + duracionClaseMin <= finMin; minuto += duracionClaseMin) {
                LocalTime inicio = LocalTime.of(minuto / 60, minuto % 60);
                if (!LocalDateTime.of(fecha, inicio).isAfter(ahora)) {
                    continue;
                }
                int finBloque = minuto + duracionClaseMin;
                franjas.add(new FranjaCalculada(fecha, inicio, LocalTime.of(finBloque / 60, finBloque % 60)));
            }
        }
        return franjas;
    }
}
