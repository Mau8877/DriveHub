package com.drivehub.academico.features.clases.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.drivehub.academico.common.exception.AcademicoErrorCodes;
import com.drivehub.academico.common.exception.ConflictException;
import com.drivehub.academico.common.exception.ForbiddenException;
import com.drivehub.academico.common.exception.NotFoundException;
import com.drivehub.academico.common.exception.ServicioNoDisponibleException;
import com.drivehub.academico.features.clases.dto.ActualizarClaseRequest;
import com.drivehub.academico.features.clases.dto.CerrarClaseRequest;
import com.drivehub.academico.features.clases.dto.ClaseResponse;
import com.drivehub.academico.features.clases.enums.EstadoClase;
import com.drivehub.academico.features.clases.model.Clase;
import com.drivehub.academico.features.clases.repository.ClaseRepository;
import com.drivehub.academico.features.reservas.client.IdentidadClient;
import com.drivehub.academico.features.reservas.model.Reserva;
import com.drivehub.academico.features.reservas.service.CierreDeReservaService;

import lombok.RequiredArgsConstructor;

/**
 * Las clases dictadas del instructor (CU17): cerrar una clase que ya empezó, verla y corregir sus datos.
 *
 * * Cerrar guarda la clase y después marca la reserva `COMPLETADA` o `NO_ASISTIO` (Mongo no da una transacción entre
 * * los dos documentos): si otra petición cambió la reserva antes, la clase recién creada se borra y se responde
 * * `ERR_ACA_37`; si el servicio se cayó entre los dos pasos, cerrar de nuevo reutiliza la clase ya guardada.
 * * Cerrar no llama a Pagos: la hora ya se descontó al reservar y nunca se devuelve.
 * * El nombre del estudiante sale de Identidad; si no responde, la respuesta dice «Estudiante» y no falla, porque el
 * * cierre o la corrección ya están hechos.
 * ! Nunca se loguea el texto del tema ni de las observaciones; solo códigos y los nombres de los campos cambiados.
 */
@Service
@RequiredArgsConstructor
public class ClaseService {

    static final String NOMBRE_SIN_DATOS = "Estudiante";

    private static final Logger LOG = LoggerFactory.getLogger(ClaseService.class);
    private static final String MODULO = "clases";

    private final ClaseRepository claseRepository;
    private final CierreDeReservaService cierre;
    private final IdentidadClient identidadClient;

    /**
     * Cierra la clase de una reserva del instructor que ya empezó.
     *
     * @throws ForbiddenException `ERR_ACA_16` si el token no trae la escuela
     * @throws com.drivehub.academico.common.exception.ValidationException `ERR_ACA_38` si los datos no cumplen las
     *         reglas de negocio
     * @throws NotFoundException `ERR_ACA_35` si la reserva no existe o es de otro instructor
     * @throws ConflictException `ERR_ACA_37` si la reserva no se puede cerrar o cambió mientras se cerraba
     */
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ClaseResponse cerrar(UUID instructorCodigo, UUID escuelaCodigo, String bearer, CerrarClaseRequest request) {
        exigirEscuela(escuelaCodigo);
        ReglasDeClase.validarCierre(request.asistencia(), request.tema(), request.kmInicio(), request.kmFin());
        Reserva reserva = cierre.buscarCerrable(instructorCodigo, request.reservaCodigo());

        // * Si ya hay una clase de esta reserva, un cierre anterior se cortó a medias: se reutiliza
        Optional<Clase> previa = claseRepository.findByReservaIdAndDeletedAtIsNull(reserva.getId());
        boolean creadaAhora = previa.isEmpty();
        Clase clase = previa.orElseGet(() -> new Clase(reserva.getId(), reserva.getEstudianteCodigo(),
            instructorCodigo, reserva.getFecha(), request.asistencia()));
        clase.setAsistencia(request.asistencia());
        clase.setTema(ReglasDeClase.limpiar(request.tema()));
        clase.setObservaciones(ReglasDeClase.limpiar(request.observaciones()));
        clase.setKmInicio(request.kmInicio());
        clase.setKmFin(request.kmFin());
        clase.setEstado(EstadoClase.CERRADA);
        Clase guardada = guardar(clase);

        if (cierre.marcarCerrada(instructorCodigo, request.reservaCodigo(), request.asistencia()).isEmpty()) {
            if (creadaAhora) {
                claseRepository.delete(guardada);
            }
            LOG.warn("La reserva cambió mientras se cerraba la clase", kv("module", MODULO),
                kv("reservaCodigo", request.reservaCodigo()), kv("instructorCodigo", instructorCodigo),
                kv("claseBorrada", creadaAhora));
            throw claseNoCerrable();
        }
        LOG.info(creadaAhora ? "Clase cerrada" : "Cierre de clase retomado", kv("module", MODULO),
            kv("claseCodigo", guardada.getCodigoClase()), kv("reservaCodigo", request.reservaCodigo()),
            kv("instructorCodigo", instructorCodigo), kv("asistencia", request.asistencia()));
        return ClaseResponse.de(guardada, nombreDe(guardada.getEstudianteCodigo(), bearer));
    }

    /**
     * Una clase cerrada del instructor.
     *
     * @throws ForbiddenException `ERR_ACA_16` si el token no trae la escuela
     * @throws NotFoundException `ERR_ACA_35` si la clase no existe o es de otro instructor
     */
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ClaseResponse obtener(UUID instructorCodigo, UUID escuelaCodigo, String bearer, UUID codigoClase) {
        exigirEscuela(escuelaCodigo);
        Clase clase = buscar(instructorCodigo, codigoClase);
        return ClaseResponse.de(clase, nombreDe(clase.getEstudianteCodigo(), bearer));
    }

    /**
     * Corrige el tema, las observaciones y los kilómetros de una clase cerrada. Lo que no se envía no se toca; un
     * texto en blanco lo borra. La asistencia no se cambia.
     *
     * @throws ForbiddenException `ERR_ACA_16` si el token no trae la escuela
     * @throws NotFoundException `ERR_ACA_35` si la clase no existe o es de otro instructor
     * @throws com.drivehub.academico.common.exception.ValidationException `ERR_ACA_38` si los datos no cumplen las
     *         reglas de negocio
     */
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ClaseResponse actualizar(UUID instructorCodigo, UUID escuelaCodigo, String bearer, UUID codigoClase,
            ActualizarClaseRequest request) {
        exigirEscuela(escuelaCodigo);
        Clase clase = buscar(instructorCodigo, codigoClase);
        ReglasDeClase.validarCorreccion(clase, request);

        List<String> cambiados = new ArrayList<>();
        if (request.tema() != null) {
            clase.setTema(ReglasDeClase.limpiar(request.tema()));
            cambiados.add("tema");
        }
        if (request.observaciones() != null) {
            clase.setObservaciones(ReglasDeClase.limpiar(request.observaciones()));
            cambiados.add("observaciones");
        }
        if (request.kmInicio() != null) {
            clase.setKmInicio(request.kmInicio());
            cambiados.add("kmInicio");
        }
        if (request.kmFin() != null) {
            clase.setKmFin(request.kmFin());
            cambiados.add("kmFin");
        }
        Clase actual = clase;
        if (!cambiados.isEmpty()) {
            actual = claseRepository.save(clase);
            LOG.info("Clase corregida", kv("module", MODULO), kv("claseCodigo", codigoClase),
                kv("instructorCodigo", instructorCodigo), kv("campos", cambiados));
        }
        return ClaseResponse.de(actual, nombreDe(actual.getEstudianteCodigo(), bearer));
    }

    /**
     * El código de la clase dictada de cada reserva (`_id` de la reserva), con una sola consulta, para la agenda.
     * Sin `@PreAuthorize`: lo llama la agenda, que ya exige el rol.
     */
    public Map<Long, UUID> codigosPorReserva(Collection<Long> reservaIds) {
        Map<Long, UUID> codigos = new HashMap<>();
        if (reservaIds.isEmpty()) {
            return codigos;
        }
        claseRepository.findByReservaIdInAndDeletedAtIsNull(reservaIds)
            .forEach(c -> codigos.put(c.getReservaId(), c.getCodigoClase()));
        return codigos;
    }

    private Clase buscar(UUID instructorCodigo, UUID codigoClase) {
        return claseRepository.findByCodigoClaseAndInstructorCodigoAndDeletedAtIsNull(codigoClase, instructorCodigo)
            .orElseThrow(() -> new NotFoundException(AcademicoErrorCodes.CLASE_INSTRUCTOR_NO_ENCONTRADA,
                "No encontramos esa clase"));
    }

    /** Un `DuplicateKeyException` es otra petición que creó la clase de esta reserva a la vez: es un conflicto. */
    private Clase guardar(Clase clase) {
        try {
            return claseRepository.save(clase);
        } catch (DuplicateKeyException e) {
            throw claseNoCerrable();
        }
    }

    private String nombreDe(UUID estudianteCodigo, String bearer) {
        try {
            return identidadClient.nombresDeEstudiantes(List.of(estudianteCodigo), bearer)
                .getOrDefault(estudianteCodigo, NOMBRE_SIN_DATOS);
        } catch (ServicioNoDisponibleException e) {
            return NOMBRE_SIN_DATOS;
        }
    }

    private static void exigirEscuela(UUID escuelaCodigo) {
        if (escuelaCodigo == null) {
            throw new ForbiddenException(AcademicoErrorCodes.TOKEN_SIN_ESCUELA,
                "Tu cuenta no pertenece a ninguna escuela");
        }
    }

    private static ConflictException claseNoCerrable() {
        return new ConflictException(AcademicoErrorCodes.CLASE_NO_CERRABLE, "Esa clase no se puede cerrar");
    }
}
