package com.drivehub.identidad.infrastructure.adapter.out.persistence.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.drivehub.identidad.domain.model.EstadoUsuario;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cuenta de acceso (CU01, CU02, CU06).
 * * codigo_usuario es el identificador de persona en toda la plataforma.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "usuario")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_usuario"))
public class UsuarioModel extends BaseModel {

    // * Login; solo cambia al confirmar el enlace de cambio de correo (CU06)
    @Setter
    @Column(name = "email", nullable = false, unique = true, length = 120)
    private String email;

    // * BCrypt o Argon2; nunca la contraseña en claro
    @Setter
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Setter
    @Column(name = "nombres", nullable = false, length = 80)
    private String nombres;

    @Setter
    @Column(name = "apellidos", nullable = false, length = 80)
    private String apellidos;

    @Setter
    @Column(name = "telefono", length = 20)
    private String telefono;

    @Setter
    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    // * Escuela (tenant) de la cuenta, sin FK física; null para el Super admin
    @Column(name = "escuela_codigo")
    private UUID escuelaCodigo;

    // * Clave del archivo en el bucket drivehub-identidad
    @Setter
    @Column(name = "foto_url", columnDefinition = "text")
    private String fotoUrl;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 12)
    private EstadoUsuario estado;

    // * Se reinicia en un login correcto; bloquea al superar el límite
    @Setter
    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos;

    @Setter
    @Column(name = "ultimo_login")
    private Instant ultimoLogin;

    // * Fin del bloqueo por intentos fallidos; null si la cuenta no está bloqueada
    @Setter
    @Column(name = "bloqueado_hasta")
    private Instant bloqueadoHasta;

    // * Se marca con el enlace de verificación (CU04b)
    @Setter
    @Column(name = "correo_verificado", nullable = false)
    private boolean correoVerificado;

    public UsuarioModel(String email, String passwordHash, String nombres, String apellidos, String telefono,
            LocalDate fechaNacimiento, UUID escuelaCodigo, EstadoUsuario estado) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.telefono = telefono;
        this.fechaNacimiento = fechaNacimiento;
        this.escuelaCodigo = escuelaCodigo;
        this.estado = estado;
    }
}
