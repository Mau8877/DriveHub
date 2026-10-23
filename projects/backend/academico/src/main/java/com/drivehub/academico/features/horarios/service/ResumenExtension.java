package com.drivehub.academico.features.horarios.service;

/**
 * Resultado de una corrida de la generación diaria de franjas.
 */
public record ResumenExtension(int horariosProcesados, int franjasCreadas, int horariosConError) {
}
