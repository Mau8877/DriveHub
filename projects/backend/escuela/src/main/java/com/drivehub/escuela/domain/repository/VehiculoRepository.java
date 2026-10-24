package com.drivehub.escuela.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.drivehub.escuela.domain.entity.Vehiculo;

/**
 * Lo que el negocio necesita saber de los autos (CU07). Todo se consulta dentro de una escuela:
 * un auto de otra escuela es, para quien consulta, un auto que no existe.
 */
public interface VehiculoRepository {

    /**
     * Si ya hay un auto con esa placa, de cualquier escuela: la placa es única en la plataforma.
     */
    boolean existePorPlaca(String placa);

    /**
     * Crea el auto con el `codigo` y la `escuelaCodigo` que trae. La placa se valida contra el
     * UNIQUE en el mismo instante: una colisión falla acá y no al confirmar.
     */
    Vehiculo guardar(Vehiculo vehiculo);

    Optional<Vehiculo> buscarPorCodigo(UUID escuelaCodigo, UUID codigo);

    /**
     * Listado paginado de la escuela, del más reciente al más antiguo.
     *
     * @param pagina 1-indexada
     */
    Pagina<Vehiculo> listar(UUID escuelaCodigo, FiltroVehiculos filtro, int pagina, int tamano);

    /**
     * Guarda los campos editables y el estado de un auto que ya existe. Vacío si no existe en la
     * escuela. La placa, la transmisión y la escuela nunca cambian.
     */
    Optional<Vehiculo> actualizar(Vehiculo vehiculo);
}
