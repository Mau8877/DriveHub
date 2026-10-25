package com.drivehub.pagos.features.billetera.dto;

/**
 * Lo que dejó una devolución de horas (CU27).
 *
 * @param horasDevueltas las horas que volvieron a la billetera
 * @param horasDisponibles lo que tiene la billetera ahora
 */
public record DevolucionResultado(int horasDevueltas, int horasDisponibles) {
}
