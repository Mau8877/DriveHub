package com.drivehub.escuela.infrastructure.persistence.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.drivehub.escuela.domain.entity.Escuela;
import com.drivehub.escuela.domain.entity.EstadoEscuela;
import com.drivehub.escuela.domain.repository.EscuelaRepository;
import com.drivehub.escuela.infrastructure.persistence.model.EscuelaModel;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class EscuelaRepositoryImpl implements EscuelaRepository {

    private final EscuelaJpaRepository jpaRepository;

    @Override
    public Optional<Escuela> buscarActivaPorSlug(String slug) {
        return jpaRepository.findBySlugAndEstado(slug, EstadoEscuela.ACTIVA).map(this::aDominio);
    }

    private Escuela aDominio(EscuelaModel model) {
        return Escuela.builder()
            .codigo(model.getCodigo())
            .slug(model.getSlug())
            .nombre(model.getNombre())
            .estado(model.getEstado())
            .creadoEn(model.getCreatedAt())
            .actualizadoEn(model.getUpdatedAt())
            .build();
    }
}
