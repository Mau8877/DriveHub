package com.drivehub.identidad.infrastructure.adapter.out.persistence.repository;

import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.drivehub.identidad.domain.model.Rol;
import com.drivehub.identidad.domain.port.out.RolRepositoryPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RolPersistenceAdapter implements RolRepositoryPort {

    private final RolJpaRepository rolJpaRepository;

    @Override
    public Optional<Rol> buscarPorNombre(String nombre) {
        return rolJpaRepository.findByNombre(nombre).map(model -> Rol.builder()
            .codigo(model.getCodigo())
            .nombre(model.getNombre())
            .descripcion(model.getDescripcion())
            .permisos(Set.of())
            .build());
    }
}
