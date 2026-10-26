package com.drivehub.escuela.application.usecase;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.drivehub.escuela.application.dto.HistorialAsignacionOutput;
import com.drivehub.escuela.application.dto.InstructorAsignadoOutput;
import com.drivehub.escuela.application.dto.PaginaOutput;
import com.drivehub.escuela.domain.gateway.InstructorCuentaGateway;
import com.drivehub.escuela.domain.gateway.PersonaInstructor;
import com.drivehub.escuela.domain.repository.AsignacionHistorial;
import com.drivehub.escuela.domain.repository.AsignacionVehiculoRepository;
import com.drivehub.escuela.domain.repository.Pagina;
import com.drivehub.escuela.domain.repository.VehiculoRepository;

import lombok.RequiredArgsConstructor;

/**
 * CU10: el Administrador de escuela ve el historial de asignaciones de un auto de SU escuela, de la
 * más reciente a la más antigua. Sin log por lectura. Sin transacción propia (espera a Identidad).
 */
@Service
@RequiredArgsConstructor
public class ObtenerHistorialVehiculoUseCase {

    static final int TAMANO_POR_DEFECTO = 20;
    static final int TAMANO_MAXIMO = 100;

    private final AsignacionVehiculoRepository asignacionRepository;
    private final VehiculoRepository vehiculoRepository;
    private final InstructorCuentaGateway cuentaGateway;

    @PreAuthorize("hasRole('ADMIN_ESCUELA')")
    public PaginaOutput<HistorialAsignacionOutput> ejecutar(
            UUID escuelaCodigo, UUID vehiculoCodigo, Integer pagina, Integer tamano) {
        ReglasAsignacionVehiculo.vehiculo(vehiculoRepository, escuelaCodigo, vehiculoCodigo);
        int p = pagina == null || pagina < 1 ? 1 : pagina;
        int t = tamano == null || tamano < 1 ? TAMANO_POR_DEFECTO : Math.min(tamano, TAMANO_MAXIMO);

        Pagina<AsignacionHistorial> resultado = asignacionRepository.historial(escuelaCodigo, vehiculoCodigo, p, t);

        List<UUID> usuarios = resultado.elementos().stream().map(AsignacionHistorial::instructorUsuarioCodigo)
            .filter(Objects::nonNull).distinct().toList();
        Map<UUID, PersonaInstructor> personas = usuarios.isEmpty() ? Map.of()
            : cuentaGateway.consultar(usuarios).stream()
                .collect(Collectors.toMap(PersonaInstructor::codigo, Function.identity(), (a, b) -> a));

        List<HistorialAsignacionOutput> elementos = resultado.elementos().stream().map(h -> {
            PersonaInstructor persona = personas.get(h.instructorUsuarioCodigo());
            return new HistorialAsignacionOutput(h.asignacion().getCodigo(),
                new InstructorAsignadoOutput(h.asignacion().getInstructorCodigo(),
                    persona == null ? null : persona.nombreCompleto()),
                h.asignacion().getFechaInicio(), h.asignacion().getFechaFin());
        }).toList();

        int totalPaginas = (int) Math.ceil((double) resultado.total() / t);
        return new PaginaOutput<>(elementos, p, t, resultado.total(), totalPaginas);
    }
}
