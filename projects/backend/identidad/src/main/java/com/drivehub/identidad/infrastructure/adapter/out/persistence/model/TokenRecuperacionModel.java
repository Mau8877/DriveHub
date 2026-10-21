package com.drivehub.identidad.infrastructure.adapter.out.persistence.model;

import java.time.Instant;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import com.drivehub.identidad.domain.model.TipoTokenCorreo;

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
 * Enlace por correo de un solo uso: restablecer la contraseña (CU04) o verificar el correo (CU04b).
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

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoTokenCorreo tipo;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    // * Vigencia corta (ej. 30 min)
    @Column(name = "expira_en", nullable = false)
    private Instant expiraEn;

    // * Un solo uso
    @Setter
    @Column(name = "usado", nullable = false)
    private boolean usado;

    public TokenRecuperacionModel(UsuarioModel usuario, TipoTokenCorreo tipo, String tokenHash, Instant expiraEn) {
        this.usuario = usuario;
        this.tipo = tipo;
        this.tokenHash = tokenHash;
        this.expiraEn = expiraEn;
    }
}
