package com.drivehub.identidad.infrastructure.adapter.out.persistence.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.drivehub.identidad.domain.model.SexoPersona;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Perfil de la persona (CU08a), 1 a 1 con la cuenta.
 * * codigo_perfil no sale del servicio: la persona se identifica por el codigo_usuario.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "perfil")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_perfil"))
public class PerfilModel extends BaseModel {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, updatable = false)
    private UsuarioModel usuario;

    @Setter
    @Column(name = "nombres", nullable = false, length = 80)
    private String nombres;

    @Setter
    @Column(name = "apellidos", nullable = false, length = 80)
    private String apellidos;

    @Setter
    @Column(name = "telefono", length = 20)
    private String telefono;

    // * Clave del archivo en el bucket drivehub-identidad
    @Setter
    @Column(name = "foto_url", columnDefinition = "text")
    private String fotoUrl;

    @Setter
    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @Setter
    @Column(name = "ci", nullable = false, length = 15)
    private String ci;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "sexo", nullable = false, length = 10)
    private SexoPersona sexo;

    @Setter
    @Column(name = "direccion", length = 300)
    private String direccion;

    @Setter
    @Column(name = "latitud", precision = 9, scale = 6)
    private BigDecimal latitud;

    @Setter
    @Column(name = "longitud", precision = 9, scale = 6)
    private BigDecimal longitud;

    // * Referencia por código a escuela.categoria_licencia.sigla (sin FK); solo estudiantes
    @Setter
    @Column(name = "categoria_sigla", length = 3)
    private String categoriaSigla;

    public PerfilModel(UsuarioModel usuario, String nombres, String apellidos, String telefono, String fotoUrl,
            LocalDate fechaNacimiento, String ci, SexoPersona sexo, String direccion, BigDecimal latitud,
            BigDecimal longitud, String categoriaSigla) {
        this.usuario = usuario;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.telefono = telefono;
        this.fotoUrl = fotoUrl;
        this.fechaNacimiento = fechaNacimiento;
        this.ci = ci;
        this.sexo = sexo;
        this.direccion = direccion;
        this.latitud = latitud;
        this.longitud = longitud;
        this.categoriaSigla = categoriaSigla;
    }
}
