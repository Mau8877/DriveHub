package com.drivehub.academico.features.resultados.service;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.drivehub.academico.features.reservas.client.IdentidadClient;
import com.drivehub.academico.features.resultados.client.EscuelaClient;
import com.drivehub.academico.features.resultados.dto.ResultadoResponse;
import com.drivehub.academico.features.resultados.model.Resultado;
import com.drivehub.academico.features.resultados.repository.FiltroResultados;
import com.drivehub.academico.features.resultados.repository.ResultadoRepository;

import lombok.RequiredArgsConstructor;

/**
 * Consulta de resultados y notas (CU23): el estudiante ve las suyas, el Administrador de escuela las de todos los
 * estudiantes de su escuela y el instructor las de sus alumnos asignados. Solo lectura, así que no loguea (LOGGING_BACKEND.md no pide registrar cada GET).
 *
 * * Las notas las escriben al cerrar cada evaluación sus casos de uso: el simulacro (CU21) y el examen de conducción
 *   (CU22). La prueba de clase (CU17) las sumará con el suyo.
 * * El nombre del estudiante no se guarda en Académico: el Administrador lo ve porque se pide a Identidad en una sola
 *   consulta por página.
 * * Los alumnos del instructor los sabe Escuela: se le piden en cada consulta, con el Bearer del instructor, y se
 *   filtran las notas por esos estudiantes.
 */
@Service
@RequiredArgsConstructor
public class ResultadoService {

    static final int TAMANO_POR_DEFECTO = 20;
    static final int TAMANO_MAXIMO = 100;

    private final ResultadoRepository resultadoRepository;
    private final IdentidadClient identidadClient;
    private final EscuelaClient escuelaClient;
    private final Clock clock;

    /** Las notas del propio estudiante. El filtro por estudiante se ignora: solo ve las suyas. */
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public Page<ResultadoResponse> listarMios(UUID estudianteCodigo, FiltroResultados filtro, Integer pagina,
            Integer tamano) {
        return resultadoRepository
            .listarDeEstudiante(estudianteCodigo, new FiltroResultados(filtro.origen(), null), pedido(pagina, tamano))
            .map(r -> aRespuesta(r, null));
    }

    /** Las notas de la escuela, con el nombre de cada estudiante. */
    @PreAuthorize("hasRole('ADMIN_ESCUELA')")
    public Page<ResultadoResponse> listarDeEscuela(UUID escuelaCodigo, String bearer, FiltroResultados filtro,
            Integer pagina, Integer tamano) {
        Page<Resultado> resultados = resultadoRepository.listarDeEscuela(escuelaCodigo, filtro, pedido(pagina, tamano));
        Map<UUID, String> nombres = resultados.isEmpty() ? Map.of() : identidadClient.nombresDeEstudiantes(
            resultados.getContent().stream().map(Resultado::getEstudianteCodigo).toList(), bearer);
        return resultados.map(r -> aRespuesta(r, nombres.get(r.getEstudianteCodigo())));
    }

    /**
     * Las notas de los alumnos asignados al instructor, con el nombre de cada uno. El filtro por estudiante solo
     * acota: un estudiante que no es alumno suyo no devuelve nada.
     */
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public Page<ResultadoResponse> listarDeAlumnos(String bearer, FiltroResultados filtro, Integer pagina,
            Integer tamano) {
        PageRequest pedido = pedido(pagina, tamano);
        List<UUID> alumnos = escuelaClient.alumnosDelInstructor(bearer);
        if (filtro.estudianteCodigo() != null) {
            alumnos = alumnos.stream().filter(filtro.estudianteCodigo()::equals).toList();
        }
        if (alumnos.isEmpty()) {
            return Page.empty(pedido);
        }
        Page<Resultado> resultados = resultadoRepository.listarDeEstudiantes(alumnos,
            new FiltroResultados(filtro.origen(), null), pedido);
        Map<UUID, String> nombres = resultados.isEmpty() ? Map.of() : identidadClient.nombresDeEstudiantes(
            resultados.getContent().stream().map(Resultado::getEstudianteCodigo).toList(), bearer);
        return resultados.map(r -> aRespuesta(r, nombres.get(r.getEstudianteCodigo())));
    }

    private static PageRequest pedido(Integer pagina, Integer tamano) {
        int numero = pagina == null || pagina < 1 ? 1 : pagina;
        int cantidad = tamano == null || tamano < 1 ? TAMANO_POR_DEFECTO : Math.min(tamano, TAMANO_MAXIMO);
        return PageRequest.of(numero - 1, cantidad);
    }

    private ResultadoResponse aRespuesta(Resultado r, String estudianteNombre) {
        return new ResultadoResponse(r.getCodigoResultado(), r.getEstudianteCodigo(), estudianteNombre, r.getOrigen(),
            r.getTitulo(), r.getPuntaje(), r.isAprobado(), r.getFecha(),
            r.getFecha().atZone(clock.getZone()).toLocalDate());
    }
}
