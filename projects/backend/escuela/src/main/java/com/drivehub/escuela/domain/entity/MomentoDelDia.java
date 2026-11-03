package com.drivehub.escuela.domain.entity;

/**
 * Un tramo del día para consultar la disponibilidad (CU11), sobre la hora de inicio de la franja y con el mismo
 * criterio que la reserva de clases: mañana antes de las 12:00, tarde desde las 12:00 hasta antes de las 18:00 y
 * noche desde las 18:00.
 */
public enum MomentoDelDia {
    MANANA(null, "12:00"),
    TARDE("12:00", "18:00"),
    NOCHE("18:00", null);

    private final String horaDesde;
    private final String horaHasta;

    MomentoDelDia(String horaDesde, String horaHasta) {
        this.horaDesde = horaDesde;
        this.horaHasta = horaHasta;
    }

    /** `HH:mm`, inclusiva; nula si el tramo no tiene límite inferior. */
    public String horaDesde() {
        return horaDesde;
    }

    /** `HH:mm`, exclusiva; nula si el tramo no tiene límite superior. */
    public String horaHasta() {
        return horaHasta;
    }
}
