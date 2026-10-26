package com.drivehub.escuela.domain.repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import com.drivehub.escuela.domain.entity.AsignacionVehiculo;

/**
 * Lo que el negocio necesita saber de qué instructor usa cada auto (CU10). Todo se consulta dentro
 * de una escuela: un auto o un instructor de otra escuela es, para quien consulta, uno que no existe.
 *
 * * Un auto tiene una sola asignación vigente (sin fecha de fin); la base lo garantiza con un índice
 * * único parcial.
 */
public interface AsignacionVehiculoRepository {

    Optional<AsignacionVehiculo> buscarVigente(UUID escuelaCodigo, UUID vehiculoCodigo);

    /** El fin de la última asignación cerrada del auto, para que una nueva no se solape con ella. */
    Optional<LocalDate> ultimaFechaFin(UUID escuelaCodigo, UUID vehiculoCodigo);

    /**
     * Abre la asignación vigente con el `codigo` que trae. El índice único se valida en el mismo
     * instante: una carrera entre dos asignaciones del mismo auto falla acá y no al confirmar.
     *
     * @throws com.drivehub.escuela.common.exception.ConflictException `ERR_ESC_16` si el auto ya tiene una vigente
     */
    AsignacionVehiculo abrir(UUID escuelaCodigo, AsignacionVehiculo nueva);

    /** Cierra la asignación vigente del auto con esa fecha de fin. Vacío si no hay vigente. */
    Optional<AsignacionVehiculo> cerrar(UUID escuelaCodigo, UUID vehiculoCodigo, LocalDate fechaFin);

    /** Cierra la vigente con `fecha` y abre `nueva`, las dos cosas juntas (cambio de instructor). */
    AsignacionVehiculo cerrarYAbrir(UUID escuelaCodigo, UUID vehiculoCodigo, LocalDate fecha, AsignacionVehiculo nueva);

    /**
     * Un renglón por auto de la escuela, del más reciente al más antiguo, con su asignación vigente.
     *
     * @param pagina 1-indexada
     */
    Pagina<AsignacionDeAuto> listar(UUID escuelaCodigo, FiltroAsignaciones filtro, int pagina, int tamano);

    /** Las asignaciones del auto, de la más reciente a la más antigua. */
    Pagina<AsignacionHistorial> historial(UUID escuelaCodigo, UUID vehiculoCodigo, int pagina, int tamano);
}
