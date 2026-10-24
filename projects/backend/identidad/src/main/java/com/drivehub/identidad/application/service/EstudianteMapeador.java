package com.drivehub.identidad.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;

import org.springframework.stereotype.Component;

import com.drivehub.identidad.domain.model.EstadoEstudiante;
import com.drivehub.identidad.domain.model.EstadoUsuario;
import com.drivehub.identidad.domain.model.Perfil;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.in.EstudianteDetalle;
import com.drivehub.identidad.domain.port.in.EstudianteResumen;

import lombok.RequiredArgsConstructor;

/**
 * Arma lo que la escuela ve de un estudiante (CU08) a partir de su cuenta y su perfil.
 *
 * * Calcula `edad` (con el reloj inyectado), `nombreCompleto`, `fechaIncorporacion` (la fecha de
 * * alta de la cuenta, en la zona del reloj) y el `estado` público: BLOQUEADO se muestra ACTIVO.
 * * La foto es la URL prefirmada de lectura que ya arma `PerfilArmador` (null si no tiene foto o si
 * * no se pudo firmar: nunca impide mostrar al estudiante).
 * * Lo comparten listar, obtener, crear y editar: el detalle es siempre el mismo.
 */
@Component
@RequiredArgsConstructor
public class EstudianteMapeador {

    private final Clock reloj;
    private final PerfilArmador perfilArmador;

    public EstudianteResumen aResumen(Usuario usuario) {
        Perfil perfil = usuario.getPerfil();
        return new EstudianteResumen(usuario.getCodigo(), nombreCompleto(perfil), perfilArmador.urlDeFoto(usuario), edad(perfil),
            perfil.getCategoriaSigla(), estado(usuario), fechaIncorporacion(usuario));
    }

    public EstudianteDetalle aDetalle(Usuario usuario) {
        Perfil perfil = usuario.getPerfil();
        return new EstudianteDetalle(usuario.getCodigo(), perfil.getNombres(), perfil.getApellidos(),
            nombreCompleto(perfil), perfilArmador.urlDeFoto(usuario), edad(perfil), perfil.getFechaNacimiento(), perfil.getSexo(), perfil.getCi(),
            usuario.getEmail(), perfil.getTelefono(), perfil.getDireccion(), perfil.getLatitud(),
            perfil.getLongitud(), perfil.getCategoriaSigla(), estado(usuario), fechaIncorporacion(usuario));
    }

    private String nombreCompleto(Perfil perfil) {
        return perfil.getNombres() + " " + perfil.getApellidos();
    }

    private int edad(Perfil perfil) {
        return Period.between(perfil.getFechaNacimiento(), LocalDate.now(reloj)).getYears();
    }

    private EstadoEstudiante estado(Usuario usuario) {
        return usuario.getEstado() == EstadoUsuario.INACTIVO ? EstadoEstudiante.INACTIVO : EstadoEstudiante.ACTIVO;
    }

    private LocalDate fechaIncorporacion(Usuario usuario) {
        return usuario.getCreadoEn().atZone(reloj.getZone()).toLocalDate();
    }
}
