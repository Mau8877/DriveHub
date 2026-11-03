package com.drivehub.academico.features.clases.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Clock;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.drivehub.academico.common.enums.ResultadoEvaluacion;
import com.drivehub.academico.common.exception.AcademicoErrorCodes;
import com.drivehub.academico.common.exception.ConflictException;
import com.drivehub.academico.common.exception.ForbiddenException;
import com.drivehub.academico.common.exception.NotFoundException;
import com.drivehub.academico.features.clases.dto.CatalogoPruebasResponse;
import com.drivehub.academico.features.clases.dto.EvaluarClaseRequest;
import com.drivehub.academico.features.clases.dto.PruebaClaseResponse;
import com.drivehub.academico.features.clases.enums.Asistencia;
import com.drivehub.academico.features.clases.enums.TipoPrueba;
import com.drivehub.academico.features.clases.model.Clase;
import com.drivehub.academico.features.clases.model.CriterioEvaluacion;
import com.drivehub.academico.features.clases.model.PruebaClase;
import com.drivehub.academico.features.clases.repository.ClaseRepository;
import com.drivehub.academico.features.clases.repository.PruebaClaseRepository;
import com.drivehub.academico.features.reservas.service.CierreDeReservaService;
import com.drivehub.academico.features.resultados.enums.OrigenResultado;
import com.drivehub.academico.features.resultados.model.Resultado;
import com.drivehub.academico.features.resultados.repository.ResultadoRepository;

import lombok.RequiredArgsConstructor;

/**
 * Las pruebas de clase del instructor (CU18): evaluar una clase ya cerrada con criterios puntuados y consultar sus
 * pruebas.
 *
 * * Evaluar guarda la prueba y después escribe su nota en `resultados` (Mongo no da una transacción entre los dos
 * * documentos). Si el servicio se cayó entre los dos pasos, repetir la evaluación completa la nota de la prueba ya
 * * guardada sin duplicarla; el índice único `(claseId, tipo)` impide dos pruebas del mismo tipo a la vez.
 * * Una prueba no se corrige ni se borra, y evaluar no toca la clase, la reserva ni la billetera.
 * ! Nunca se loguean las observaciones ni el puntaje de cada criterio.
 */
@Service
@RequiredArgsConstructor
public class PruebaClaseService {

    static final int TAMANO_POR_DEFECTO = 20;
    static final int TAMANO_MAXIMO = 100;

    private static final Logger LOG = LoggerFactory.getLogger(PruebaClaseService.class);
    private static final String MODULO = "clases";

    private final ClaseRepository claseRepository;
    private final PruebaClaseRepository pruebaRepository;
    private final ResultadoRepository resultadoRepository;
    private final CierreDeReservaService cierre;
    private final Clock clock;

    /**
     * Evalúa una clase cerrada del instructor.
     *
     * @throws ForbiddenException `ERR_ACA_16` si el token no trae la escuela
     * @throws NotFoundException `ERR_ACA_35` si la clase no existe o es de otro instructor
     * @throws ConflictException `ERR_ACA_39` si la clase no se puede evaluar (el estudiante no asistió o su cierre no
     *         terminó); `ERR_ACA_40` si ya tiene una prueba de ese tipo
     * @throws com.drivehub.academico.common.exception.ValidationException `ERR_ACA_41` si los criterios o sus
     *         puntajes no son válidos
     */
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public PruebaClaseResponse evaluar(UUID instructorCodigo, UUID escuelaCodigo, EvaluarClaseRequest request) {
        exigirEscuela(escuelaCodigo);
        Clase clase = buscarClase(instructorCodigo, request.claseCodigo());
        if (clase.getAsistencia() == Asistencia.AUSENTE || !cierre.reservaCompletada(clase.getReservaId())) {
            throw new ConflictException(AcademicoErrorCodes.CLASE_NO_EVALUABLE, "Esa clase no se puede evaluar");
        }
        // * Antes de buscar la prueba existente: un cuerpo inválido nunca se confunde con un reintento
        List<CriterioEvaluacion> criterios = ReglasDePrueba.validarYArmarCriterios(request.tipo(),
            request.criterios());

        Optional<PruebaClase> previa = pruebaRepository.findByClaseIdAndTipoAndDeletedAtIsNull(clase.getId(),
            request.tipo());
        PruebaClase prueba;
        if (previa.isPresent()) {
            prueba = previa.get();
            if (resultadoRepository.existsByOrigenAndOrigenIdAndDeletedAtIsNull(OrigenResultado.PRUEBA_CLASE,
                    prueba.getId())) {
                throw pruebaYaRegistrada();
            }
        } else {
            double total = ReglasDePrueba.calcularTotal(criterios);
            prueba = guardar(new PruebaClase(clase.getId(), clase.getEstudianteCodigo(), instructorCodigo,
                request.tipo(), criterios, total, ReglasDePrueba.resultadoDe(total),
                ReglasDePrueba.limpiarObservaciones(request.observaciones()), clase.getFecha()));
        }

        boolean retomada = previa.isPresent();
        escribirNota(escuelaCodigo, prueba);
        LOG.info(retomada ? "Nota de prueba de clase completada" : "Prueba de clase registrada", kv("module", MODULO),
            kv("pruebaCodigo", prueba.getCodigoPruebaClase()), kv("claseCodigo", clase.getCodigoClase()),
            kv("instructorCodigo", instructorCodigo), kv("tipo", prueba.getTipo()),
            kv("resultado", prueba.getResultado()));
        return PruebaClaseResponse.de(prueba, clase.getCodigoClase());
    }

    /**
     * Las pruebas de una clase del instructor, en el orden en que se hicieron.
     *
     * @throws ForbiddenException `ERR_ACA_16` si el token no trae la escuela
     * @throws NotFoundException `ERR_ACA_35` si la clase no existe o es de otro instructor
     */
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public Page<PruebaClaseResponse> listarPorClase(UUID instructorCodigo, UUID escuelaCodigo, UUID claseCodigo,
            Integer pagina, Integer tamano) {
        exigirEscuela(escuelaCodigo);
        Clase clase = buscarClase(instructorCodigo, claseCodigo);
        int numero = pagina == null || pagina < 1 ? 1 : pagina;
        int cantidad = tamano == null || tamano < 1 ? TAMANO_POR_DEFECTO : Math.min(tamano, TAMANO_MAXIMO);
        return pruebaRepository
            .findByClaseIdAndDeletedAtIsNull(clase.getId(), PageRequest.of(numero - 1, cantidad, Sort.by("id")))
            .map(p -> PruebaClaseResponse.de(p, clase.getCodigoClase()));
    }

    /** Los criterios de cada tipo de prueba. */
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public CatalogoPruebasResponse criterios() {
        return CatalogoPruebasResponse.actual();
    }

    /**
     * Para la agenda: de cada reserva (`_id`) que ya tiene clase dictada, los tipos de prueba que ya se hicieron. Una
     * reserva sin clase no aparece en el resultado. Dos consultas, sin importar cuántas reservas haya. Sin
     * `@PreAuthorize`: lo llama la agenda, que ya exige el rol.
     */
    public Map<Long, Set<TipoPrueba>> tiposEvaluadosPorReserva(Collection<Long> reservaIds) {
        Map<Long, Set<TipoPrueba>> tipos = new HashMap<>();
        if (reservaIds.isEmpty()) {
            return tipos;
        }
        Map<Long, Long> reservaDeClase = new HashMap<>();
        claseRepository.findByReservaIdInAndDeletedAtIsNull(reservaIds).forEach(c -> {
            reservaDeClase.put(c.getId(), c.getReservaId());
            tipos.put(c.getReservaId(), EnumSet.noneOf(TipoPrueba.class));
        });
        if (reservaDeClase.isEmpty()) {
            return tipos;
        }
        pruebaRepository.findByClaseIdInAndDeletedAtIsNull(reservaDeClase.keySet())
            .forEach(p -> tipos.get(reservaDeClase.get(p.getClaseId())).add(p.getTipo()));
        return tipos;
    }

    private Clase buscarClase(UUID instructorCodigo, UUID claseCodigo) {
        return claseRepository.findByCodigoClaseAndInstructorCodigoAndDeletedAtIsNull(claseCodigo, instructorCodigo)
            .orElseThrow(() -> new NotFoundException(AcademicoErrorCodes.CLASE_INSTRUCTOR_NO_ENCONTRADA,
                "No encontramos esa clase"));
    }

    /** Un `DuplicateKeyException` es otra petición que creó la prueba de este tipo a la vez: es un conflicto. */
    private PruebaClase guardar(PruebaClase prueba) {
        try {
            return pruebaRepository.save(prueba);
        } catch (DuplicateKeyException e) {
            throw pruebaYaRegistrada();
        }
    }

    /**
     * La nota que ve CU23, como la que escribe el examen de conducción (CU22). El índice único `(origen, origenId)`
     * garantiza una sola nota por prueba aunque dos peticiones lleguen a la vez.
     */
    private void escribirNota(UUID escuelaCodigo, PruebaClase prueba) {
        Resultado nota = new Resultado();
        nota.setEscuelaCodigo(escuelaCodigo);
        nota.setEstudianteCodigo(prueba.getEstudianteCodigo());
        nota.setOrigen(OrigenResultado.PRUEBA_CLASE);
        nota.setOrigenId(prueba.getId());
        nota.setTitulo("Prueba de clase: " + CatalogoDePruebas.de(prueba.getTipo()).etiqueta());
        nota.setPuntaje(prueba.getPuntajeTotal());
        nota.setAprobado(prueba.getResultado() == ResultadoEvaluacion.APROBADO);
        nota.setFecha(clock.instant());
        try {
            resultadoRepository.save(nota);
        } catch (DuplicateKeyException e) {
            // * Otra petición a la vez ya escribió la nota de esta prueba: la evaluación tiene su nota, no hay nada más
            LOG.info("La nota de la prueba ya estaba escrita", kv("module", MODULO),
                kv("pruebaCodigo", prueba.getCodigoPruebaClase()));
        }
    }

    private static void exigirEscuela(UUID escuelaCodigo) {
        if (escuelaCodigo == null) {
            throw new ForbiddenException(AcademicoErrorCodes.TOKEN_SIN_ESCUELA,
                "Tu cuenta no pertenece a ninguna escuela");
        }
    }

    private static ConflictException pruebaYaRegistrada() {
        return new ConflictException(AcademicoErrorCodes.PRUEBA_YA_REGISTRADA,
            "Esa clase ya tiene una prueba de ese tipo");
    }
}
