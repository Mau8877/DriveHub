package com.drivehub.academico.features.simulacros.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;

import com.drivehub.academico.features.simulacros.model.SimulacroConfig;

import lombok.RequiredArgsConstructor;

/**
 * Spring Data la une a SimulacroConfigRepository por el sufijo `Impl`.
 *
 * ! MongoDB no filtra el soft delete solo: `deletedAt` se pregunta acá explícitamente.
 */
@RequiredArgsConstructor
public class SimulacroConfigRepositoryCustomImpl implements SimulacroConfigRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public Page<SimulacroConfig> listar(UUID escuelaCodigo, FiltroSimulacros filtro, Pageable pageable) {
        List<Criteria> condiciones = new ArrayList<>();
        condiciones.add(Criteria.where("escuelaCodigo").is(escuelaCodigo));
        condiciones.add(Criteria.where("deletedAt").is(null));
        if (filtro.categoria() != null && !filtro.categoria().isBlank()) {
            condiciones.add(Criteria.where("categoria").is(filtro.categoria()));
        }
        if (filtro.activo() != null) {
            condiciones.add(Criteria.where("activo").is(filtro.activo()));
        }
        if (filtro.busqueda() != null && !filtro.busqueda().isBlank()) {
            condiciones.add(Criteria.where("nombre").regex(Pattern.quote(filtro.busqueda().trim()), "i"));
        }

        Query consulta = new Query(new Criteria().andOperator(condiciones));
        List<SimulacroConfig> pagina = mongoTemplate.find(
            Query.of(consulta).with(pageable).with(Sort.by(Sort.Direction.DESC, "createdAt", "id")),
            SimulacroConfig.class);
        return PageableExecutionUtils.getPage(pagina, pageable,
            () -> mongoTemplate.count(Query.of(consulta).limit(-1).skip(-1), SimulacroConfig.class));
    }
}
