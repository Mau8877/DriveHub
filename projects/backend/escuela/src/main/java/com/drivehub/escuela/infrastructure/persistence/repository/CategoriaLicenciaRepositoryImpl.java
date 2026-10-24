package com.drivehub.escuela.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.drivehub.escuela.domain.entity.CategoriaLicencia;
import com.drivehub.escuela.domain.repository.CategoriaLicenciaRepository;
import com.drivehub.escuela.domain.repository.Pagina;
import com.drivehub.escuela.infrastructure.persistence.model.CategoriaLicenciaModel;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class CategoriaLicenciaRepositoryImpl implements CategoriaLicenciaRepository {

    private final CategoriaLicenciaJpaRepository jpaRepository;

    @Override
    public Pagina<CategoriaLicencia> listar(int pagina, int tamano) {
        Page<CategoriaLicenciaModel> resultado = jpaRepository.findAll(
            PageRequest.of(pagina - 1, tamano, Sort.by("id")));
        return new Pagina<>(resultado.getContent().stream().map(this::aDominio).toList(), resultado.getTotalElements());
    }

    @Override
    public Optional<CategoriaLicencia> buscarPorSigla(String sigla) {
        return jpaRepository.findBySiglaIgnoreCase(sigla).map(this::aDominio);
    }

    @Override
    public List<CategoriaLicencia> buscarPorSiglas(Collection<String> siglas) {
        if (siglas == null || siglas.isEmpty()) {
            return List.of();
        }
        return jpaRepository.buscarPorSiglas(siglas.stream().map(s -> s.toUpperCase(Locale.ROOT)).toList())
            .stream().map(this::aDominio).toList();
    }

    private CategoriaLicencia aDominio(CategoriaLicenciaModel model) {
        return CategoriaLicencia.builder().codigo(model.getCodigo()).sigla(model.getSigla())
            .descripcion(model.getDescripcion()).edadMinima(model.getEdadMinima())
            .clasesMinimas(model.getClasesMinimas()).build();
    }
}
