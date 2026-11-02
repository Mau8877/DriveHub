package com.drivehub.academico.features.reservas.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.drivehub.academico.common.exception.AcademicoErrorCodes;
import com.drivehub.academico.common.exception.ConflictException;
import com.drivehub.academico.common.exception.ForbiddenException;
import com.drivehub.academico.common.exception.NotFoundException;
import com.drivehub.academico.common.exception.ServicioNoDisponibleException;
import com.drivehub.academico.common.exception.ValidationException;
import com.drivehub.academico.features.reservas.client.ClienteDePagos;
import com.drivehub.academico.features.reservas.client.IdentidadClient;
import com.drivehub.academico.features.reservas.dto.CancelacionInstructorResponse;
import com.drivehub.academico.features.reservas.dto.ClaseAgendaResponse;
import com.drivehub.academico.features.reservas.dto.HoraDeCancelacion;
import com.drivehub.academico.features.reservas.enums.EstadoReserva;
import com.drivehub.academico.features.reservas.model.Reserva;
import com.drivehub.academico.features.reservas.repository.ReservaRepository;

/**
 * La agenda del instructor (CU16): sus clases por día y la cancelación de una clase que no puede dar.
 *
 * * Ver la agenda no loguea (una lectura no se registra). Los nombres de los estudiantes salen de Identidad con el
 * * Bearer del instructor; si Identidad no responde no se muestra la agenda a medias (`ERR_ACA_17`).
 * * Cancelar reutiliza el camino de CU15: marca la reserva `CANCELACION_PENDIENTE` y la termina el
 * * {@link ManejadorDeCancelacion} (el mismo del proceso programado de CU44), que devuelve SIEMPRE la hora y deja la
 * * franja `BLOQUEADA`. El instructor no tiene plazo de anticipación: cancela hasta que la clase empieza.
 * * La hora se devuelve con la credencial de servicio y no con el Bearer del instructor: la billetera es del
 * * estudiante. Si Pagos no responde la cancelación SE ACEPTA y la hora se devuelve sola cuando vuelva.
 * ! No se loguean tokens ni datos de pago; solo códigos.
 */
@Service
public class AgendaService {

    static final String NOMBRE_SIN_DATOS = "Estudiante";
    static final int PAGE_SIZE_MAXIMO = 100;

    private static final Logger LOG = LoggerFactory.getLogger(AgendaService.class);
    private static final String MODULO = "reservas";

    private final ReservaRepository reservaRepository;
    private final IdentidadClient identidadClient;
    private final PlazoDeClase plazo;
    private final OrquestadorClases orquestador;
    private final ClienteDePagos clienteDeServicio;
    private final Clock clock;

    public AgendaService(ReservaRepository reservaRepository, IdentidadClient identidadClient, PlazoDeClase plazo,
            OrquestadorClases orquestador, @Qualifier("clienteDeServicio") ClienteDePagos clienteDeServicio,
            Clock clock) {
        this.reservaRepository = reservaRepository;
        this.identidadClient = identidadClient;
        this.plazo = plazo;
        this.orquestador = orquestador;
        this.clienteDeServicio = clienteDeServicio;
        this.clock = clock;
    }

    /**
     * Las clases del instructor en un día, por hora de inicio, con el nombre de cada estudiante.
     *
     * @param fecha `yyyy-MM-dd`; nula o vacía es hoy en la zona de la escuela
     * @throws ForbiddenException `ERR_ACA_16` si el token no trae la escuela
     * @throws ValidationException `ERR_SYS_01` si la fecha no es válida
     * @throws ServicioNoDisponibleException `ERR_ACA_17` si Identidad no entrega los nombres
     */
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public Page<ClaseAgendaResponse> agenda(UUID instructorCodigo, UUID escuelaCodigo, String bearer, String fecha,
            int page, int pageSize) {
        exigirEscuela(escuelaCodigo);
        LocalDateTime ahora = LocalDateTime.now(clock);
        LocalDate dia = fechaDe(fecha, ahora.toLocalDate());
        Pageable pagina = PageRequest.of(Math.max(page, 1) - 1, Math.min(Math.max(pageSize, 1), PAGE_SIZE_MAXIMO));

        Page<Reserva> clases = reservaRepository.agendaDeInstructor(instructorCodigo, dia, pagina);

        List<UUID> estudiantes = clases.getContent().stream().map(Reserva::getEstudianteCodigo).distinct().toList();
        Map<UUID, String> nombres = estudiantes.isEmpty() ? Map.of()
            : identidadClient.nombresDeEstudiantes(estudiantes, bearer);
        return clases.map(c -> aRespuesta(c, nombres, ahora));
    }

    /**
     * Cancela una clase del instructor. Siempre se devuelve la hora al estudiante y la franja queda `BLOQUEADA`.
     *
     * @throws ForbiddenException `ERR_ACA_16` si el token no trae la escuela
     * @throws NotFoundException `ERR_ACA_35` si la clase no existe o es de otro instructor
     * @throws ConflictException `ERR_ACA_36` si no está `CONFIRMADA`, ya empezó o otra petición la cambió antes
     */
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public CancelacionInstructorResponse cancelar(UUID instructorCodigo, UUID escuelaCodigo, String bearer,
            UUID codigoReserva) {
        exigirEscuela(escuelaCodigo);
        Reserva reserva = reservaRepository
            .findByCodigoReservaAndInstructorCodigoAndDeletedAtIsNull(codigoReserva, instructorCodigo)
            .orElseThrow(() -> new NotFoundException(AcademicoErrorCodes.CLASE_INSTRUCTOR_NO_ENCONTRADA,
                "No encontramos esa clase"));
        if (reserva.getEstado() != EstadoReserva.CONFIRMADA || plazo.haEmpezado(reserva)) {
            throw claseNoCancelable();
        }
        Reserva marcada = reservaRepository.iniciarCancelacionPorInstructor(codigoReserva, instructorCodigo,
            ManejadorDeCancelacion.MOTIVO_INSTRUCTOR, clock.instant()).orElseThrow(AgendaService::claseNoCancelable);
        LOG.info("Cancelación por el instructor pedida", kv("module", MODULO),
            kv("instructorCodigo", instructorCodigo), kv("reservaCodigo", codigoReserva));

        ResultadoOrquestacion resultado = terminarCancelacion(marcada);
        HoraDeCancelacion hora = resultado == ResultadoOrquestacion.CANCELADA ? HoraDeCancelacion.DEVUELTA
            : HoraDeCancelacion.EN_PROCESO;
        if (hora == HoraDeCancelacion.EN_PROCESO) {
            LOG.warn("La hora del estudiante se devolverá después de la cancelación del instructor",
                kv("module", MODULO), kv("reservaCodigo", codigoReserva), kv("resultado", resultado));
        }
        Reserva actual = reservaRepository
            .findByCodigoReservaAndInstructorCodigoAndDeletedAtIsNull(codigoReserva, instructorCodigo)
            .orElse(marcada);
        return new CancelacionInstructorResponse(respuestaConNombre(actual, bearer), hora);
    }

    private static void exigirEscuela(UUID escuelaCodigo) {
        if (escuelaCodigo == null) {
            throw new ForbiddenException(AcademicoErrorCodes.TOKEN_SIN_ESCUELA,
                "Tu cuenta no pertenece a ninguna escuela");
        }
    }

    private static LocalDate fechaDe(String fecha, LocalDate hoy) {
        if (fecha == null || fecha.isBlank()) {
            return hoy;
        }
        try {
            return LocalDate.parse(fecha.trim());
        } catch (DateTimeParseException e) {
            throw new ValidationException(AcademicoErrorCodes.VALIDACION, "La fecha debe tener el formato aaaa-mm-dd");
        }
    }

    /**
     * Termina la cancelación con el manejador y la credencial de servicio. La cancelación ya está registrada: un fallo
     * inesperado acá no la deshace, queda `CANCELACION_PENDIENTE` y el proceso programado la termina.
     */
    private ResultadoOrquestacion terminarCancelacion(Reserva marcada) {
        try {
            return orquestador.resolver(marcada, clienteDeServicio);
        } catch (RuntimeException e) {
            LOG.warn("No se pudo terminar la cancelación: la retomará el sistema", kv("module", MODULO),
                kv("reservaCodigo", marcada.getCodigoReserva()), kv("tipoError", e.getClass().getSimpleName()));
            return ResultadoOrquestacion.PENDIENTE;
        }
    }

    /** La cancelación ya está hecha: si Identidad no responde el nombre del estudiante se muestra como «Estudiante». */
    private ClaseAgendaResponse respuestaConNombre(Reserva reserva, String bearer) {
        Map<UUID, String> nombres;
        try {
            nombres = identidadClient.nombresDeEstudiantes(List.of(reserva.getEstudianteCodigo()), bearer);
        } catch (ServicioNoDisponibleException e) {
            nombres = Map.of();
        }
        return aRespuesta(reserva, nombres, LocalDateTime.now(clock));
    }

    private ClaseAgendaResponse aRespuesta(Reserva reserva, Map<UUID, String> nombres, LocalDateTime ahora) {
        boolean cancelable = reserva.getEstado() == EstadoReserva.CONFIRMADA && !plazo.haEmpezado(reserva);
        return ClaseAgendaResponse.de(reserva, nombres.getOrDefault(reserva.getEstudianteCodigo(), NOMBRE_SIN_DATOS),
            ReservaService.terminada(reserva, ahora), cancelable);
    }

    private static ConflictException claseNoCancelable() {
        return new ConflictException(AcademicoErrorCodes.CLASE_NO_CANCELABLE_POR_INSTRUCTOR,
            "Esa clase ya no se puede cancelar");
    }
}
