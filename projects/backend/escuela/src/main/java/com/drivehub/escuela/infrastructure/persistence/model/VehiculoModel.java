package com.drivehub.escuela.infrastructure.persistence.model;

import java.time.LocalDate;
import java.util.UUID;

import com.drivehub.escuela.domain.entity.EstadoVehiculo;
import com.drivehub.escuela.domain.entity.Transmision;

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
 * Auto de la escuela (CU07).
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "vehiculo")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_vehiculo"))
public class VehiculoModel extends BaseModel {

    // * Escuela (tenant) dueña del auto: es el claim `escuela` del JWT. Sin FK, es de otro dominio
    @Column(name = "escuela_codigo", nullable = false, updatable = false)
    private UUID escuelaCodigo;

    @Setter
    @Column(name = "placa", nullable = false, unique = true, length = 10)
    private String placa;

    @Setter
    @Column(name = "marca", nullable = false, length = 40)
    private String marca;

    @Setter
    @Column(name = "modelo", nullable = false, length = 40)
    private String modelo;

    @Setter
    @Column(name = "anio")
    private Short anio;

    @Setter
    @Column(name = "color", length = 20)
    private String color;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "transmision", nullable = false, length = 10)
    private Transmision transmision;

    // * Categoría en la que se usa
    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private CategoriaLicenciaModel categoria;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 14)
    private EstadoVehiculo estado;

    // * Se actualiza al cerrar cada clase
    @Setter
    @Column(name = "kilometraje", nullable = false)
    private int kilometraje;

    @Setter
    @Column(name = "soat_vence")
    private LocalDate soatVence;

    @Setter
    @Column(name = "inspeccion_vence")
    private LocalDate inspeccionVence;

    public VehiculoModel(UUID codigo, UUID escuelaCodigo, String placa, String marca, String modelo,
            Transmision transmision) {
        this.escuelaCodigo = escuelaCodigo;
        this.placa = placa;
        this.marca = marca;
        this.modelo = modelo;
        this.transmision = transmision;
        this.estado = EstadoVehiculo.DISPONIBLE;
        asignarCodigo(codigo);
    }
}
