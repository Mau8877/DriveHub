package com.drivehub.escuela.application.usecase;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;

import org.springframework.stereotype.Component;

import com.drivehub.escuela.application.dto.CategoriaOutput;
import com.drivehub.escuela.application.dto.InstructorDetalleOutput;
import com.drivehub.escuela.application.dto.InstructorResumenOutput;
import com.drivehub.escuela.domain.entity.Instructor;
import com.drivehub.escuela.domain.gateway.PersonaInstructor;

import lombok.RequiredArgsConstructor;

/**
 * Arma lo que la escuela ve de un instructor (CU09) a partir de su ficha y de su persona en Identidad.
 *
 * * Calcula `edad` (con el reloj inyectado) y `fechaAlta` (el día del alta de la ficha, en la zona
 * * del reloj). Las categorías salen en el orden del catálogo, que es el que trae la ficha.
 * * Si la persona NO viene (`null`: Identidad no la devolvió), los datos de persona quedan en `null`
 * * y el instructor se muestra igual con los de su ficha.
 * * Lo comparten listar, obtener, crear y editar: el detalle es siempre el mismo.
 */
@Component
@RequiredArgsConstructor
public class InstructorMapeador {

    private final Clock reloj;

    public InstructorResumenOutput aResumen(Instructor ficha, PersonaInstructor persona) {
        return new InstructorResumenOutput(ficha.getCodigo(),
            persona == null ? null : persona.nombreCompleto(), persona == null ? null : persona.fotoUrl(),
            ficha.getNroLicencia(), ficha.getLicenciaVence(), categorias(ficha), ficha.getEstado());
    }

    public InstructorDetalleOutput aDetalle(Instructor ficha, PersonaInstructor persona) {
        LocalDate fechaAlta = ficha.getCreadoEn() == null
            ? null : ficha.getCreadoEn().atZone(reloj.getZone()).toLocalDate();
        if (persona == null) {
            return new InstructorDetalleOutput(ficha.getCodigo(), null, null, null, null, null, null, null, null,
                null, null, null, null, null, ficha.getNroLicencia(), ficha.getLicenciaVence(),
                ficha.getFechaContratacion(), categorias(ficha), ficha.getEstado(), fechaAlta);
        }
        return new InstructorDetalleOutput(ficha.getCodigo(), persona.nombres(), persona.apellidos(),
            persona.nombreCompleto(), persona.fotoUrl(), edad(persona), persona.fechaNacimiento(), persona.sexo(),
            persona.ci(), persona.email(), persona.telefono(), persona.direccion(), persona.latitud(),
            persona.longitud(), ficha.getNroLicencia(), ficha.getLicenciaVence(), ficha.getFechaContratacion(),
            categorias(ficha), ficha.getEstado(), fechaAlta);
    }

    private Integer edad(PersonaInstructor persona) {
        return persona.fechaNacimiento() == null
            ? null : Period.between(persona.fechaNacimiento(), LocalDate.now(reloj)).getYears();
    }

    private List<CategoriaOutput> categorias(Instructor ficha) {
        return ficha.getCategorias().stream().map(c -> new CategoriaOutput(c.getSigla(), c.getDescripcion())).toList();
    }
}
