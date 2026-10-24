package com.drivehub.academico.features.horarios.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Tarea programada que completa cada día las franjas que faltan de todos los horarios activos.
 *
 * * Sirve para una sola instancia del servicio: con más réplicas, todas correrían la tarea a la vez
 * * (es inofensivo por la idempotencia, pero duplica trabajo) y haría falta un candado (ShedLock).
 * * Se apaga con `app.horarios.job-habilitado=false` (los tests de integración la invocan a mano).
 */
@Component
@ConditionalOnProperty(name = "app.horarios.job-habilitado", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class ExtensionFranjasJob {

    private final FranjaGeneracionService generacion;

    @Scheduled(cron = "${app.horarios.extension-cron}", zone = "${app.horarios.zona-horaria}")
    public void extender() {
        generacion.extenderHorariosActivos();
    }
}
