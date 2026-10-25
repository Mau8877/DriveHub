package com.drivehub.pagos.features.planes.repository;

import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.drivehub.pagos.features.planes.model.PlanPago;

/**
 * Los filtros del listado de planes (CU25). Se combinan con `Specification.allOf`: el de la escuela va
 * siempre; los otros solo si la persona los pidió.
 *
 * * El orden NO está acá: lo define quien consulta, con un `Sort`.
 */
public final class PlanPagoSpecification {

    private static final char ESCAPE = '\\';

    private PlanPagoSpecification() {
    }

    /** Solo los planes de esa escuela (siempre presente: nunca se lista sin tenant). */
    public static Specification<PlanPago> deLaEscuela(UUID escuelaCodigo) {
        return (planes, consulta, cb) -> cb.equal(planes.get("escuelaCodigo"), escuelaCodigo);
    }

    /**
     * El nombre contiene el texto, sin distinguir mayúsculas. Un texto nulo o en blanco no filtra. `%`, `_` y la
     * barra invertida se escapan para que valgan como letras (no como comodines).
     */
    public static Specification<PlanPago> nombreContiene(String texto) {
        if (texto == null || texto.isBlank()) {
            return Specification.unrestricted();
        }
        String patron = "%" + escapar(texto.trim().toLowerCase()) + "%";
        return (planes, consulta, cb) -> cb.like(cb.lower(planes.get("nombre")), patron, ESCAPE);
    }

    /** Solo los activos (`true`) o solo los desactivados (`false`); nulo trae ambos. */
    public static Specification<PlanPago> conEstado(Boolean activo) {
        if (activo == null) {
            return Specification.unrestricted();
        }
        return (planes, consulta, cb) -> cb.equal(planes.get("activo"), activo);
    }

    private static String escapar(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
