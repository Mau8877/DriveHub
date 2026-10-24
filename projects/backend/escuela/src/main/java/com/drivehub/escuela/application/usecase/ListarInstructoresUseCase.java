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

import com.drivehub.escuela.application.dto.InstructorResumenOutput;
import com.drivehub.escuela.application.dto.ListarInstructoresInput;
import com.drivehub.escuela.application.dto.PaginaOutput;
import com.drivehub.escuela.domain.entity.Instructor;
import com.drivehub.escuela.domain.gateway.InstructorCuentaGateway;
import com.drivehub.escuela.domain.gateway.PersonaInstructor;
import com.drivehub.escuela.domain.repository.InstructorRepository;
import com.drivehub.escuela.domain.repository.Pagina;

import lombok.RequiredArgsConstructor;

/**
 * CU09: el Administrador de escuela lista los instructores de SU escuela, del alta más reciente al
 * más antiguo, con búsqueda por nombre y filtro de estado.
 *
 * * Manda la ficha (Escuela): filtro de estado, orden y paginación son de su tabla. La búsqueda por
 * * nombre le pide a Identidad los códigos que coinciden y filtra la tabla por ellos. Después, UNA
 * * consulta por lote trae nombre y foto de las filas de la página; una persona que Identidad no
 * * devuelve deja esos datos en `null` (WARN), nunca rompe el listado.
 * * Sin log por lectura: LOGGING_BACKEND.md no pide registrar cada GET.
 * * No abre una transacción propia: cada método del repositorio abre la suya, así no se retiene una
 * * conexión de la base mientras se espera a Identidad.
 */
@Service
@RequiredArgsConstructor
public class ListarInstructoresUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(ListarInstructoresUseCase.class);
    static final int TAMANO_POR_DEFECTO = 20;
    static final int TAMANO_MAXIMO = 100;

    private final InstructorRepository instructorRepository;
    private final InstructorCuentaGateway cuentaGateway;
    private final InstructorMapeador mapeador;

    @PreAuthorize("hasRole('ADMIN_ESCUELA')")
    public PaginaOutput<InstructorResumenOutput> ejecutar(ListarInstructoresInput input) {
        int pagina = input.pagina() == null || input.pagina() < 1 ? 1 : input.pagina();
        int tamano = input.tamano() == null || input.tamano() < 1
            ? TAMANO_POR_DEFECTO : Math.min(input.tamano(), TAMANO_MAXIMO);
        String busqueda = input.busqueda() == null || input.busqueda().isBlank() ? null : input.busqueda().trim();

        List<UUID> usuarioCodigos = null;
        if (busqueda != null) {
            usuarioCodigos = cuentaGateway.buscarCodigos(busqueda);
            if (usuarioCodigos.isEmpty()) {
                return new PaginaOutput<>(List.of(), pagina, tamano, 0, 0);
            }
        }

        Pagina<Instructor> resultado = instructorRepository.listar(
            input.escuelaCodigo(), input.estado(), usuarioCodigos, pagina, tamano);

        Map<UUID, PersonaInstructor> personas = resultado.elementos().isEmpty() ? Map.of()
            : cuentaGateway.consultar(resultado.elementos().stream().map(Instructor::getUsuarioCodigo).toList())
                .stream().collect(Collectors.toMap(PersonaInstructor::codigo, Function.identity(), (a, b) -> a));

        List<InstructorResumenOutput> elementos = resultado.elementos().stream().map(ficha -> {
            PersonaInstructor persona = personas.get(ficha.getUsuarioCodigo());
            if (persona == null) {
                LOG.warn("Identidad no devolvió a la persona de un instructor",
                    kv("module", "instructores"), kv("instructorCodigo", ficha.getCodigo()));
            }
            return mapeador.aResumen(ficha, persona);
        }).toList();

        int totalPaginas = (int) Math.ceil((double) resultado.total() / tamano);
        return new PaginaOutput<>(elementos, pagina, tamano, resultado.total(), totalPaginas);
    }
}
