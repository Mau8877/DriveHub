package com.drivehub.escuela.application.usecase;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.drivehub.escuela.application.dto.AsignacionVehiculoOutput;
import com.drivehub.escuela.application.dto.InstructorAsignadoOutput;
import com.drivehub.escuela.application.dto.ListarAsignacionesInput;
import com.drivehub.escuela.application.dto.PaginaOutput;
import com.drivehub.escuela.domain.gateway.InstructorCuentaGateway;
import com.drivehub.escuela.domain.gateway.PersonaInstructor;
import com.drivehub.escuela.domain.repository.AsignacionDeAuto;
import com.drivehub.escuela.domain.repository.AsignacionVehiculoRepository;
import com.drivehub.escuela.domain.repository.FiltroAsignaciones;
import com.drivehub.escuela.domain.repository.Pagina;

import lombok.RequiredArgsConstructor;

/**
 * CU10: el Administrador de escuela lista los autos de SU escuela con el instructor que los usa
 * (o "sin asignar"), del más reciente al más antiguo.
 *
 * * La búsqueda coincide con placa, marca o modelo y, además, con el nombre del instructor: se le
 * * piden a Identidad las personas que coinciden y se suman sus autos. Después, UNA consulta por
 * * lote trae el nombre de los instructores de la página; una persona que Identidad no devuelve deja
 * * el nombre en `null` (WARN), nunca rompe el listado.
 * * Sin log por lectura (LOGGING_BACKEND.md). No abre una transacción propia: así no se retiene una
 * * conexión de la base mientras se espera a Identidad.
 */
@Service
@RequiredArgsConstructor
public class ListarAsignacionesVehiculoUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(ListarAsignacionesVehiculoUseCase.class);
    static final int TAMANO_POR_DEFECTO = 20;
    static final int TAMANO_MAXIMO = 100;

    private final AsignacionVehiculoRepository asignacionRepository;
    private final InstructorCuentaGateway cuentaGateway;

    @PreAuthorize("hasRole('ADMIN_ESCUELA')")
    public PaginaOutput<AsignacionVehiculoOutput> ejecutar(ListarAsignacionesInput input) {
        int pagina = input.pagina() == null || input.pagina() < 1 ? 1 : input.pagina();
        int tamano = input.tamano() == null || input.tamano() < 1
            ? TAMANO_POR_DEFECTO : Math.min(input.tamano(), TAMANO_MAXIMO);
        String busqueda = input.busqueda() == null || input.busqueda().isBlank() ? null : input.busqueda().trim();

        List<UUID> personasQueCoinciden = busqueda == null ? List.of() : cuentaGateway.buscarCodigos(busqueda);

        Pagina<AsignacionDeAuto> resultado = asignacionRepository.listar(input.escuelaCodigo(),
            new FiltroAsignaciones(busqueda, personasQueCoinciden, input.asignacion()), pagina, tamano);

        Map<UUID, PersonaInstructor> personas = nombres(resultado.elementos());
        List<AsignacionVehiculoOutput> elementos = resultado.elementos().stream().map(fila -> {
            var v = fila.vehiculo();
            if (fila.vigente() == null) {
                return new AsignacionVehiculoOutput(v.getCodigo(), v.getPlaca(), v.getMarca(), v.getModelo(),
                    v.getEstado(), null, null);
            }
            PersonaInstructor persona = personas.get(fila.instructorUsuarioCodigo());
            if (persona == null) {
                LOG.warn("Identidad no devolvió a la persona de un instructor", kv("module", "asignaciones-vehiculo"),
                    kv("instructorCodigo", fila.vigente().getInstructorCodigo()));
            }
            return new AsignacionVehiculoOutput(v.getCodigo(), v.getPlaca(), v.getMarca(), v.getModelo(),
                v.getEstado(), new InstructorAsignadoOutput(fila.vigente().getInstructorCodigo(),
                    persona == null ? null : persona.nombreCompleto()), fila.vigente().getFechaInicio());
        }).toList();

        int totalPaginas = (int) Math.ceil((double) resultado.total() / tamano);
        return new PaginaOutput<>(elementos, pagina, tamano, resultado.total(), totalPaginas);
    }

    private Map<UUID, PersonaInstructor> nombres(List<AsignacionDeAuto> filas) {
        List<UUID> usuarios = filas.stream().map(AsignacionDeAuto::instructorUsuarioCodigo)
            .filter(java.util.Objects::nonNull).distinct().toList();
        if (usuarios.isEmpty()) {
            return Map.of();
        }
        return cuentaGateway.consultar(usuarios).stream()
            .collect(Collectors.toMap(PersonaInstructor::codigo, Function.identity(), (a, b) -> a));
    }
}
