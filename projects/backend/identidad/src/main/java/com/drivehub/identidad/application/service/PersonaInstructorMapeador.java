package com.drivehub.identidad.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;

import org.springframework.stereotype.Component;

import com.drivehub.identidad.domain.model.Perfil;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.in.PersonaInstructor;

import lombok.RequiredArgsConstructor;

/**
 * Arma lo que Escuela ve de la persona de un instructor (CU09) a partir de su cuenta y su perfil.
 *
 * * Calcula `edad` (con el reloj inyectado), `nombreCompleto` y `fechaAlta` (la fecha de alta de la
 * * cuenta, en la zona del reloj). La foto es la URL prefirmada que arma `PerfilArmador` (null si no
 * * tiene foto o no se pudo firmar).
 * * Lo comparten crear, consultar, buscar y editar la ubicación.
 */
@Component
@RequiredArgsConstructor
public class PersonaInstructorMapeador {

    private final Clock reloj;
    private final PerfilArmador perfilArmador;

    public PersonaInstructor aPersona(Usuario usuario) {
        Perfil perfil = usuario.getPerfil();
        return new PersonaInstructor(usuario.getCodigo(), perfil.getNombres(), perfil.getApellidos(),
            perfil.getNombres() + " " + perfil.getApellidos(), perfilArmador.urlDeFoto(usuario),
            Period.between(perfil.getFechaNacimiento(), LocalDate.now(reloj)).getYears(),
            perfil.getFechaNacimiento(), perfil.getSexo(), perfil.getCi(), usuario.getEmail(), perfil.getTelefono(),
            perfil.getDireccion(), perfil.getLatitud(), perfil.getLongitud(),
            usuario.getCreadoEn().atZone(reloj.getZone()).toLocalDate());
    }
}
