package com.drivehub.identidad.infrastructure.adapter.in.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import com.drivehub.identidad.domain.model.SexoPersona;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Cuerpo de PATCH /api/v1/usuarios/me (CU06): solo los campos que cambian.
 *
 * * Todos son opcionales; las reglas son las del registro y deben coincidir con las de la web
 * * y mobile. No hay campo de correo, rol ni escuela: no se editan desde acá.
 * * Es una clase y no un `record` porque PATCH necesita distinguir "campo ausente" (no se toca) de
 * * "`telefono: null` explícito" (se borra), y un record no puede saber si Jackson llamó o no al
 * * setter. El setter de `telefono` anota que el campo vino en el cuerpo.
 */
@Getter
@Setter
public class EditarPerfilRequest {

    @Pattern(regexp = ".*\\S.*", message = "Los nombres no pueden estar vacíos")
    @Size(max = 80, message = "Los nombres no pueden superar 80 caracteres")
    private String nombres;

    @Pattern(regexp = ".*\\S.*", message = "Los apellidos no pueden estar vacíos")
    @Size(max = 80, message = "Los apellidos no pueden superar 80 caracteres")
    private String apellidos;

    @Size(max = 20, message = "El teléfono no puede superar 20 caracteres")
    private String telefono;

    @Setter(lombok.AccessLevel.NONE)
    private boolean telefonoPresente;

    @Past(message = "La fecha de nacimiento debe estar en el pasado")
    private LocalDate fechaNacimiento;

    @Pattern(regexp = "[A-Za-z0-9-]+", message = "El CI solo admite letras, números y guion")
    @Size(min = 5, max = 15, message = "El CI debe tener entre 5 y 15 caracteres")
    private String ci;

    private SexoPersona sexo;

    // * CU08a: van juntas (el servicio lo comprueba). No hay `direccion` ni `categoriaSigla`: la dirección
    // * se calcula en el servidor y la categoría la fija la escuela; si llegan en el cuerpo, se ignoran.
    @DecimalMin(value = "-90", message = "La latitud no es válida")
    @DecimalMax(value = "90", message = "La latitud no es válida")
    private BigDecimal latitud;

    @DecimalMin(value = "-180", message = "La longitud no es válida")
    @DecimalMax(value = "180", message = "La longitud no es válida")
    private BigDecimal longitud;

    public void setTelefono(String telefono) {
        this.telefono = telefono;
        this.telefonoPresente = true;
    }

    /** El teléfono tal como lo entiende el caso de uso: ausente = no tocar, vacío = borrar. */
    public Optional<String> telefonoParaCambiar() {
        return telefonoPresente ? Optional.of(telefono == null ? "" : telefono) : Optional.empty();
    }
}
