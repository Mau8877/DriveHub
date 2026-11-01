package com.drivehub.academico.features.simulacros.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.drivehub.academico.common.exception.AcademicoErrorCodes;
import com.drivehub.academico.common.exception.ConflictException;
import com.drivehub.academico.common.exception.NotFoundException;
import com.drivehub.academico.features.preguntas.model.OpcionPregunta;
import com.drivehub.academico.features.preguntas.model.Pregunta;
import com.drivehub.academico.features.preguntas.repository.PreguntaRepository;
import com.drivehub.academico.features.resultados.enums.OrigenResultado;
import com.drivehub.academico.features.resultados.model.Resultado;
import com.drivehub.academico.features.resultados.repository.ResultadoRepository;
import com.drivehub.academico.features.simulacros.dto.FinalizarIntentoRequest;
import com.drivehub.academico.features.simulacros.dto.IntentoIniciadoResponse;
import com.drivehub.academico.features.simulacros.dto.IntentoResultadoResponse;
import com.drivehub.academico.features.simulacros.dto.IntentoResumenResponse;
import com.drivehub.academico.features.simulacros.dto.SimulacroDisponibleResponse;
import com.drivehub.academico.features.simulacros.enums.EstadoIntento;
import com.drivehub.academico.features.simulacros.model.IntentoSimulacro;
import com.drivehub.academico.features.simulacros.model.PreguntaIntento;
import com.drivehub.academico.features.simulacros.model.SimulacroConfig;
import com.drivehub.academico.features.simulacros.repository.IntentoSimulacroRepository;
import com.drivehub.academico.features.simulacros.repository.SimulacroConfigRepository;

import lombok.RequiredArgsConstructor;

/**
 * Lógica de rendir un simulacro escrito (CU21): el estudiante ve los simulacros activos de su escuela, inicia un
 * intento (el sistema arma las preguntas del banco según las reglas del simulacro), responde y finaliza. Única capa
 * que loguea y lanza excepciones de negocio.
 *
 * * El intento guarda una COPIA de las preguntas y de la configuración: editar el banco o el simulacro no lo altera.
 * * Las respuestas correctas solo salen al terminar el intento (en la corrección), nunca al iniciarlo.
 * * Un intento tiene `duracionMin` minutos; se admite una gracia corta para el envío automático al acabarse el tiempo.
 *   Pasada la gracia queda EXPIRADO con 0 puntos.
 */
@Service
@RequiredArgsConstructor
public class IntentoSimulacroService {

    static final int TAMANO_POR_DEFECTO = 20;
    static final int TAMANO_MAXIMO = 100;
    static final Duration GRACIA = Duration.ofSeconds(30);
    static final String MODULO = "intentos-simulacro";
    private static final String GENERAL = "GENERAL";

    private static final Logger LOG = LoggerFactory.getLogger(IntentoSimulacroService.class);

    private final SimulacroConfigRepository simulacroRepository;
    private final IntentoSimulacroRepository intentoRepository;
    private final PreguntaRepository preguntaRepository;
    private final ResultadoRepository resultadoRepository;
    private final Clock clock;
    private final Random random = new Random();

    @PreAuthorize("hasRole('ESTUDIANTE')")
    public List<SimulacroDisponibleResponse> disponibles(UUID escuelaCodigo) {
        return simulacroRepository.findByEscuelaCodigoAndActivoTrueAndDeletedAtIsNullOrderByNombreAsc(escuelaCodigo)
            .stream().map(s -> new SimulacroDisponibleResponse(s.getCodigoSimulacro(), s.getNombre(),
                s.getCategoria(), s.getDuracionMin(), s.getNroPreguntas(), s.getPuntajeAprobacion())).toList();
    }

    @PreAuthorize("hasRole('ESTUDIANTE')")
    public IntentoIniciadoResponse iniciar(UUID escuelaCodigo, UUID estudianteCodigo, UUID simulacroCodigo) {
        SimulacroConfig simulacro = simulacroRepository
            .findByEscuelaCodigoAndCodigoSimulacroAndDeletedAtIsNull(escuelaCodigo, simulacroCodigo)
            .filter(SimulacroConfig::isActivo)
            .orElseThrow(() -> {
                LOG.warn("Intento rechazado: el simulacro no está disponible", kv("module", MODULO),
                    kv("errorCode", AcademicoErrorCodes.SIMULACRO_NO_ENCONTRADO),
                    kv("simulacroCodigo", simulacroCodigo));
                return new NotFoundException(AcademicoErrorCodes.SIMULACRO_NO_ENCONTRADO, "El simulacro no existe");
            });
        Instant ahora = clock.instant();

        for (IntentoSimulacro previo : intentoRepository
                .findByEstudianteCodigoAndSimulacroCodigoAndEstadoAndDeletedAtIsNull(
                    estudianteCodigo, simulacroCodigo, EstadoIntento.EN_CURSO)) {
            if (ahora.isBefore(vence(previo))) {
                LOG.warn("Intento rechazado: ya hay uno en curso", kv("module", MODULO),
                    kv("errorCode", AcademicoErrorCodes.INTENTO_EN_CURSO));
                throw new ConflictException(AcademicoErrorCodes.INTENTO_EN_CURSO,
                    "Ya tienes un intento en curso de este simulacro");
            }
            expirar(previo);
        }

        List<PreguntaIntento> preguntas = armarPreguntas(escuelaCodigo, simulacro);
        IntentoSimulacro intento = new IntentoSimulacro();
        intento.setSimulacroId(simulacro.getId());
        intento.setEscuelaCodigo(escuelaCodigo);
        intento.setSimulacroCodigo(simulacro.getCodigoSimulacro());
        intento.setSimulacroNombre(simulacro.getNombre());
        intento.setDuracionMin(simulacro.getDuracionMin());
        intento.setPuntajeAprobacion(simulacro.getPuntajeAprobacion());
        intento.setEstudianteCodigo(estudianteCodigo);
        intento.setEstado(EstadoIntento.EN_CURSO);
        intento.setIniciadoEn(ahora);
        intento.setPreguntas(preguntas);

        IntentoSimulacro guardado = intentoRepository.save(intento);
        LOG.info("Intento de simulacro iniciado", kv("module", MODULO),
            kv("intentoCodigo", guardado.getCodigoIntentoSimulacro()), kv("simulacroCodigo", simulacroCodigo));
        return new IntentoIniciadoResponse(guardado.getCodigoIntentoSimulacro(), guardado.getSimulacroNombre(),
            guardado.getDuracionMin(), ahora, vence(guardado),
            preguntas.stream().map(p -> new IntentoIniciadoResponse.PreguntaParaRendir(p.preguntaCodigo(),
                p.enunciado(), p.tipo(),
                p.opciones().stream().map(o -> new IntentoIniciadoResponse.OpcionParaRendir(o.id(), o.texto())).toList()))
                .toList());
    }

    @PreAuthorize("hasRole('ESTUDIANTE')")
    public IntentoResultadoResponse finalizar(UUID estudianteCodigo, UUID codigo, FinalizarIntentoRequest request) {
        IntentoSimulacro intento = buscar(estudianteCodigo, codigo);
        if (intento.getEstado() != EstadoIntento.EN_CURSO) {
            LOG.warn("Finalización rechazada: el intento ya terminó", kv("module", MODULO),
                kv("errorCode", AcademicoErrorCodes.INTENTO_YA_FINALIZADO), kv("intentoCodigo", codigo));
            throw new ConflictException(AcademicoErrorCodes.INTENTO_YA_FINALIZADO, "Este intento ya terminó");
        }
        Instant ahora = clock.instant();
        if (ahora.isAfter(vence(intento).plus(GRACIA))) {
            return aResultado(expirar(intento));
        }

        Map<UUID, List<String>> respuestas = request.respuestas().stream().collect(Collectors.toMap(
            FinalizarIntentoRequest.RespuestaRequest::preguntaCodigo,
            r -> r.elegidas() == null ? List.<String>of() : r.elegidas(), (a, b) -> b));
        List<PreguntaIntento> corregidas = new ArrayList<>();
        int aciertos = 0;
        for (PreguntaIntento p : intento.getPreguntas()) {
            Set<String> validas = p.opciones().stream().map(OpcionPregunta::id).collect(Collectors.toSet());
            Set<String> elegidas = new LinkedHashSet<>();
            for (String id : respuestas.getOrDefault(p.preguntaCodigo(), List.of())) {
                if (validas.contains(id)) {
                    elegidas.add(id);
                }
            }
            Set<String> correctas = p.opciones().stream().filter(OpcionPregunta::correcta).map(OpcionPregunta::id)
                .collect(Collectors.toCollection(HashSet::new));
            boolean acierto = !elegidas.isEmpty() && correctas.equals(new HashSet<>(elegidas));
            aciertos += acierto ? 1 : 0;
            corregidas.add(new PreguntaIntento(p.preguntaId(), p.preguntaCodigo(), p.enunciado(), p.tipo(),
                p.opciones(), p.explicacion(), List.copyOf(elegidas), acierto));
        }
        int total = corregidas.size();
        double puntaje = total == 0 ? 0 : Math.round(aciertos * 10000.0 / total) / 100.0;

        intento.setPreguntas(corregidas);
        intento.setAciertos(aciertos);
        intento.setPuntaje(puntaje);
        intento.setAprobado(puntaje >= intento.getPuntajeAprobacion());
        intento.setEstado(EstadoIntento.FINALIZADO);
        intento.setFinalizadoEn(ahora);
        IntentoSimulacro guardado = intentoRepository.save(intento);

        Resultado nota = new Resultado();
        nota.setEscuelaCodigo(guardado.getEscuelaCodigo());
        nota.setEstudianteCodigo(estudianteCodigo);
        nota.setOrigen(OrigenResultado.SIMULACRO);
        nota.setOrigenId(guardado.getId());
        nota.setTitulo("Simulacro: " + guardado.getSimulacroNombre());
        nota.setPuntaje(puntaje);
        nota.setAprobado(guardado.isAprobado());
        nota.setFecha(ahora);
        resultadoRepository.save(nota);

        LOG.info("Intento de simulacro finalizado", kv("module", MODULO), kv("intentoCodigo", codigo),
            kv("aprobado", guardado.isAprobado()));
        return aResultado(guardado);
    }

    /** El historial del estudiante: terminados y expirados, del más reciente al más antiguo. */
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public Page<IntentoResumenResponse> listarMios(UUID estudianteCodigo, Integer pagina, Integer tamano) {
        int numero = pagina == null || pagina < 1 ? 1 : pagina;
        int cantidad = tamano == null || tamano < 1 ? TAMANO_POR_DEFECTO : Math.min(tamano, TAMANO_MAXIMO);
        return intentoRepository.findByEstudianteCodigoAndEstadoNotAndDeletedAtIsNullOrderByIniciadoEnDesc(
            estudianteCodigo, EstadoIntento.EN_CURSO, PageRequest.of(numero - 1, cantidad))
            .map(i -> new IntentoResumenResponse(i.getCodigoIntentoSimulacro(), i.getSimulacroNombre(), i.getEstado(),
                i.getIniciadoEn(), i.getFinalizadoEn(), i.getAciertos(), i.getPreguntas().size(), i.getPuntaje(),
                i.isAprobado()));
    }

    /** La corrección de un intento terminado. Uno que sigue en curso todavía no tiene corrección. */
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public IntentoResultadoResponse obtener(UUID estudianteCodigo, UUID codigo) {
        IntentoSimulacro intento = buscar(estudianteCodigo, codigo);
        if (intento.getEstado() == EstadoIntento.EN_CURSO) {
            if (clock.instant().isBefore(vence(intento).plus(GRACIA))) {
                throw new ConflictException(AcademicoErrorCodes.INTENTO_EN_CURSO,
                    "El intento todavía está en curso");
            }
            return aResultado(expirar(intento));
        }
        return aResultado(intento);
    }

    private List<PreguntaIntento> armarPreguntas(UUID escuelaCodigo, SimulacroConfig simulacro) {
        // * Las preguntas de la categoría del simulacro y las GENERAL sirven a todas
        List<String> categorias = GENERAL.equals(simulacro.getCategoria())
            ? List.of(GENERAL) : List.of(simulacro.getCategoria(), GENERAL);
        List<Pregunta> elegidas = new ArrayList<>();
        for (var regla : simulacro.getReglas()) {
            List<Pregunta> candidatas = new ArrayList<>(
                preguntaRepository.activasDelTema(escuelaCodigo, categorias, regla.tema()));
            if (candidatas.size() < regla.cantidad()) {
                LOG.warn("Intento rechazado: el banco no alcanza", kv("module", MODULO),
                    kv("errorCode", AcademicoErrorCodes.SIMULACRO_SIN_PREGUNTAS));
                throw new ConflictException(AcademicoErrorCodes.SIMULACRO_SIN_PREGUNTAS,
                    "Este simulacro todavía no tiene preguntas suficientes. Avisa a tu escuela.");
            }
            Collections.shuffle(candidatas, random);
            elegidas.addAll(candidatas.subList(0, regla.cantidad()));
        }
        Collections.shuffle(elegidas, random);
        return elegidas.stream().map(p -> new PreguntaIntento(p.getId(), p.getCodigoPregunta(), p.getEnunciado(),
            p.getTipo(), p.getOpciones(), p.getExplicacion(), List.of(), false)).toList();
    }

    /** Cierra un intento al que se le acabó el tiempo: 0 puntos y ninguna respuesta. No genera nota. */
    private IntentoSimulacro expirar(IntentoSimulacro intento) {
        intento.setEstado(EstadoIntento.EXPIRADO);
        intento.setFinalizadoEn(vence(intento));
        intento.setAciertos(0);
        intento.setPuntaje(0);
        intento.setAprobado(false);
        IntentoSimulacro guardado = intentoRepository.save(intento);
        LOG.info("Intento de simulacro expirado", kv("module", MODULO),
            kv("intentoCodigo", guardado.getCodigoIntentoSimulacro()));
        return guardado;
    }

    private IntentoSimulacro buscar(UUID estudianteCodigo, UUID codigo) {
        return intentoRepository.findByEstudianteCodigoAndCodigoIntentoSimulacroAndDeletedAtIsNull(estudianteCodigo, codigo)
            .orElseThrow(() -> {
                LOG.warn("Intento no encontrado", kv("module", MODULO),
                    kv("errorCode", AcademicoErrorCodes.INTENTO_NO_ENCONTRADO), kv("intentoCodigo", codigo));
                return new NotFoundException(AcademicoErrorCodes.INTENTO_NO_ENCONTRADO, "El intento no existe");
            });
    }

    private static Instant vence(IntentoSimulacro i) {
        return i.getIniciadoEn().plus(Duration.ofMinutes(i.getDuracionMin()));
    }

    private static IntentoResultadoResponse aResultado(IntentoSimulacro i) {
        List<IntentoResultadoResponse.Correccion> preguntas = i.getPreguntas().stream()
            .map(p -> new IntentoResultadoResponse.Correccion(p.preguntaCodigo(), p.enunciado(), p.tipo(),
                p.opciones().stream()
                    .map(o -> new IntentoResultadoResponse.OpcionCorregida(o.id(), o.texto(), o.correcta())).toList(),
                p.elegidas(), p.correcta(), p.explicacion())).toList();
        return new IntentoResultadoResponse(i.getCodigoIntentoSimulacro(), i.getSimulacroNombre(), i.getEstado(),
            i.getIniciadoEn(), i.getFinalizadoEn(), i.getAciertos(), preguntas.size(), i.getPuntaje(), i.isAprobado(),
            i.getPuntajeAprobacion(), preguntas);
    }
}
