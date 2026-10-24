package com.drivehub.academico.features.preguntas.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.drivehub.academico.common.exception.AcademicoErrorCodes;
import com.drivehub.academico.common.exception.ConflictException;
import com.drivehub.academico.common.exception.NotFoundException;
import com.drivehub.academico.features.preguntas.dto.CrearPreguntaRequest;
import com.drivehub.academico.features.preguntas.dto.EditarPreguntaRequest;
import com.drivehub.academico.features.preguntas.dto.PreguntaResponse;
import com.drivehub.academico.features.preguntas.model.Pregunta;
import com.drivehub.academico.features.preguntas.repository.FiltroPreguntas;
import com.drivehub.academico.features.preguntas.repository.PreguntaRepository;

import lombok.RequiredArgsConstructor;

/**
 * Lógica del banco de preguntas de una escuela (CU19). Única capa que loguea y lanza excepciones de
 * negocio. Todo se hace dentro de la escuela que llega de parámetro (la del token): una pregunta de
 * otra escuela es, para quien la pide, una que no existe.
 *
 * * Una pregunta no se borra: se desactiva (`activa = false`) y deja de salir en los simulacros nuevos
 * * (CU20), pero los intentos ya rendidos conservan su copia (CU21).
 */
@Service
@RequiredArgsConstructor
public class PreguntaService {

    static final int TAMANO_POR_DEFECTO = 20;
    static final int TAMANO_MAXIMO = 100;

    private static final Logger LOG = LoggerFactory.getLogger(PreguntaService.class);

    private final PreguntaRepository preguntaRepository;

    @PreAuthorize("hasRole('ADMIN_ESCUELA')")
    public PreguntaResponse crear(UUID escuelaCodigo, UUID autorCodigo, CrearPreguntaRequest request) {
        Pregunta pregunta = new Pregunta();
        pregunta.setEscuelaCodigo(escuelaCodigo);
        pregunta.setCreadaPorCodigo(autorCodigo);
        pregunta.setCategoria(ReglasPregunta.categoria(request.categoria()));
        pregunta.setTema(ReglasPregunta.texto(request.tema(), "tema", 2, 80));
        pregunta.setTipo(request.tipo());
        pregunta.setEnunciado(ReglasPregunta.texto(request.enunciado(), "enunciado", 5, 500));
        pregunta.setOpciones(ReglasPregunta.opciones(request.tipo(), request.opciones()));
        pregunta.setExplicacion(ReglasPregunta.explicacion(request.explicacion()));
        pregunta.setDificultad(ReglasPregunta.dificultad(request.dificultad()));
        pregunta.setActiva(true);

        Pregunta guardada = preguntaRepository.save(pregunta);
        LOG.info("Pregunta creada", kv("module", ReglasPregunta.MODULO),
            kv("preguntaCodigo", guardada.getCodigoPregunta()), kv("escuelaCodigo", escuelaCodigo));
        return aRespuesta(guardada);
    }

    /**
     * Página 1 y 20 por página por defecto (máximo 100). Sin log por lectura: LOGGING_BACKEND.md no
     * pide registrar cada GET.
     */
    @PreAuthorize("hasRole('ADMIN_ESCUELA')")
    public Page<PreguntaResponse> listar(UUID escuelaCodigo, FiltroPreguntas filtro, Integer pagina, Integer tamano) {
        int numero = pagina == null || pagina < 1 ? 1 : pagina;
        int cantidad = tamano == null || tamano < 1 ? TAMANO_POR_DEFECTO : Math.min(tamano, TAMANO_MAXIMO);
        return preguntaRepository.listar(escuelaCodigo, filtro, PageRequest.of(numero - 1, cantidad))
            .map(PreguntaService::aRespuesta);
    }

    @PreAuthorize("hasRole('ADMIN_ESCUELA')")
    public PreguntaResponse obtener(UUID escuelaCodigo, UUID codigo) {
        return aRespuesta(buscar(escuelaCodigo, codigo));
    }

    /**
     * Cambia solo lo enviado. Si cambia el tipo o las opciones, se revalida el conjunto completo.
     */
    @PreAuthorize("hasRole('ADMIN_ESCUELA')")
    public PreguntaResponse editar(UUID escuelaCodigo, UUID codigo, EditarPreguntaRequest request) {
        Pregunta pregunta = buscar(escuelaCodigo, codigo);

        if (request.categoria() != null) {
            pregunta.setCategoria(ReglasPregunta.categoria(request.categoria()));
        }
        if (request.tema() != null) {
            pregunta.setTema(ReglasPregunta.texto(request.tema(), "tema", 2, 80));
        }
        if (request.enunciado() != null) {
            pregunta.setEnunciado(ReglasPregunta.texto(request.enunciado(), "enunciado", 5, 500));
        }
        if (request.explicacion() != null) {
            pregunta.setExplicacion(ReglasPregunta.explicacion(request.explicacion()));
        }
        if (request.dificultad() != null) {
            pregunta.setDificultad(ReglasPregunta.dificultad(request.dificultad()));
        }
        if (request.tipo() != null || request.opciones() != null) {
            var tipo = request.tipo() != null ? request.tipo() : pregunta.getTipo();
            var pedidas = request.opciones() != null
                ? request.opciones() : ReglasPregunta.comoPedidas(pregunta.getOpciones());
            pregunta.setTipo(tipo);
            pregunta.setOpciones(ReglasPregunta.opciones(tipo, pedidas));
        }

        Pregunta guardada = preguntaRepository.save(pregunta);
        LOG.info("Pregunta editada", kv("module", ReglasPregunta.MODULO), kv("preguntaCodigo", codigo));
        return aRespuesta(guardada);
    }

    /**
     * Activa o desactiva la pregunta. Pedir el estado que ya tiene es un conflicto (también si dos
     * peticiones llegan a la vez y la segunda no cambia nada).
     */
    @PreAuthorize("hasRole('ADMIN_ESCUELA')")
    public PreguntaResponse cambiarActiva(UUID escuelaCodigo, UUID codigo, boolean activa) {
        Pregunta pregunta = buscar(escuelaCodigo, codigo);
        if (pregunta.isActiva() == activa) {
            LOG.warn("Cambio de pregunta rechazado: ya estaba en ese estado", kv("module", ReglasPregunta.MODULO),
                kv("errorCode", AcademicoErrorCodes.PREGUNTA_YA_EN_ESE_ESTADO), kv("preguntaCodigo", codigo));
            throw new ConflictException(AcademicoErrorCodes.PREGUNTA_YA_EN_ESE_ESTADO,
                activa ? "La pregunta ya estaba activa" : "La pregunta ya estaba desactivada");
        }

        pregunta.setActiva(activa);
        Pregunta guardada = preguntaRepository.save(pregunta);
        LOG.info(activa ? "Pregunta activada" : "Pregunta desactivada", kv("module", ReglasPregunta.MODULO),
            kv("preguntaCodigo", codigo));
        return aRespuesta(guardada);
    }

    private Pregunta buscar(UUID escuelaCodigo, UUID codigo) {
        return preguntaRepository.findByEscuelaCodigoAndCodigoPreguntaAndDeletedAtIsNull(escuelaCodigo, codigo)
            .orElseThrow(() -> {
                LOG.warn("Pregunta no encontrada", kv("module", ReglasPregunta.MODULO),
                    kv("errorCode", AcademicoErrorCodes.PREGUNTA_NO_ENCONTRADA), kv("preguntaCodigo", codigo));
                return new NotFoundException(AcademicoErrorCodes.PREGUNTA_NO_ENCONTRADA, "La pregunta no existe");
            });
    }

    static PreguntaResponse aRespuesta(Pregunta p) {
        return new PreguntaResponse(p.getCodigoPregunta(), p.getCategoria(), p.getTema(), p.getTipo(), p.getEnunciado(),
            p.getOpciones().stream()
                .map(o -> new PreguntaResponse.OpcionResponse(o.id(), o.texto(), o.correcta())).toList(),
            p.getExplicacion(), p.getDificultad(), p.isActiva(), p.getCreatedAt());
    }
}
