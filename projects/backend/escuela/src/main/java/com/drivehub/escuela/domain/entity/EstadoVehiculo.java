package com.drivehub.escuela.domain.entity;

/**
 * Estado del vehículo (CU07). Solo DISPONIBLE se puede asignar o reservar.
 */
public enum EstadoVehiculo {
    DISPONIBLE,
    MANTENIMIENTO,
    BAJA;

    /**
     * Transiciones permitidas: DISPONIBLE y MANTENIMIENTO se alternan, y desde cualquiera de las
     * dos se puede dar de baja. BAJA es final.
     */
    public boolean permiteIrA(EstadoVehiculo destino) {
        return switch (this) {
            case DISPONIBLE -> destino == MANTENIMIENTO || destino == BAJA;
            case MANTENIMIENTO -> destino == DISPONIBLE || destino == BAJA;
            case BAJA -> false;
        };
    }
}
