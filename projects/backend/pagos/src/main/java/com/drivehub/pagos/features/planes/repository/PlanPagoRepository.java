package com.drivehub.pagos.features.planes.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.drivehub.pagos.features.planes.model.PlanPago;

/**
 * Acceso a los planes de horas (CU25). Sin lógica: todo está aislado por escuela y nunca devuelve el
 * plan de otra.
 *
 * * `BaseModel` ya filtra `deleted_at IS NULL` en todas las consultas (`@SQLRestriction`).
 * * Los planes desactivados SÍ cuentan en las comprobaciones de nombre: el nombre es único por escuela
 * * incluyéndolos (igual que el índice único de la base).
 */
public interface PlanPagoRepository extends JpaRepository<PlanPago, Long>, JpaSpecificationExecutor<PlanPago> {

    /**
     * El plan con ese código dentro de esa escuela. Vacío si no existe o es de otra escuela: no se distinguen.
     */
    Optional<PlanPago> findByCodigoAndEscuelaCodigo(UUID codigo, UUID escuelaCodigo);

    /**
     * Si la escuela ya tiene un plan con ese nombre, sin distinguir mayúsculas. El nombre llega ya recortado.
     */
    boolean existsByEscuelaCodigoAndNombreIgnoreCase(UUID escuelaCodigo, String nombre);

    /**
     * Si la escuela tiene OTRO plan (distinto al de ese código) con ese nombre: sirve para editar sin chocar
     * con el propio nombre.
     */
    boolean existsByEscuelaCodigoAndNombreIgnoreCaseAndCodigoNot(UUID escuelaCodigo, String nombre, UUID codigo);
}
