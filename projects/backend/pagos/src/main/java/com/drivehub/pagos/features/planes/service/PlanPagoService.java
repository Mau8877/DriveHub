package com.drivehub.pagos.features.planes.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.pagos.common.exception.ConflictException;
import com.drivehub.pagos.common.exception.NotFoundException;
import com.drivehub.pagos.common.exception.PagosErrorCodes;
import com.drivehub.pagos.common.exception.ValidationException;
import com.drivehub.pagos.features.planes.dto.CrearPlanPagoRequest;
import com.drivehub.pagos.features.planes.dto.EditarPlanPagoRequest;
import com.drivehub.pagos.features.planes.dto.PlanPagoResponse;
import com.drivehub.pagos.features.planes.model.PlanPago;
import com.drivehub.pagos.features.planes.repository.PlanPagoRepository;
import com.drivehub.pagos.features.planes.repository.PlanPagoSpecification;

import lombok.RequiredArgsConstructor;

/**
 * Planes de horas de la escuela (CU25a): crear, listar, obtener, editar, desactivar y activar.
 *
 * * La escuela es siempre la del token (la pasa el controller): un plan de otra escuela responde "no
 * * encontrado", no "prohibido", para no revelar que existe.
 * * El nombre es único por escuela sin distinguir mayúsculas, incluidos los desactivados: el service lo
 * * comprueba para dar un mensaje claro y el índice único de la base cierra la carrera entre dos altas.
 * * Desactivar no borra: el plan conserva su historia y se puede reactivar.
 * * Cambiar un plan no toca lo que ya contrataron los estudiantes (cada inscripción guarda su monto).
 * ! No se loguean el nombre ni el costo; solo códigos.
 */
@Service
@RequiredArgsConstructor
public class PlanPagoService {

    static final int PAGE_SIZE_POR_DEFECTO = 20;
    static final int PAGE_SIZE_MAXIMO = 100;

    private static final Logger LOG = LoggerFactory.getLogger(PlanPagoService.class);
    private static final String MODULO = "planes";
    private static final String INDICE_NOMBRE_UNICO = "uq_plan_pago_escuela_nombre";
    private static final Sort MAS_RECIENTE_PRIMERO = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final PlanPagoRepository planRepository;
    private final ConversionMoneda conversion;

    @PreAuthorize("hasRole('ADMIN_ESCUELA')")
    @Transactional
    public PlanPagoResponse crear(UUID escuelaCodigo, CrearPlanPagoRequest request) {
        String nombre = request.nombre().trim();
        if (planRepository.existsByEscuelaCodigoAndNombreIgnoreCase(escuelaCodigo, nombre)) {
            throw nombreEnUso();
        }
        PlanPago plan = new PlanPago(escuelaCodigo, nombre, request.horas().shortValue(), request.costoBs());
        PlanPago creado = guardar(plan);

        LOG.info("Plan de horas creado", kv("module", MODULO), kv("planCodigo", creado.getCodigo()),
            kv("escuelaCodigo", escuelaCodigo));
        return aRespuesta(creado);
    }

    /**
     * Sin `page`, la 1; sin `pageSize`, 20 y nunca más de 100. La búsqueda se recorta y en blanco no filtra.
     * Va del más reciente al más antiguo. `activo` nulo trae activos y desactivados.
     */
    @PreAuthorize("hasRole('ADMIN_ESCUELA')")
    @Transactional(readOnly = true)
    public Page<PlanPagoResponse> listar(UUID escuelaCodigo, Integer page, Integer pageSize, String search,
            Boolean activo) {
        int pagina = page == null || page < 1 ? 1 : page;
        int tamano = pageSize == null ? PAGE_SIZE_POR_DEFECTO : Math.min(Math.max(pageSize, 1), PAGE_SIZE_MAXIMO);
        Specification<PlanPago> filtro = Specification.allOf(PlanPagoSpecification.deLaEscuela(escuelaCodigo),
            PlanPagoSpecification.nombreContiene(search), PlanPagoSpecification.conEstado(activo));
        return planRepository.findAll(filtro, PageRequest.of(pagina - 1, tamano, MAS_RECIENTE_PRIMERO))
            .map(this::aRespuesta);
    }

    @PreAuthorize("hasRole('ADMIN_ESCUELA')")
    @Transactional(readOnly = true)
    public PlanPagoResponse obtener(UUID escuelaCodigo, UUID codigo) {
        return aRespuesta(buscar(escuelaCodigo, codigo));
    }

    /**
     * PATCH parcial: cambia solo lo que llega y no toca el estado. Un cuerpo sin ningún campo es un error.
     */
    @PreAuthorize("hasRole('ADMIN_ESCUELA')")
    @Transactional
    public PlanPagoResponse editar(UUID escuelaCodigo, UUID codigo, EditarPlanPagoRequest request) {
        if (request.nombre() == null && request.horas() == null && request.costoBs() == null) {
            throw new ValidationException(PagosErrorCodes.VALIDACION, "Indica al menos un dato para cambiar");
        }
        PlanPago plan = buscar(escuelaCodigo, codigo);

        if (request.nombre() != null) {
            String nombre = request.nombre().trim();
            if (nombre.isEmpty()) {
                throw new ValidationException(PagosErrorCodes.VALIDACION, "El nombre no puede estar vacío");
            }
            if (planRepository.existsByEscuelaCodigoAndNombreIgnoreCaseAndCodigoNot(escuelaCodigo, nombre, codigo)) {
                throw nombreEnUso();
            }
            plan.setNombre(nombre);
        }
        if (request.horas() != null) {
            plan.setHoras(request.horas().shortValue());
        }
        if (request.costoBs() != null) {
            plan.setCostoBs(request.costoBs());
        }
        PlanPago actualizado = guardar(plan);

        LOG.info("Plan de horas actualizado", kv("module", MODULO), kv("planCodigo", codigo),
            kv("escuelaCodigo", escuelaCodigo));
        return aRespuesta(actualizado);
    }

    @PreAuthorize("hasRole('ADMIN_ESCUELA')")
    @Transactional
    public PlanPagoResponse desactivar(UUID escuelaCodigo, UUID codigo) {
        return cambiarActivo(escuelaCodigo, codigo, false, "Plan de horas desactivado");
    }

    @PreAuthorize("hasRole('ADMIN_ESCUELA')")
    @Transactional
    public PlanPagoResponse activar(UUID escuelaCodigo, UUID codigo) {
        return cambiarActivo(escuelaCodigo, codigo, true, "Plan de horas reactivado");
    }

    private PlanPagoResponse cambiarActivo(UUID escuelaCodigo, UUID codigo, boolean activo, String mensajeLog) {
        PlanPago plan = buscar(escuelaCodigo, codigo);
        if (plan.isActivo() == activo) {
            throw new ConflictException(PagosErrorCodes.PLAN_YA_EN_ESE_ESTADO,
                activo ? "El plan ya está activo" : "El plan ya está desactivado");
        }
        plan.setActivo(activo);
        PlanPago guardado = planRepository.save(plan);

        LOG.info(mensajeLog, kv("module", MODULO), kv("planCodigo", codigo), kv("escuelaCodigo", escuelaCodigo));
        return aRespuesta(guardado);
    }

    private PlanPago buscar(UUID escuelaCodigo, UUID codigo) {
        return planRepository.findByCodigoAndEscuelaCodigo(codigo, escuelaCodigo)
            .orElseThrow(() -> new NotFoundException(PagosErrorCodes.PLAN_NO_ENCONTRADO, "El plan no existe"));
    }

    /**
     * Guarda y fuerza la escritura, para que una violación del índice único (dos altas simultáneas con el
     * mismo nombre) salte acá y se traduzca a `ERR_PAG_01`. Otra violación de integridad no se disfraza.
     */
    private PlanPago guardar(PlanPago plan) {
        try {
            return planRepository.saveAndFlush(plan);
        } catch (DataIntegrityViolationException e) {
            if (esChoqueDeNombre(e)) {
                throw nombreEnUso();
            }
            throw e;
        }
    }

    private static boolean esChoqueDeNombre(DataIntegrityViolationException e) {
        Throwable causa = e.getMostSpecificCause();
        return causa != null && causa.getMessage() != null && causa.getMessage().contains(INDICE_NOMBRE_UNICO);
    }

    private static ConflictException nombreEnUso() {
        return new ConflictException(PagosErrorCodes.PLAN_NOMBRE_EN_USO, "Ya existe un plan con ese nombre");
    }

    private PlanPagoResponse aRespuesta(PlanPago plan) {
        return new PlanPagoResponse(plan.getCodigo(), plan.getNombre(), plan.getHoras(), plan.getCostoBs(),
            conversion.aDolares(plan.getCostoBs()), plan.isActivo(), plan.getCreatedAt());
    }
}
