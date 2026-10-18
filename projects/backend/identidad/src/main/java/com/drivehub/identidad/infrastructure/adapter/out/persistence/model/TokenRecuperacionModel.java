package com.drivehub.identidad.infrastructure.adapter.out.persistence.model;

import java.time.Instant;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Token para restablecer la contraseña (CU04).
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "token_recuperacion")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_token_recuperacion"))
public class TokenRecuperacionModel extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioModel usuario;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    // * Vigencia corta (ej. 30 min)
    @Column(name = "expira_en", nullable = false)
    private Instant expiraEn;

    // * Un solo uso
    @Setter
    @Column(name = "usado", nullable = false)
    private boolean usado;
}
