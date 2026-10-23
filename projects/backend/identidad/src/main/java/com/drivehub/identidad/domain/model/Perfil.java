package com.drivehub.identidad.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Builder;
import lombok.Getter;

/**
 * Perfil de la persona (CU08a): los datos de quien es, separados de los de acceso de la cuenta.
 * Modelo de dominio: no conoce JPA y no tiene id interno (su identidad es la del `Usuario`).
 *
 * * Todo usuario tiene uno, y uno solo, cualquiera sea su rol.
 * * `direccion` la calcula siempre el servidor a partir de `latitud` y `longitud`.
 * * `categoriaSigla` solo la tiene el estudiante (referencia por código al catálogo de Escuela).
 */
@Getter
@Builder(toBuilder = true)
public class Perfil {

    private final String nombres;
    private final String apellidos;
    private final String telefono;
    // * Clave del archivo en el bucket drivehub-identidad
    private final String fotoUrl;
    private final LocalDate fechaNacimiento;
    // * Documento de identidad: único en toda la plataforma, sin espacios y en mayúsculas
    private final String ci;
    private final SexoPersona sexo;
    private final String direccion;
    private final BigDecimal latitud;
    private final BigDecimal longitud;
    private final String categoriaSigla;
}
