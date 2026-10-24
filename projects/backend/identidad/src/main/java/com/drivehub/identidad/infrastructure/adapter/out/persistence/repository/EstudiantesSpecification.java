package com.drivehub.identidad.infrastructure.adapter.out.persistence.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.drivehub.identidad.domain.model.EstadoFiltroEstudiante;
import com.drivehub.identidad.domain.model.EstadoUsuario;
import com.drivehub.identidad.domain.model.RolNombres;
import com.drivehub.identidad.domain.port.out.FiltroEstudiantes;
import com.drivehub.identidad.infrastructure.adapter.out.persistence.model.PerfilModel;
import com.drivehub.identidad.infrastructure.adapter.out.persistence.model.UsuarioModel;
import com.drivehub.identidad.infrastructure.adapter.out.persistence.model.UsuarioRolModel;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

/**
 * Condiciones de la consulta de estudiantes de una escuela (CU08), armadas con Criteria para que
 * el AND de N palabras de la búsqueda no se escriba concatenando texto SQL.
 */
final class EstudiantesSpecification {

    private static final char ESCAPE = '\\';

    private EstudiantesSpecification() {
    }

    /** Las cuentas ESTUDIANTE de esa escuela que cumplen el filtro, con su perfil en la misma consulta. */
    static Specification<UsuarioModel> deEscuela(UUID escuelaCodigo, FiltroEstudiantes filtro) {
        return (root, query, cb) -> {
            Join<UsuarioModel, PerfilModel> perfil = perfilDe(root, query.getResultType());

            List<Predicate> condiciones = new ArrayList<>();
            condiciones.add(cb.equal(root.get("escuelaCodigo"), escuelaCodigo));

            Subquery<Long> esEstudiante = query.subquery(Long.class);
            Root<UsuarioRolModel> usuarioRol = esEstudiante.from(UsuarioRolModel.class);
            esEstudiante.select(usuarioRol.<Long>get("id")).where(
                cb.equal(usuarioRol.get("usuario").get("id"), root.get("id")),
                cb.equal(usuarioRol.get("rol").get("nombre"), RolNombres.ESTUDIANTE));
            condiciones.add(cb.exists(esEstudiante));

            if (filtro.estado() == EstadoFiltroEstudiante.ACTIVO) {
                condiciones.add(root.get("estado").in(EstadoUsuario.ACTIVO, EstadoUsuario.BLOQUEADO));
            } else if (filtro.estado() == EstadoFiltroEstudiante.INACTIVO) {
                condiciones.add(cb.equal(root.get("estado"), EstadoUsuario.INACTIVO));
            }

            if (filtro.busqueda() != null && !filtro.busqueda().isBlank()) {
                Expression<String> nombreCompleto = cb.function("unaccent", String.class, cb.lower(
                    cb.concat(cb.concat(perfil.<String>get("nombres"), " "), perfil.<String>get("apellidos"))));
                for (String palabra : filtro.busqueda().trim().split("\\s+")) {
                    Expression<String> patron = cb.function("unaccent", String.class,
                        cb.lower(cb.literal("%" + escapar(palabra) + "%")));
                    condiciones.add(cb.like(nombreCompleto, patron, ESCAPE));
                }
            }
            return cb.and(condiciones.toArray(Predicate[]::new));
        };
    }

    /** La cuenta con ese código; se combina con {@link #deEscuela} para no salir de la escuela. */
    static Specification<UsuarioModel> conCodigo(UUID codigo) {
        return (root, query, cb) -> cb.equal(root.get("codigo"), codigo);
    }

    // * En la consulta de datos el perfil viaja con un JOIN FETCH (sin N+1); en la de conteo, un JOIN común
    @SuppressWarnings("unchecked")
    private static Join<UsuarioModel, PerfilModel> perfilDe(Root<UsuarioModel> root, Class<?> tipoDeResultado) {
        boolean esConteo = Long.class.equals(tipoDeResultado) || long.class.equals(tipoDeResultado);
        if (esConteo) {
            return root.join("perfil", JoinType.INNER);
        }
        return (Join<UsuarioModel, PerfilModel>) root.<UsuarioModel, PerfilModel>fetch("perfil", JoinType.INNER);
    }

    /** `%`, `_` y la propia barra de escape del texto del usuario se buscan tal cual, no como comodines. */
    private static String escapar(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
