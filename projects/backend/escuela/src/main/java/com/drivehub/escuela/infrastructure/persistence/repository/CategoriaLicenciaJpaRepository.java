package com.drivehub.escuela.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.drivehub.escuela.infrastructure.persistence.model.CategoriaLicenciaModel;

public interface CategoriaLicenciaJpaRepository extends JpaRepository<CategoriaLicenciaModel, Long> {

    Optional<CategoriaLicenciaModel> findBySiglaIgnoreCase(String sigla);

    // * Las siglas llegan en mayúsculas; el catálogo las guarda así
    @Query("select c from CategoriaLicenciaModel c where upper(c.sigla) in :siglas order by c.id")
    List<CategoriaLicenciaModel> buscarPorSiglas(@Param("siglas") Collection<String> siglas);
}
