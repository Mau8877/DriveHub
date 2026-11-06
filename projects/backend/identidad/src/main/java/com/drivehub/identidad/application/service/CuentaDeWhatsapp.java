package com.drivehub.identidad.application.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.drivehub.identidad.domain.model.EstadoUsuario;
import com.drivehub.identidad.domain.model.Rol;
import com.drivehub.identidad.domain.model.RolNombres;
import com.drivehub.identidad.domain.model.Usuario;

/**
 * Qué cuentas pueden usar el chat de WhatsApp (CU47): estudiantes activos de la escuela de este chat.
 * Instructores, administradores y el Super admin no lo usan.
 */
@Component
public class CuentaDeWhatsapp {

    private final UUID escuelaCodigo;

    public CuentaDeWhatsapp(@Value("${app.whatsapp.escuela-codigo}") UUID escuelaCodigo) {
        this.escuelaCodigo = escuelaCodigo;
    }

    public boolean habilitada(Usuario usuario) {
        return usuario.getEstado() == EstadoUsuario.ACTIVO
            && escuelaCodigo.equals(usuario.getEscuelaCodigo())
            && usuario.getRoles() != null
            && usuario.getRoles().stream().map(Rol::getNombre).anyMatch(RolNombres.ESTUDIANTE::equals);
    }
}
