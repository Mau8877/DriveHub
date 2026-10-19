package com.drivehub.identidad.infrastructure.adapter.out.persistence.model;

import java.time.Instant;

import com.drivehub.identidad.domain.model.Dispositivo;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Sesión activa, login único (CU02, CU03).
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "refresh_token")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_refresh_token"))
public class RefreshTokenModel extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioModel usuario;

    // * SHA-256 del token; el token en claro solo lo tiene el cliente
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "dispositivo", nullable = false, length = 10)
    private Dispositivo dispositivo;

    @Column(name = "expira_en", nullable = false)
    private Instant expiraEn;

    // * Se pone en true al renovar (rotación), al iniciar sesión de nuevo en el mismo dispositivo y en CU03
    @Setter
    @Column(name = "revocado", nullable = false)
    private boolean revocado;

    public RefreshTokenModel(UsuarioModel usuario, String tokenHash, Dispositivo dispositivo, Instant expiraEn) {
        this.usuario = usuario;
        this.tokenHash = tokenHash;
        this.dispositivo = dispositivo;
        this.expiraEn = expiraEn;
        this.revocado = false;
    }
}
