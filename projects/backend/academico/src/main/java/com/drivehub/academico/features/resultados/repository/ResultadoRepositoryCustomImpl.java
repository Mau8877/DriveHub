package com.drivehub.academico.features.resultados.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;

import com.drivehub.academico.features.resultados.model.Resultado;

import lombok.RequiredArgsConstructor;

/**
 * Spring Data la une a ResultadoRepository por el sufijo `Impl`.
 *
 * ! MongoDB no filtra el soft delete solo: `deletedAt` se pregunta acá explícitamente.
 */
@RequiredArgsConstructor
public class ResultadoRepositoryCustomImpl implements ResultadoRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public Page<Resultado> listarDeEscuela(UUID escuelaCodigo, FiltroResultados filtro, Pageable pageable) {
        return listar(Criteria.where("escuelaCodigo").is(escuelaCodigo), filtro, pageable);
    }

    @Override
    public Page<Resultado> listarDeEstudiante(UUID estudianteCodigo, FiltroResultados filtro, Pageable pageable) {
        return listar(Criteria.where("estudianteCodigo").is(estudianteCodigo), filtro, pageable);
    }

    @Override
    public Page<Resultado> listarDeEstudiantes(Collection<UUID> estudiantes, FiltroResultados filtro,
            Pageable pageable) {
        return listar(Criteria.where("estudianteCodigo").in(estudiantes), filtro, pageable);
    }

    private Page<Resultado> listar(Criteria alcance, FiltroResultados filtro, Pageable pageable) {
        List<Criteria> condiciones = new ArrayList<>();
        condiciones.add(alcance);
        condiciones.add(Criteria.where("deletedAt").is(null));
        if (filtro.origen() != null) {
            condiciones.add(Criteria.where("origen").is(filtro.origen()));
        }
        if (filtro.estudianteCodigo() != null) {
            condiciones.add(Criteria.where("estudianteCodigo").is(filtro.estudianteCodigo()));
        }

        Query consulta = new Query(new Criteria().andOperator(condiciones));
        List<Resultado> pagina = mongoTemplate.find(
            Query.of(consulta).with(pageable).with(Sort.by(Sort.Direction.DESC, "fecha", "id")), Resultado.class);
        return PageableExecutionUtils.getPage(pagina, pageable,
            () -> mongoTemplate.count(Query.of(consulta).limit(-1).skip(-1), Resultado.class));
    }
}
