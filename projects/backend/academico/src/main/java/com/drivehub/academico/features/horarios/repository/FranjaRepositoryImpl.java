package com.drivehub.academico.features.horarios.repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    public Page<Franja> listar(UUID instructorCodigo, LocalDate fechaDesde, LocalDate fechaHasta,
            EstadoFranja estado, Pageable pageable) {
        Criteria criterio = Criteria.where("instructorCodigo").is(instructorCodigo).and("deletedAt").is(null);
        if (fechaDesde != null && fechaHasta != null) {
            criterio = criterio.and("fecha").gte(fechaDesde).lte(fechaHasta);
        } else if (fechaDesde != null) {
            criterio = criterio.and("fecha").gte(fechaDesde);
        } else if (fechaHasta != null) {
            criterio = criterio.and("fecha").lte(fechaHasta);
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
    public long cambiarEstadoDelDia(UUID instructorCodigo, LocalDate fecha, EstadoFranja esperado,
            EstadoFranja nuevo, String horaMinima) {
        Criteria criterio = Criteria.where("instructorCodigo").is(instructorCodigo)
            .and("fecha").is(fecha)
            .and("estado").is(esperado)
            .and("deletedAt").is(null);
        if (horaMinima != null) {
            criterio = criterio.and("horaInicio").gt(horaMinima);
        }
        return mongoTemplate.updateMulti(Query.query(criterio), new Update().set("estado", nuevo), Franja.class)
            .getModifiedCount();
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

    @Override
    public Page<Franja> disponibles(UUID escuelaCodigo, LocalDate hoy, String ahora, LocalDate fechaDesde,
            LocalDate fechaHasta, UUID instructorCodigo, String horaDesde, String horaHasta, Pageable pageable) {
        List<Criteria> condiciones = condicionesDisponibles(escuelaCodigo, hoy, ahora, fechaDesde, fechaHasta);
        if (instructorCodigo != null) {
            condiciones.add(Criteria.where("instructorCodigo").is(instructorCodigo));
        }
        if (horaDesde != null && horaHasta != null) {
            condiciones.add(Criteria.where("horaInicio").gte(horaDesde).lt(horaHasta));
        } else if (horaDesde != null) {
            condiciones.add(Criteria.where("horaInicio").gte(horaDesde));
        } else if (horaHasta != null) {
            condiciones.add(Criteria.where("horaInicio").lt(horaHasta));
        }
        Pageable ordenada = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
            Sort.by("fecha", "horaInicio", "instructorCodigo"));
        Query consulta = Query.query(new Criteria().andOperator(condiciones));
        List<Franja> pagina = mongoTemplate.find(Query.of(consulta).with(ordenada), Franja.class);
        return PageableExecutionUtils.getPage(pagina, ordenada,
            () -> mongoTemplate.count(Query.of(consulta).limit(-1).skip(-1), Franja.class));
    }

    @Override
    public List<UUID> instructoresConDisponibles(UUID escuelaCodigo, LocalDate hoy, String ahora,
            LocalDate fechaDesde, LocalDate fechaHasta) {
        Query consulta = Query.query(new Criteria().andOperator(
            condicionesDisponibles(escuelaCodigo, hoy, ahora, fechaDesde, fechaHasta)));
        return mongoTemplate.findDistinct(consulta, "instructorCodigo", Franja.class, UUID.class);
    }

    @Override
    public Optional<Franja> apartar(UUID codigoFranja, UUID escuelaCodigo, Long reservaId, LocalDate hoy,
            String ahora) {
        Query consulta = Query.query(new Criteria().andOperator(
            Criteria.where("codigoFranja").is(codigoFranja),
            Criteria.where("escuelaCodigo").is(escuelaCodigo),
            Criteria.where("estado").is(EstadoFranja.LIBRE),
            Criteria.where("deletedAt").is(null),
            noEmpezo(hoy, ahora)));
        Franja apartada = mongoTemplate.findAndModify(consulta,
            new Update().set("estado", EstadoFranja.RESERVADA).set("reservaId", reservaId),
            FindAndModifyOptions.options().returnNew(true), Franja.class);
        return Optional.ofNullable(apartada);
    }

    @Override
    public boolean liberar(UUID codigoFranja, Long reservaId) {
        Query consulta = Query.query(Criteria.where("codigoFranja").is(codigoFranja)
            .and("estado").is(EstadoFranja.RESERVADA)
            .and("reservaId").is(reservaId)
            .and("deletedAt").is(null));
        return mongoTemplate.updateFirst(consulta,
            new Update().set("estado", EstadoFranja.LIBRE).unset("reservaId"), Franja.class)
            .getModifiedCount() > 0;
    }

    /** Escuela, `LIBRE`, sin borrar, que no empezó y dentro del rango de fechas (si lo hay). */
    private static List<Criteria> condicionesDisponibles(UUID escuelaCodigo, LocalDate hoy, String ahora,
            LocalDate fechaDesde, LocalDate fechaHasta) {
        List<Criteria> condiciones = new ArrayList<>();
        condiciones.add(Criteria.where("escuelaCodigo").is(escuelaCodigo));
        condiciones.add(Criteria.where("estado").is(EstadoFranja.LIBRE));
        condiciones.add(Criteria.where("deletedAt").is(null));
        condiciones.add(noEmpezo(hoy, ahora));
        if (fechaDesde != null && fechaHasta != null) {
            condiciones.add(Criteria.where("fecha").gte(fechaDesde).lte(fechaHasta));
        } else if (fechaDesde != null) {
            condiciones.add(Criteria.where("fecha").gte(fechaDesde));
        } else if (fechaHasta != null) {
            condiciones.add(Criteria.where("fecha").lte(fechaHasta));
        }
        return condiciones;
    }

    /** La franja todavía no empezó: es de un día posterior a hoy, o de hoy con hora de inicio posterior a ahora. */
    private static Criteria noEmpezo(LocalDate hoy, String ahora) {
        return new Criteria().orOperator(
            Criteria.where("fecha").gt(hoy),
            Criteria.where("fecha").is(hoy).and("horaInicio").gt(ahora));
    }
}
