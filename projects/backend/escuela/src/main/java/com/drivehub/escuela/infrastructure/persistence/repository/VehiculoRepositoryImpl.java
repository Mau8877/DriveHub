package com.drivehub.escuela.infrastructure.persistence.repository;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.drivehub.escuela.common.exception.ConflictException;
import com.drivehub.escuela.common.exception.EscuelaErrorCodes;
import com.drivehub.escuela.domain.entity.Vehiculo;
import com.drivehub.escuela.domain.repository.FiltroVehiculos;
import com.drivehub.escuela.domain.repository.Pagina;
import com.drivehub.escuela.domain.repository.VehiculoRepository;
import com.drivehub.escuela.infrastructure.persistence.model.VehiculoModel;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class VehiculoRepositoryImpl implements VehiculoRepository {

    private final VehiculoJpaRepository jpaRepository;

    @Override
    public boolean existePorPlaca(String placa) {
        return jpaRepository.existsByPlaca(placa);
    }

    @Override
    public Vehiculo guardar(Vehiculo vehiculo) {
        VehiculoModel model = new VehiculoModel(vehiculo.getCodigo(), vehiculo.getEscuelaCodigo(),
            vehiculo.getPlaca(), vehiculo.getMarca(), vehiculo.getModelo(), vehiculo.getTransmision());
        copiarEditables(model, vehiculo);
        try {
            // * saveAndFlush: el UNIQUE de la placa salta acá y no al confirmar la transacción
            return aDominio(jpaRepository.saveAndFlush(model));
        } catch (DataIntegrityViolationException e) {
            // * Carrera entre dos registros con la misma placa: el segundo pierde
            throw new ConflictException(EscuelaErrorCodes.PLACA_EN_USO, "Ya existe un auto con esa placa");
        }
    }

    @Override
    public Optional<Vehiculo> buscarPorCodigo(UUID escuelaCodigo, UUID codigo) {
        return jpaRepository.findByEscuelaCodigoAndCodigo(escuelaCodigo, codigo).map(this::aDominio);
    }

    @Override
    public Pagina<Vehiculo> listar(UUID escuelaCodigo, FiltroVehiculos filtro, int pagina, int tamano) {
        String estado = filtro.estado() == null ? null : filtro.estado().name();
        Page<VehiculoModel> resultado = jpaRepository.listar(
            escuelaCodigo, patronDeBusqueda(filtro.busqueda()), estado, PageRequest.of(pagina - 1, tamano));
        return new Pagina<>(resultado.getContent().stream().map(this::aDominio).toList(), resultado.getTotalElements());
    }

    @Override
    public Optional<Vehiculo> actualizar(Vehiculo vehiculo) {
        return jpaRepository.findByEscuelaCodigoAndCodigo(vehiculo.getEscuelaCodigo(), vehiculo.getCodigo())
            .map(model -> {
                copiarEditables(model, vehiculo);
                return aDominio(jpaRepository.saveAndFlush(model));
            });
    }

    // * La placa, la transmisión y la escuela no se copian: no cambian nunca después de crear el auto
    private void copiarEditables(VehiculoModel model, Vehiculo v) {
        model.setMarca(v.getMarca());
        model.setModelo(v.getModelo());
        model.setAnio(v.getAnio());
        model.setColor(v.getColor());
        model.setKilometraje(v.getKilometraje());
        model.setSoatVence(v.getSoatVence());
        model.setInspeccionVence(v.getInspeccionVence());
        model.setEstado(v.getEstado());
    }

    /**
     * Texto libre a patrón LIKE en minúsculas. Los comodines del texto (`%`, `_`) y la barra
     * se escapan: la persona busca texto, no escribe patrones.
     */
    private String patronDeBusqueda(String busqueda) {
        if (busqueda == null || busqueda.isBlank()) {
            return null;
        }
        String escapado = busqueda.trim().toLowerCase(Locale.ROOT)
            .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escapado + "%";
    }

    // * La categoría de licencia no se gestiona en CU07: el dominio la deja en null
    private Vehiculo aDominio(VehiculoModel model) {
        return Vehiculo.builder()
            .codigo(model.getCodigo())
            .escuelaCodigo(model.getEscuelaCodigo())
            .placa(model.getPlaca())
            .marca(model.getMarca())
            .modelo(model.getModelo())
            .anio(model.getAnio())
            .color(model.getColor())
            .transmision(model.getTransmision())
            .estado(model.getEstado())
            .kilometraje(model.getKilometraje())
            .soatVence(model.getSoatVence())
            .inspeccionVence(model.getInspeccionVence())
            .creadoEn(model.getCreatedAt())
            .build();
    }
}
