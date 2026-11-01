package com.drivehub.academico.features.examenes.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.drivehub.academico.common.enums.ResultadoEvaluacion;
import com.drivehub.academico.common.exception.AcademicoErrorCodes;
import com.drivehub.academico.common.exception.ConflictException;
import com.drivehub.academico.common.exception.NotFoundException;
import com.drivehub.academico.common.exception.ValidationException;
import com.drivehub.academico.features.examenes.dto.ExamenResponse;
import com.drivehub.academico.features.examenes.dto.ProgramarExamenRequest;
import com.drivehub.academico.features.examenes.dto.RegistrarResultadoExamenRequest;
import com.drivehub.academico.features.examenes.dto.ReprogramarExamenRequest;
import com.drivehub.academico.features.examenes.enums.EstadoExamen;
import com.drivehub.academico.features.examenes.model.ExamenConduccion;
import com.drivehub.academico.features.examenes.repository.ExamenConduccionRepository;
import com.drivehub.academico.features.examenes.repository.FiltroExamenes;
import com.drivehub.academico.features.reservas.client.IdentidadClient;
import com.drivehub.academico.features.resultados.enums.OrigenResultado;
import com.drivehub.academico.features.resultados.model.Resultado;
import com.drivehub.academico.features.resultados.repository.ResultadoRepository;

import lombok.RequiredArgsConstructor;

/**
 * Lógica de los exámenes oficiales de conducción de una escuela (CU22). Única capa que loguea y lanza excepciones
 * de negocio. Todo se hace dentro de la escuela que llega de parámetro (la del token): un examen de otra escuela es,
 * para quien lo pide, uno que no existe.
 *
 * * Un examen programado termina de una sola forma: se cancela, el estudiante no asiste o se registra el resultado.
 * * Al registrar el resultado se escribe también la nota consolidada que ve CU23.
 * ? Hoy el evaluador y el vehículo se guardan como llegan (la pantalla los elige de las listas de la escuela):
 * ? validarlos contra Escuela e Identidad queda como seguimiento.
 */
@Service
@RequiredArgsConstructor
public class ExamenConduccionService {

    static final int TAMANO_POR_DEFECTO = 20;
    static final int TAMANO_MAXIMO = 100;
    // * Puntaje mínimo (sobre 100) para aprobar el examen de conducción
    static final double PUNTAJE_APROBACION = 70;
    static final String MODULO = "examenes";

    private static final Logger LOG = LoggerFactory.getLogger(ExamenConduccionService.class);

    private final ExamenConduccionRepository examenRepository;
    private final ResultadoRepository resultadoRepository;
    private final IdentidadClient identidadClient;
    private final Clock clock;

    @PreAuthorize("hasRole('ADMIN_ESCUELA')")
    public ExamenResponse programar(UUID escuelaCodigo, UUID autorCodigo, String bearer, ProgramarExamenRequest request) {
        Instant fecha = fechaProgramada(request.fecha(), request.hora());
        String nombre = identidadClient.nombresDeEstudiantes(List.of(request.estudianteCodigo()), bearer)
            .get(request.estudianteCodigo());
        if (nombre == null) {
            LOG.warn("Examen rechazado: el estudiante no existe en la escuela", kv("module", MODULO),
                kv("errorCode", AcademicoErrorCodes.ESTUDIANTE_NO_ENCONTRADO));
            throw new NotFoundException(AcademicoErrorCodes.ESTUDIANTE_NO_ENCONTRADO,
                "El estudiante no existe en tu escuela");
        }
        if (examenRepository.existsByEscuelaCodigoAndEstudianteCodigoAndCategoriaAndEstadoAndDeletedAtIsNull(
                escuelaCodigo, request.estudianteCodigo(), request.categoria(), EstadoExamen.PROGRAMADO)) {
            LOG.warn("Examen rechazado: ya tiene uno programado", kv("module", MODULO),
                kv("errorCode", AcademicoErrorCodes.EXAMEN_YA_PROGRAMADO));
            throw new ConflictException(AcademicoErrorCodes.EXAMEN_YA_PROGRAMADO,
                "El estudiante ya tiene un examen programado en esa categoría");
        }

        long anteriores = examenRepository.countByEscuelaCodigoAndEstudianteCodigoAndCategoriaAndEstadoNotAndDeletedAtIsNull(
            escuelaCodigo, request.estudianteCodigo(), request.categoria(), EstadoExamen.CANCELADO);
        ExamenConduccion examen = new ExamenConduccion();
        examen.setEscuelaCodigo(escuelaCodigo);
        examen.setCreadoPorCodigo(autorCodigo);
        examen.setEstudianteCodigo(request.estudianteCodigo());
        examen.setCategoria(request.categoria());
        examen.setFechaProgramada(fecha);
        examen.setEvaluadorCodigo(request.evaluadorCodigo());
        examen.setVehiculoCodigo(request.vehiculoCodigo());
        examen.setIntento((int) anteriores + 1);
        examen.setEstado(EstadoExamen.PROGRAMADO);

        ExamenConduccion guardado = examenRepository.save(examen);
        LOG.info("Examen de conducción programado", kv("module", MODULO),
            kv("examenCodigo", guardado.getCodigoExamenConduccion()), kv("escuelaCodigo", escuelaCodigo));
        return aRespuesta(guardado, nombre);
    }

    /**
     * Página 1 y 20 por página por defecto (máximo 100). Los nombres de los estudiantes se piden a Identidad en una
     * sola consulta por página: Académico no los guarda.
     */
    @PreAuthorize("hasRole('ADMIN_ESCUELA')")
    public Page<ExamenResponse> listar(UUID escuelaCodigo, String bearer, FiltroExamenes filtro, Integer pagina,
            Integer tamano) {
        int numero = pagina == null || pagina < 1 ? 1 : pagina;
        int cantidad = tamano == null || tamano < 1 ? TAMANO_POR_DEFECTO : Math.min(tamano, TAMANO_MAXIMO);
        Page<ExamenConduccion> examenes = examenRepository.listar(escuelaCodigo, filtro, PageRequest.of(numero - 1, cantidad));
        Map<UUID, String> nombres = examenes.isEmpty() ? Map.of() : identidadClient.nombresDeEstudiantes(
            examenes.getContent().stream().map(ExamenConduccion::getEstudianteCodigo).toList(), bearer);
        return examenes.map(e -> aRespuesta(e, nombres.get(e.getEstudianteCodigo())));
    }

    @PreAuthorize("hasRole('ADMIN_ESCUELA')")
    public ExamenResponse obtener(UUID escuelaCodigo, String bearer, UUID codigo) {
        ExamenConduccion examen = buscar(escuelaCodigo, codigo);
        return aRespuesta(examen,
            identidadClient.nombresDeEstudiantes(List.of(examen.getEstudianteCodigo()), bearer).get(examen.getEstudianteCodigo()));
    }

    /**
     * Cambia solo lo enviado, y solo en un examen programado. La fecha y la hora se revisan juntas: si llega una sola,
     * la otra es la que ya tenía.
     */
    @PreAuthorize("hasRole('ADMIN_ESCUELA')")
    public ExamenResponse reprogramar(UUID escuelaCodigo, UUID codigo, ReprogramarExamenRequest request) {
        ExamenConduccion examen = buscarProgramado(escuelaCodigo, codigo);

        if (request.fecha() != null || request.hora() != null) {
            var zona = clock.getZone();
            var actual = examen.getFechaProgramada().atZone(zona);
            LocalDate fecha = request.fecha() != null ? request.fecha() : actual.toLocalDate();
            String hora = request.hora() != null ? request.hora() : actual.toLocalTime().toString().substring(0, 5);
            examen.setFechaProgramada(fechaProgramada(fecha, hora));
        }
        if (request.evaluadorCodigo() != null) {
            examen.setEvaluadorCodigo(request.evaluadorCodigo());
        }
        if (request.vehiculoCodigo() != null) {
            examen.setVehiculoCodigo(request.vehiculoCodigo());
        }

        ExamenConduccion guardado = examenRepository.save(examen);
        LOG.info("Examen de conducción reprogramado", kv("module", MODULO), kv("examenCodigo", codigo));
        return aRespuesta(guardado, null);
    }

    @PreAuthorize("hasRole('ADMIN_ESCUELA')")
    public ExamenResponse cancelar(UUID escuelaCodigo, UUID codigo) {
        ExamenConduccion examen = buscarProgramado(escuelaCodigo, codigo);
        examen.setEstado(EstadoExamen.CANCELADO);
        ExamenConduccion guardado = examenRepository.save(examen);
        LOG.info("Examen de conducción cancelado", kv("module", MODULO), kv("examenCodigo", codigo));
        return aRespuesta(guardado, null);
    }

    @PreAuthorize("hasRole('ADMIN_ESCUELA')")
    public ExamenResponse marcarNoAsistio(UUID escuelaCodigo, UUID codigo) {
        ExamenConduccion examen = buscarProgramado(escuelaCodigo, codigo);
        examen.setEstado(EstadoExamen.NO_ASISTIO);
        ExamenConduccion guardado = examenRepository.save(examen);
        LOG.info("Examen de conducción sin asistencia", kv("module", MODULO), kv("examenCodigo", codigo));
        return aRespuesta(guardado, null);
    }

    /**
     * Cierra el examen con su puntaje: aprobado desde {@value #PUNTAJE_APROBACION} puntos. Escribe también la nota
     * consolidada que ve CU23.
     */
    @PreAuthorize("hasRole('ADMIN_ESCUELA')")
    public ExamenResponse registrarResultado(UUID escuelaCodigo, UUID codigo, RegistrarResultadoExamenRequest request) {
        ExamenConduccion examen = buscarProgramado(escuelaCodigo, codigo);
        double puntaje = request.puntaje();
        boolean aprobado = puntaje >= PUNTAJE_APROBACION;
        String observaciones = request.observaciones() == null || request.observaciones().isBlank()
            ? null : request.observaciones().trim();

        examen.setEstado(EstadoExamen.REALIZADO);
        examen.setPuntaje(puntaje);
        examen.setResultado(aprobado ? ResultadoEvaluacion.APROBADO : ResultadoEvaluacion.REPROBADO);
        examen.setObservaciones(observaciones);
        ExamenConduccion guardado = examenRepository.save(examen);

        Resultado nota = new Resultado();
        nota.setEscuelaCodigo(escuelaCodigo);
        nota.setEstudianteCodigo(guardado.getEstudianteCodigo());
        nota.setOrigen(OrigenResultado.EXAMEN_CONDUCCION);
        nota.setOrigenId(guardado.getId());
        nota.setTitulo("Examen de conducción, categoría " + guardado.getCategoria() + " (intento " + guardado.getIntento() + ")");
        nota.setPuntaje(puntaje);
        nota.setAprobado(aprobado);
        nota.setFecha(clock.instant());
        resultadoRepository.save(nota);

        LOG.info("Resultado del examen de conducción registrado", kv("module", MODULO), kv("examenCodigo", codigo),
            kv("aprobado", aprobado));
        return aRespuesta(guardado, null);
    }

    /** La fecha y la hora de la escuela como instante; tiene que ser posterior a ahora. */
    private Instant fechaProgramada(LocalDate fecha, String hora) {
        if (fecha == null || hora == null) {
            throw invalido("La fecha y la hora son obligatorias");
        }
        Instant instante = fecha.atTime(LocalTime.parse(hora)).atZone(clock.getZone()).toInstant();
        if (!instante.isAfter(clock.instant())) {
            throw invalido("La fecha y la hora del examen deben ser posteriores a ahora");
        }
        return instante;
    }

    private ValidationException invalido(String mensaje) {
        LOG.warn("Examen rechazado: datos inválidos", kv("module", MODULO),
            kv("errorCode", AcademicoErrorCodes.VALIDACION));
        return new ValidationException(AcademicoErrorCodes.VALIDACION, mensaje);
    }

    private ExamenConduccion buscarProgramado(UUID escuelaCodigo, UUID codigo) {
        ExamenConduccion examen = buscar(escuelaCodigo, codigo);
        if (examen.getEstado() != EstadoExamen.PROGRAMADO) {
            LOG.warn("Cambio de examen rechazado: ya no está programado", kv("module", MODULO),
                kv("errorCode", AcademicoErrorCodes.EXAMEN_NO_ESTA_PROGRAMADO), kv("examenCodigo", codigo));
            throw new ConflictException(AcademicoErrorCodes.EXAMEN_NO_ESTA_PROGRAMADO,
                "Solo se puede cambiar un examen programado");
        }
        return examen;
    }

    private ExamenConduccion buscar(UUID escuelaCodigo, UUID codigo) {
        return examenRepository.findByEscuelaCodigoAndCodigoExamenConduccionAndDeletedAtIsNull(escuelaCodigo, codigo)
            .orElseThrow(() -> {
                LOG.warn("Examen no encontrado", kv("module", MODULO),
                    kv("errorCode", AcademicoErrorCodes.EXAMEN_NO_ENCONTRADO), kv("examenCodigo", codigo));
                return new NotFoundException(AcademicoErrorCodes.EXAMEN_NO_ENCONTRADO, "El examen no existe");
            });
    }

    private ExamenResponse aRespuesta(ExamenConduccion e, String estudianteNombre) {
        var local = e.getFechaProgramada().atZone(clock.getZone());
        return new ExamenResponse(e.getCodigoExamenConduccion(), e.getEstudianteCodigo(), estudianteNombre,
            e.getCategoria(), e.getFechaProgramada(), local.toLocalDate(),
            local.toLocalTime().toString().substring(0, 5), e.getEvaluadorCodigo(), e.getVehiculoCodigo(),
            e.getIntento(), e.getEstado(), e.getPuntaje(), e.getResultado(), e.getObservaciones());
    }
}
