package com.drivehub.identidad.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;

import lombok.Builder;
import lombok.Getter;

/**
 * Cuenta de acceso (CU01, CU02, CU06) con su `Perfil` (CU08a). Modelo de dominio: no conoce JPA.
 *
 * * `codigo` es el identificador de persona en TODA la plataforma: el
 * * mismo UUID viaja como usuarioCodigo / estudianteCodigo / instructorCodigo
 * * en los demás microservicios. El id interno (Long) es un detalle del
 * * adaptador de persistencia y no existe en el dominio.
 * * Los datos de la persona viven en `perfil`. Por comodidad, `getNombres()` y compañía los
 * * delegan, y el builder acepta `nombres(...)`, `telefono(...)`... que arman el perfil: así quien
 * * solo necesita un dato suelto no tiene que construir un `Perfil` entero.
 */
@Getter
@Builder(toBuilder = true)
public class Usuario {

    private final UUID codigo;
    private final String email;
    private final String passwordHash;
    private final Perfil perfil;
    // * Escuela (tenant) de la cuenta; null para el Super admin
    private final UUID escuelaCodigo;
    private final EstadoUsuario estado;
    private final int intentosFallidos;
    private final Instant ultimoLogin;
    // * Hasta cuándo está bloqueada la cuenta (login CU02); null si no lo está
    private final Instant bloqueadoHasta;
    // * El correo se verificó con el enlace enviado (CU04b); no bloquea el login, solo la recuperación (CU04)
    private final boolean correoVerificado;
    private final Set<Rol> roles;
    private final Instant creadoEn;
    private final Instant actualizadoEn;

    public String getNombres() {
        return perfil == null ? null : perfil.getNombres();
    }

    public String getApellidos() {
        return perfil == null ? null : perfil.getApellidos();
    }

    public String getTelefono() {
        return perfil == null ? null : perfil.getTelefono();
    }

    public String getFotoUrl() {
        return perfil == null ? null : perfil.getFotoUrl();
    }

    public LocalDate getFechaNacimiento() {
        return perfil == null ? null : perfil.getFechaNacimiento();
    }

    /** Atajos del builder para los datos de la persona; el resto lo completa Lombok. */
    public static class UsuarioBuilder {

        public UsuarioBuilder nombres(String nombres) {
            return conPerfil(p -> p.nombres(nombres));
        }

        public UsuarioBuilder apellidos(String apellidos) {
            return conPerfil(p -> p.apellidos(apellidos));
        }

        public UsuarioBuilder telefono(String telefono) {
            return conPerfil(p -> p.telefono(telefono));
        }

        public UsuarioBuilder fotoUrl(String fotoUrl) {
            return conPerfil(p -> p.fotoUrl(fotoUrl));
        }

        public UsuarioBuilder fechaNacimiento(LocalDate fechaNacimiento) {
            return conPerfil(p -> p.fechaNacimiento(fechaNacimiento));
        }

        public UsuarioBuilder ci(String ci) {
            return conPerfil(p -> p.ci(ci));
        }

        public UsuarioBuilder sexo(SexoPersona sexo) {
            return conPerfil(p -> p.sexo(sexo));
        }

        private UsuarioBuilder conPerfil(UnaryOperator<Perfil.PerfilBuilder> cambio) {
            Perfil.PerfilBuilder base = this.perfil == null ? Perfil.builder() : this.perfil.toBuilder();
            this.perfil = cambio.apply(base).build();
            return this;
        }
    }
}
