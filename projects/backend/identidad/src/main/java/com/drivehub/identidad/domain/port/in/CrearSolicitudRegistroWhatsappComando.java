package com.drivehub.identidad.domain.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.drivehub.identidad.domain.model.SexoPersona;

/**
 * Datos del registro por WhatsApp (CU48). No tienen rol ni escuela a propósito: son siempre ESTUDIANTE y la
 * escuela del chat. Tampoco hay contraseña: se elige en la web.
 */
public record CrearSolicitudRegistroWhatsappComando(
        String telefono,
        String nombres,
        String apellidos,
        LocalDate fechaNacimiento,
        SexoPersona sexo,
        String ci,
        String correo,
        BigDecimal latitud,
        BigDecimal longitud
) {
}
