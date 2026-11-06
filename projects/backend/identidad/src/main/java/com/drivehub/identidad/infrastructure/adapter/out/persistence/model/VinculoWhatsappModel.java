package com.drivehub.identidad.infrastructure.adapter.out.persistence.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Número de WhatsApp (hasheado) de un estudiante (CU47). */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "vinculo_whatsapp")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_vinculo_whatsapp"))
public class VinculoWhatsappModel extends BaseModel {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private UsuarioModel usuario;

    @Column(name = "telefono_hash", nullable = false, unique = true, length = 64)
    private String telefonoHash;

    public VinculoWhatsappModel(UsuarioModel usuario, String telefonoHash) {
        this.usuario = usuario;
        this.telefonoHash = telefonoHash;
    }
}
