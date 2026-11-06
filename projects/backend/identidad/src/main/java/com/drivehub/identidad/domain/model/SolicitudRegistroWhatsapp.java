package com.drivehub.identidad.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

/**
 * Los datos que una persona dictó por el chat para registrarse (CU48), a la espera de que fije su contraseña
 * en la web. No es una cuenta: no se puede iniciar sesión con ella. Solo se guarda el hash del enlace.
 */
@Getter
@Builder
public class SolicitudRegistroWhatsapp {

    private final UUID codigo;
    // * Solo dígitos, con el código de país (el identificador de WhatsApp)
    private final String telefono;
    private final String nombres;
    private final String apellidos;
    private final LocalDate fechaNacimiento;
    private final SexoPersona sexo;
    private final String ci;
    private final String correo;
    private final BigDecimal latitud;
    private final BigDecimal longitud;
    private final String tokenHash;
    private final Instant expiraEn;
}
