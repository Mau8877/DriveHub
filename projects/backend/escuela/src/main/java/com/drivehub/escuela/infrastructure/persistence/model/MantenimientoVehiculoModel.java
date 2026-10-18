package com.drivehub.escuela.infrastructure.persistence.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.drivehub.escuela.domain.entity.TipoMantenimiento;

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
 * Historial de mantenimiento de un vehículo (CU07).
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "mantenimiento_vehiculo")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_mantenimiento_vehiculo"))
public class MantenimientoVehiculoModel extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehiculo_id", nullable = false)
    private VehiculoModel vehiculo;

    @Setter
    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 12)
    private TipoMantenimiento tipo;

    @Setter
    @Column(name = "descripcion", columnDefinition = "text")
    private String descripcion;

    @Setter
    @Column(name = "costo", precision = 10, scale = 2)
    private BigDecimal costo;

    @Setter
    @Column(name = "km_al_momento")
    private Integer kmAlMomento;
}
