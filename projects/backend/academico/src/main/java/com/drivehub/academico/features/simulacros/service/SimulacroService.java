package com.drivehub.academico.features.simulacros.service;

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
import com.drivehub.academico.features.simulacros.dto.CrearSimulacroRequest;
import com.drivehub.academico.features.simulacros.dto.EditarSimulacroRequest;
import com.drivehub.academico.features.simulacros.dto.SimulacroResponse;
import com.drivehub.academico.features.simulacros.model.SimulacroConfig;
import com.drivehub.academico.features.simulacros.repository.FiltroSimulacros;
import com.drivehub.academico.features.simulacros.repository.SimulacroConfigRepository;

import lombok.RequiredArgsConstructor;

/**
 * Lógica de la configuración de simulacros de una escuela (CU20). Única capa que loguea y lanza
 * excepciones de negocio. Todo se hace dentro de la escuela que llega de parámetro (la del token): un
 * simulacro de otra escuela es, para quien lo pide, uno que no existe.
 *
 * * Un simulacro no se borra: se desactiva (`activo = false`) y deja de ofrecerse a los estudiantes
 * * (CU21), pero los intentos ya rendidos conservan su copia.
 */
@Service
@RequiredArgsConstructor
public class SimulacroService {

    static final int TAMANO_POR_DEFECTO = 20;
    static final int TAMANO_MAXIMO = 100;

    private static final Logger LOG = LoggerFactory.getLogger(SimulacroService.class);

    private final SimulacroConfigRepository simulacroRepository;

    @PreAuthorize("hasRole('ADMIN_ESCUELA')")
    public SimulacroResponse crear(UUID escuelaCodigo, UUID autorCodigo, CrearSimulacroRequest request) {
        SimulacroConfig simulacro = new SimulacroConfig();
        simulacro.setEscuelaCodigo(escuelaCodigo);
        simulacro.setCreadoPorCodigo(autorCodigo);
        simulacro.setNombre(ReglasSimulacro.nombre(request.nombre()));
        simulacro.setCategoria(ReglasSimulacro.categoria(request.categoria()));
        simulacro.setDuracionMin(ReglasSimulacro.duracion(request.duracionMin()));
        int nroPreguntas = ReglasSimulacro.nroPreguntas(request.nroPreguntas());
        simulacro.setNroPreguntas(nroPreguntas);
        simulacro.setPuntajeAprobacion(ReglasSimulacro.puntaje(request.puntajeAprobacion()));
        simulacro.setReglas(ReglasSimulacro.reglas(request.reglas(), nroPreguntas));
        simulacro.setActivo(true);
        verificarNombreLibre(escuelaCodigo, simulacro.getNombre());

        SimulacroConfig guardado = simulacroRepository.save(simulacro);
        LOG.info("Simulacro creado", kv("module", ReglasSimulacro.MODULO),
            kv("simulacroCodigo", guardado.getCodigoSimulacro()), kv("escuelaCodigo", escuelaCodigo));
        return aRespuesta(guardado);
    }

    /**
     * Página 1 y 20 por página por defecto (máximo 100). Sin log por lectura: LOGGING_BACKEND.md no pide
     * registrar cada GET.
     */
    @PreAuthorize("hasRole('ADMIN_ESCUELA')")
    public Page<SimulacroResponse> listar(UUID escuelaCodigo, FiltroSimulacros filtro, Integer pagina,
            Integer tamano) {
        int numero = pagina == null || pagina < 1 ? 1 : pagina;
        int cantidad = tamano == null || tamano < 1 ? TAMANO_POR_DEFECTO : Math.min(tamano, TAMANO_MAXIMO);
        return simulacroRepository.listar(escuelaCodigo, filtro, PageRequest.of(numero - 1, cantidad))
            .map(SimulacroService::aRespuesta);
    }

    @PreAuthorize("hasRole('ADMIN_ESCUELA')")
    public SimulacroResponse obtener(UUID escuelaCodigo, UUID codigo) {
        return aRespuesta(buscar(escuelaCodigo, codigo));
    }

    /**
     * Cambia solo lo enviado. Si cambia el número de preguntas o las reglas, se revalida que sumen lo mismo
     * (cambiar solo el número revalida las reglas guardadas).
     */
    @PreAuthorize("hasRole('ADMIN_ESCUELA')")
    public SimulacroResponse editar(UUID escuelaCodigo, UUID codigo, EditarSimulacroRequest request) {
        SimulacroConfig simulacro = buscar(escuelaCodigo, codigo);

        if (request.nombre() != null) {
            String nombre = ReglasSimulacro.nombre(request.nombre());
            if (!nombre.equalsIgnoreCase(simulacro.getNombre())) {
                verificarNombreLibre(escuelaCodigo, nombre);
            }
            simulacro.setNombre(nombre);
        }
        if (request.categoria() != null) {
            simulacro.setCategoria(ReglasSimulacro.categoria(request.categoria()));
        }
        if (request.duracionMin() != null) {
            simulacro.setDuracionMin(ReglasSimulacro.duracion(request.duracionMin()));
        }
        if (request.puntajeAprobacion() != null) {
            simulacro.setPuntajeAprobacion(ReglasSimulacro.puntaje(request.puntajeAprobacion()));
        }
        if (request.nroPreguntas() != null || request.reglas() != null) {
            int nroPreguntas = request.nroPreguntas() != null
                ? ReglasSimulacro.nroPreguntas(request.nroPreguntas()) : simulacro.getNroPreguntas();
            var pedidas = request.reglas() != null
                ? request.reglas() : ReglasSimulacro.comoPedidas(simulacro.getReglas());
            simulacro.setNroPreguntas(nroPreguntas);
            simulacro.setReglas(ReglasSimulacro.reglas(pedidas, nroPreguntas));
        }

        SimulacroConfig guardado = simulacroRepository.save(simulacro);
        LOG.info("Simulacro editado", kv("module", ReglasSimulacro.MODULO), kv("simulacroCodigo", codigo));
        return aRespuesta(guardado);
    }

    /**
     * Activa o desactiva el simulacro. Pedir el estado que ya tiene es un conflicto (también si dos
     * peticiones llegan a la vez y la segunda no cambia nada).
     */
    @PreAuthorize("hasRole('ADMIN_ESCUELA')")
    public SimulacroResponse cambiarActivo(UUID escuelaCodigo, UUID codigo, boolean activo) {
        SimulacroConfig simulacro = buscar(escuelaCodigo, codigo);
        if (simulacro.isActivo() == activo) {
            LOG.warn("Cambio de simulacro rechazado: ya estaba en ese estado", kv("module", ReglasSimulacro.MODULO),
                kv("errorCode", AcademicoErrorCodes.SIMULACRO_YA_EN_ESE_ESTADO), kv("simulacroCodigo", codigo));
            throw new ConflictException(AcademicoErrorCodes.SIMULACRO_YA_EN_ESE_ESTADO,
                activo ? "El simulacro ya estaba activo" : "El simulacro ya estaba desactivado");
        }

        simulacro.setActivo(activo);
        SimulacroConfig guardado = simulacroRepository.save(simulacro);
        LOG.info(activo ? "Simulacro activado" : "Simulacro desactivado", kv("module", ReglasSimulacro.MODULO),
            kv("simulacroCodigo", codigo));
        return aRespuesta(guardado);
    }

    private void verificarNombreLibre(UUID escuelaCodigo, String nombre) {
        if (simulacroRepository.existsByEscuelaCodigoAndNombreIgnoreCaseAndDeletedAtIsNull(escuelaCodigo, nombre)) {
            LOG.warn("Simulacro rechazado: el nombre ya existe", kv("module", ReglasSimulacro.MODULO),
                kv("errorCode", AcademicoErrorCodes.SIMULACRO_NOMBRE_DUPLICADO));
            throw new ConflictException(AcademicoErrorCodes.SIMULACRO_NOMBRE_DUPLICADO,
                "Ya existe un simulacro con ese nombre");
        }
    }

    private SimulacroConfig buscar(UUID escuelaCodigo, UUID codigo) {
        return simulacroRepository.findByEscuelaCodigoAndCodigoSimulacroAndDeletedAtIsNull(escuelaCodigo, codigo)
            .orElseThrow(() -> {
                LOG.warn("Simulacro no encontrado", kv("module", ReglasSimulacro.MODULO),
                    kv("errorCode", AcademicoErrorCodes.SIMULACRO_NO_ENCONTRADO), kv("simulacroCodigo", codigo));
                return new NotFoundException(AcademicoErrorCodes.SIMULACRO_NO_ENCONTRADO, "El simulacro no existe");
            });
    }

    static SimulacroResponse aRespuesta(SimulacroConfig s) {
        return new SimulacroResponse(s.getCodigoSimulacro(), s.getNombre(), s.getCategoria(), s.getDuracionMin(),
            s.getNroPreguntas(), s.getPuntajeAprobacion(),
            s.getReglas().stream().map(r -> new SimulacroResponse.ReglaResponse(r.tema(), r.cantidad())).toList(),
            s.isActivo(), s.getCreatedAt());
    }
}
