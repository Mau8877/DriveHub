package com.drivehub.escuela.application.usecase;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.escuela.application.dto.ListarVehiculosInput;
import com.drivehub.escuela.application.dto.PaginaOutput;
import com.drivehub.escuela.application.dto.VehiculoOutput;
import com.drivehub.escuela.domain.entity.Vehiculo;
import com.drivehub.escuela.domain.repository.FiltroVehiculos;
import com.drivehub.escuela.domain.repository.Pagina;
import com.drivehub.escuela.domain.repository.VehiculoRepository;

import lombok.RequiredArgsConstructor;

/**
 * CU07: el Administrador de escuela lista los autos de SU escuela, del más reciente al más
 * antiguo, con búsqueda por placa, marca o modelo y filtro de estado.
 *
 * * Sin log por lectura: LOGGING_BACKEND.md no pide registrar cada GET.
 */
@Service
@RequiredArgsConstructor
public class ListarVehiculosUseCase {

    static final int TAMANO_POR_DEFECTO = 20;
    static final int TAMANO_MAXIMO = 100;

    private final VehiculoRepository vehiculoRepository;

    @PreAuthorize("hasRole('ADMIN_ESCUELA')")
    @Transactional(readOnly = true)
    public PaginaOutput<VehiculoOutput> ejecutar(ListarVehiculosInput input) {
        int pagina = input.pagina() == null || input.pagina() < 1 ? 1 : input.pagina();
        int tamano = input.tamano() == null || input.tamano() < 1
            ? TAMANO_POR_DEFECTO : Math.min(input.tamano(), TAMANO_MAXIMO);

        Pagina<Vehiculo> resultado = vehiculoRepository.listar(
            input.escuelaCodigo(), new FiltroVehiculos(input.busqueda(), input.estado()), pagina, tamano);

        int totalPaginas = (int) Math.ceil((double) resultado.total() / tamano);
        return new PaginaOutput<>(
            resultado.elementos().stream().map(ReglasVehiculo::aSalida).toList(),
            pagina, tamano, resultado.total(), totalPaginas);
    }
}
