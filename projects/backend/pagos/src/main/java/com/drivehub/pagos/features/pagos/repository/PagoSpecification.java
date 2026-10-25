package com.drivehub.pagos.features.pagos.repository;

import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.drivehub.pagos.features.pagos.enums.ConceptoPago;
import com.drivehub.pagos.features.pagos.enums.EstadoPago;
import com.drivehub.pagos.features.pagos.enums.EstadoPagoVisible;
import com.drivehub.pagos.features.pagos.enums.EstadoReversion;
import com.drivehub.pagos.features.pagos.model.Pago;
import com.drivehub.pagos.features.pagos.model.ReversionPago;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

/**
 * Los filtros del historial de pagos del estudiante (CU28). Se combinan con `Specification.allOf`: el de la
 * persona y su escuela va siempre.
 *
 * * El orden NO está acá: lo define quien consulta, con un `Sort`.
 */
public final class PagoSpecification {

    private PagoSpecification() {
    }

    /** Solo los pagos de ese estudiante en esa escuela (siempre los dos: nadie ve el pago de otro). */
    public static Specification<Pago> deEstudiante(UUID escuelaCodigo, UUID estudianteCodigo) {
        return (pagos, consulta, cb) -> cb.and(cb.equal(pagos.get("escuelaCodigo"), escuelaCodigo),
            cb.equal(pagos.get("estudianteCodigo"), estudianteCodigo));
    }

    public static Specification<Pago> deConcepto(ConceptoPago concepto) {
        return (pagos, consulta, cb) -> cb.equal(pagos.get("concepto"), concepto);
    }

    /**
     * Los pagos que el estudiante ve en ese estado. Nulo no filtra. `REEMBOLSO_EN_PROCESO` es un pago confirmado
     * con una reversión solicitada; `CONFIRMADO` excluye esos casos.
     */
    public static Specification<Pago> conEstadoVisible(EstadoPagoVisible visible) {
        if (visible == null) {
            return Specification.unrestricted();
        }
        return (pagos, consulta, cb) -> switch (visible) {
            case PENDIENTE -> cb.equal(pagos.get("estado"), EstadoPago.PENDIENTE);
            case FALLIDO -> cb.equal(pagos.get("estado"), EstadoPago.FALLIDO);
            case REEMBOLSADO -> cb.equal(pagos.get("estado"), EstadoPago.REVERTIDO);
            case REEMBOLSO_EN_PROCESO -> cb.and(cb.equal(pagos.get("estado"), EstadoPago.CONFIRMADO),
                conReversionSolicitada(pagos, consulta.subquery(Long.class), cb));
            case CONFIRMADO -> cb.and(cb.equal(pagos.get("estado"), EstadoPago.CONFIRMADO),
                conReversionSolicitada(pagos, consulta.subquery(Long.class), cb).not());
        };
    }

    private static Predicate conReversionSolicitada(Root<Pago> pagos, Subquery<Long> subconsulta,
            CriteriaBuilder cb) {
        Root<ReversionPago> reversion = subconsulta.from(ReversionPago.class);
        subconsulta.select(reversion.get("id")).where(cb.equal(reversion.get("pago"), pagos),
            cb.equal(reversion.get("estado"), EstadoReversion.SOLICITADA));
        return cb.exists(subconsulta);
    }
}
