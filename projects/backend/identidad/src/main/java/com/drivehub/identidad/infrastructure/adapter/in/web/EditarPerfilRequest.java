package com.drivehub.identidad.infrastructure.adapter.in.web;

import java.time.LocalDate;
import java.util.Optional;

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

    public void setTelefono(String telefono) {
        this.telefono = telefono;
        this.telefonoPresente = true;
    }

    /** El teléfono tal como lo entiende el caso de uso: ausente = no tocar, vacío = borrar. */
    public Optional<String> telefonoParaCambiar() {
        return telefonoPresente ? Optional.of(telefono == null ? "" : telefono) : Optional.empty();
    }
}
