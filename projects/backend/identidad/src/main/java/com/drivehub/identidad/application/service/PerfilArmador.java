package com.drivehub.identidad.application.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.drivehub.identidad.domain.model.Perfil;
import com.drivehub.identidad.domain.model.Rol;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.in.PerfilUsuario;
import com.drivehub.identidad.domain.port.out.AlmacenFotosPort;
import com.drivehub.identidad.domain.port.out.EscuelaConsultaPort;
import com.drivehub.identidad.domain.port.out.EscuelaRef;

/**
 * Arma el perfil que ve la persona (CU06) a partir de su cuenta. Lo comparten ver y editar el
 * perfil y subir o quitar la foto, y el login y la renovación usan su `urlDeFoto`.
 *
 * * Lo que está en otro servicio o en el almacenamiento nunca impide mostrar el perfil: si Escuela
 * * no responde, `escuelaNombre` viaja null; si no se puede firmar la foto, `fotoUrl` viaja null.
 */
@Component
public class PerfilArmador {

    private static final Logger LOG = LoggerFactory.getLogger(PerfilArmador.class);
    private static final String MODULO = "perfil";

    private final AlmacenFotosPort almacenFotos;
    private final EscuelaConsultaPort escuelaConsulta;

    public PerfilArmador(AlmacenFotosPort almacenFotos, EscuelaConsultaPort escuelaConsulta) {
        this.almacenFotos = almacenFotos;
        this.escuelaConsulta = escuelaConsulta;
    }

    public PerfilUsuario armar(Usuario usuario) {
        Perfil perfil = usuario.getPerfil();
        List<String> roles = usuario.getRoles().stream().map(Rol::getNombre).sorted().toList();
        return new PerfilUsuario(usuario.getCodigo(), usuario.getNombres(), usuario.getApellidos(),
            usuario.getFechaNacimiento(), usuario.getTelefono(), usuario.getEmail(), usuario.isCorreoVerificado(),
            roles, usuario.getEscuelaCodigo(), nombreDeEscuela(usuario), urlDeFoto(usuario),
            perfil.getCi(), perfil.getSexo(), perfil.getDireccion(), perfil.getLatitud(), perfil.getLongitud(),
            perfil.getCategoriaSigla());
    }

    /** La URL firmada de la foto, o null si no tiene foto o no se pudo firmar (nunca rompe al llamador). */
    public String urlDeFoto(Usuario usuario) {
        if (usuario.getFotoUrl() == null) {
            return null;
        }
        try {
            return almacenFotos.urlDeLectura(usuario.getFotoUrl());
        } catch (RuntimeException e) {
            LOG.warn("No se pudo firmar la URL de la foto: se sigue sin ella",
                kv("module", MODULO), kv("usuarioCodigo", usuario.getCodigo()));
            return null;
        }
    }

    private String nombreDeEscuela(Usuario usuario) {
        if (usuario.getEscuelaCodigo() == null) {
            return null;
        }
        try {
            return escuelaConsulta.buscarPorCodigo(usuario.getEscuelaCodigo()).map(EscuelaRef::nombre).orElse(null);
        } catch (RuntimeException e) {
            LOG.warn("Escuela no respondió: el perfil se muestra sin el nombre de la escuela",
                kv("module", MODULO), kv("usuarioCodigo", usuario.getCodigo()));
            return null;
        }
    }
}
