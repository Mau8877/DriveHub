package com.drivehub.pagos.features.billetera.dto;

/**
 * Lo que dejó un consumo de horas (CU27).
 *
 * @param horasDescontadas las horas que se descontaron (las mismas de un reintento con la misma clave)
 * @param horasDisponibles lo que le queda a la billetera
 */
public record ConsumoResultado(int horasDescontadas, int horasDisponibles) {
}
