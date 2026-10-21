package com.drivehub.academico.features.horarios.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.support.PageableExecutionUtils;

import com.drivehub.academico.features.horarios.enums.EstadoFranja;
import com.drivehub.academico.features.horarios.model.Franja;

import lombok.RequiredArgsConstructor;

/**
 * Implementación con `MongoTemplate` de las consultas de franjas. Sin lógica de negocio.
 */
@RequiredArgsConstructor
public class FranjaRepositoryImpl implements FranjaRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public Page<Franja> listar(UUID instructorCodigo, LocalDate fecha, LocalDate fechaDesde, EstadoFranja estado,
            Pageable pageable) {
        Criteria criterio = Criteria.where("instructorCodigo").is(instructorCodigo).and("deletedAt").is(null);
        if (fecha != null) {
            criterio = criterio.and("fecha").is(fecha);
        } else if (fechaDesde != null) {
            criterio = criterio.and("fecha").gte(fechaDesde);
        }
        if (estado != null) {
            criterio = criterio.and("estado").is(estado);
        }
        Query consulta = Query.query(criterio);
        List<Franja> pagina = mongoTemplate.find(consulta.with(pageable), Franja.class);
        return PageableExecutionUtils.getPage(pagina, pageable,
            () -> mongoTemplate.count(Query.of(consulta).limit(-1).skip(-1), Franja.class));
    }

    @Override
    public Optional<Franja> cambiarEstado(UUID codigoFranja, UUID instructorCodigo, EstadoFranja esperado,
            EstadoFranja nuevo) {
        Query consulta = Query.query(Criteria.where("codigoFranja").is(codigoFranja)
            .and("instructorCodigo").is(instructorCodigo)
            .and("estado").is(esperado)
            .and("deletedAt").is(null));
        Franja actualizada = mongoTemplate.findAndModify(consulta, new Update().set("estado", nuevo),
            FindAndModifyOptions.options().returnNew(true), Franja.class);
        return Optional.ofNullable(actualizada);
    }

    @Override
    public long eliminarFuturasNoReservadas(Long plantillaId, LocalDate hoy, String horaActual) {
        Criteria futura = new Criteria().orOperator(
            Criteria.where("fecha").gt(hoy),
            Criteria.where("fecha").is(hoy).and("horaInicio").gt(horaActual));
        Query consulta = Query.query(new Criteria().andOperator(
            Criteria.where("plantillaId").is(plantillaId),
            Criteria.where("estado").in(EstadoFranja.LIBRE, EstadoFranja.BLOQUEADA),
            Criteria.where("deletedAt").is(null),
            futura));
        return mongoTemplate.remove(consulta, Franja.class).getDeletedCount();
    }

    @Override
    public boolean insertarSiNoExiste(Franja franja) {
        try {
            mongoTemplate.insert(franja);
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }
}
