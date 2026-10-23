package com.drivehub.identidad.infrastructure.adapter.out.persistence.model;

import java.time.Instant;
import java.util.UUID;

import com.drivehub.identidad.domain.model.EstadoUsuario;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cuenta de acceso (CU01, CU02, CU06). Los datos de la persona están en `PerfilModel` (CU08a).
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

    // * Lado inverso: el dueño de la relación es PerfilModel (perfil.usuario_id)
    @OneToOne(mappedBy = "usuario", fetch = FetchType.LAZY)
    private PerfilModel perfil;

    // * Escuela (tenant) de la cuenta, sin FK física; null para el Super admin
    @Column(name = "escuela_codigo")
    private UUID escuelaCodigo;

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

    public UsuarioModel(String email, String passwordHash, UUID escuelaCodigo, EstadoUsuario estado) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.escuelaCodigo = escuelaCodigo;
        this.estado = estado;
    }
}
