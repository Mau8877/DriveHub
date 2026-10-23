package com.drivehub.identidad.domain.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import com.drivehub.identidad.domain.model.SexoPersona;

/**
 * Datos de la persona a cambiar (CU06, CU08a). Cada campo es opcional: vacío significa "no tocar".
 *
 * * `telefono`: presente pero vacío (o en blanco) lo borra.
 * * `latitud` y `longitud` van juntas: el servidor recalcula la dirección. No hay dirección ni
 * * categoría: la dirección se calcula y la categoría la fija la escuela (CU08).
 * * No lleva el correo, la contraseña ni la foto: tienen sus propios casos de uso.
 */
public record EditarPerfilComando(
        UUID usuarioCodigo,
        Optional<String> nombres,
        Optional<String> apellidos,
        Optional<String> telefono,
        Optional<LocalDate> fechaNacimiento,
        Optional<String> ci,
        Optional<SexoPersona> sexo,
        Optional<BigDecimal> latitud,
        Optional<BigDecimal> longitud) {

    /** Solo los datos personales de CU06, sin CI, sexo ni ubicación. */
    public EditarPerfilComando(UUID usuarioCodigo, Optional<String> nombres, Optional<String> apellidos,
            Optional<String> telefono, Optional<LocalDate> fechaNacimiento) {
        this(usuarioCodigo, nombres, apellidos, telefono, fechaNacimiento,
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }
}
