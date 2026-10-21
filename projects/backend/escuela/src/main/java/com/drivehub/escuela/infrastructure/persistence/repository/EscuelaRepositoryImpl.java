package com.drivehub.escuela.infrastructure.persistence.repository;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.escuela.common.exception.ConflictException;
import com.drivehub.escuela.common.exception.EscuelaErrorCodes;
import com.drivehub.escuela.domain.entity.Escuela;
import com.drivehub.escuela.domain.repository.EscuelaRepository;
import com.drivehub.escuela.domain.repository.FiltroEscuelas;
import com.drivehub.escuela.domain.repository.Pagina;
import com.drivehub.escuela.infrastructure.persistence.model.EscuelaModel;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class EscuelaRepositoryImpl implements EscuelaRepository {

    private final EscuelaJpaRepository jpaRepository;

    @Override
    public Optional<Escuela> buscarActivaPorSlug(String slug) {
        return jpaRepository.findBySlug(slug).map(this::aDominio);
    }

    @Override
    public Optional<Escuela> buscarPorCodigo(UUID codigo) {
        return jpaRepository.buscarPorCodigoIncluyendoEliminadas(codigo).map(this::aDominio);
    }

    @Override
    public Escuela guardar(Escuela escuela) {
        try {
            // * saveAndFlush: el UNIQUE del slug salta acá y no al confirmar la transacción
            return aDominio(jpaRepository.saveAndFlush(
                new EscuelaModel(escuela.getCodigo(), escuela.getSlug(), escuela.getNombre())));
        } catch (DataIntegrityViolationException e) {
            // * Carrera entre dos creaciones con el mismo slug: la segunda pierde
            throw new ConflictException(EscuelaErrorCodes.SLUG_EN_USO, "Ya existe una escuela con ese slug");
        }
    }

    @Override
    public boolean existePorSlug(String slug) {
        return jpaRepository.existePorSlugIncluyendoEliminadas(slug);
    }

    @Override
    public Pagina<Escuela> listar(FiltroEscuelas filtro, int pagina, int tamano) {
        String estado = filtro.estado() == null ? null : filtro.estado().name();
        Page<EscuelaModel> resultado = jpaRepository.listar(
            patronDeBusqueda(filtro.busqueda()), estado, PageRequest.of(pagina - 1, tamano));
        return new Pagina<>(resultado.getContent().stream().map(this::aDominio).toList(), resultado.getTotalElements());
    }

    @Override
    public Optional<Escuela> actualizarNombre(UUID codigo, String nombre) {
        return jpaRepository.findByCodigo(codigo).map(model -> {
            model.setNombre(nombre);
            return aDominio(jpaRepository.saveAndFlush(model));
        });
    }

    @Override
    @Transactional
    public boolean desactivar(UUID codigo) {
        return jpaRepository.desactivar(codigo) > 0;
    }

    @Override
    @Transactional
    public boolean reactivar(UUID codigo) {
        return jpaRepository.reactivar(codigo) > 0;
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

    private Escuela aDominio(EscuelaModel model) {
        return Escuela.builder()
            .codigo(model.getCodigo())
            .slug(model.getSlug())
            .nombre(model.getNombre())
            .eliminadaEn(model.getDeletedAt())
            .creadoEn(model.getCreatedAt())
            .actualizadoEn(model.getUpdatedAt())
            .build();
    }
}
