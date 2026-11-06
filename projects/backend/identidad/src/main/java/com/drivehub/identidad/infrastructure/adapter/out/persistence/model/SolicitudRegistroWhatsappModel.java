package com.drivehub.identidad.infrastructure.adapter.out.persistence.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.drivehub.identidad.domain.model.SexoPersona;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Datos de un registro por WhatsApp pendiente de la contraseña (CU48). */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "solicitud_registro_whatsapp")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_solicitud_registro_whatsapp"))
public class SolicitudRegistroWhatsappModel extends BaseModel {

    @Column(name = "telefono", nullable = false, length = 20)
    private String telefono;

    @Column(name = "nombres", nullable = false, length = 80)
    private String nombres;

    @Column(name = "apellidos", nullable = false, length = 80)
    private String apellidos;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @Enumerated(EnumType.STRING)
    @Column(name = "sexo", nullable = false, length = 10)
    private SexoPersona sexo;

    @Column(name = "ci", nullable = false, length = 15)
    private String ci;

    @Column(name = "correo", nullable = false, length = 120)
    private String correo;

    @Column(name = "latitud", nullable = false, precision = 9, scale = 6)
    private BigDecimal latitud;

    @Column(name = "longitud", nullable = false, precision = 9, scale = 6)
    private BigDecimal longitud;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expira_en", nullable = false)
    private Instant expiraEn;

    public SolicitudRegistroWhatsappModel(String telefono, String nombres, String apellidos,
            LocalDate fechaNacimiento, SexoPersona sexo, String ci, String correo, BigDecimal latitud,
            BigDecimal longitud, String tokenHash, Instant expiraEn) {
        this.telefono = telefono;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.fechaNacimiento = fechaNacimiento;
        this.sexo = sexo;
        this.ci = ci;
        this.correo = correo;
        this.latitud = latitud;
        this.longitud = longitud;
        this.tokenHash = tokenHash;
        this.expiraEn = expiraEn;
    }
}
