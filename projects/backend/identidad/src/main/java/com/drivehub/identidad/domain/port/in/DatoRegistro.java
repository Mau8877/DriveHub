package com.drivehub.identidad.domain.port.in;

import java.time.LocalDate;

/**
 * Un dato del registro que depende de algo más que su formato: la fecha de nacimiento (edad mínima), el
 * correo y el CI (que no estén ya en uso). `valor` ya viene con formato válido.
 */
public record DatoRegistro(Tipo tipo, String valor) {

    public enum Tipo { FECHA_NACIMIENTO, CORREO, CI }

    public LocalDate comoFecha() {
        return LocalDate.parse(valor);
    }
}
