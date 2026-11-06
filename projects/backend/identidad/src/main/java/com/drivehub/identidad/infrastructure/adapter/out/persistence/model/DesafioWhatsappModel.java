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

/** Código de 6 dígitos enviado al correo para vincular un número (CU47). Solo se guarda su hash. */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "desafio_whatsapp")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_desafio_whatsapp"))
public class DesafioWhatsappModel extends BaseModel {

    // * Nulo cuando el correo no tenía una cuenta válida (pedido descartado que cuenta para el límite)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private UsuarioModel usuario;

    @Column(name = "telefono_hash", nullable = false, length = 64)
    private String telefonoHash;

    @Column(name = "correo_hash", nullable = false, length = 64)
    private String correoHash;

    @Column(name = "codigo_hash", nullable = false, length = 64)
    private String codigoHash;

    @Setter
    @Column(name = "intentos", nullable = false)
    private int intentos;

    @Column(name = "expira_en", nullable = false)
    private Instant expiraEn;

    @Setter
    @Column(name = "usado", nullable = false)
    private boolean usado;

    public DesafioWhatsappModel(UsuarioModel usuario, String telefonoHash, String correoHash, String codigoHash,
            Instant expiraEn, boolean usado) {
        this.usuario = usuario;
        this.telefonoHash = telefonoHash;
        this.correoHash = correoHash;
        this.codigoHash = codigoHash;
        this.expiraEn = expiraEn;
        this.usado = usado;
    }
}
