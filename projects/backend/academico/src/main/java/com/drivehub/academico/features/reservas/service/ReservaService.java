package com.drivehub.academico.features.reservas.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.drivehub.academico.common.exception.AcademicoErrorCodes;
import com.drivehub.academico.common.exception.BusinessException;
import com.drivehub.academico.common.exception.ConflictException;
import com.drivehub.academico.common.exception.ForbiddenException;
import com.drivehub.academico.common.exception.ServicioNoDisponibleException;
import com.drivehub.academico.common.exception.ValidationException;
import com.drivehub.academico.config.ReservasProperties;
import com.drivehub.academico.features.horarios.model.Franja;
import com.drivehub.academico.features.horarios.repository.FranjaRepository;
import com.drivehub.academico.features.reservas.client.ClienteDePagos;
import com.drivehub.academico.features.reservas.client.ClientesDePagos;
import com.drivehub.academico.features.reservas.client.IdentidadClient;
import com.drivehub.academico.features.reservas.client.PagosClient;
import com.drivehub.academico.features.reservas.dto.FranjaRechazadaResponse;
import com.drivehub.academico.features.reservas.dto.ReservaResponse;
import com.drivehub.academico.features.reservas.dto.ReservasResultadoResponse;
import com.drivehub.academico.features.reservas.enums.EstadoReserva;
import com.drivehub.academico.features.reservas.model.Reserva;
import com.drivehub.academico.features.reservas.repository.ReservaRepository;
import com.drivehub.academico.features.reservas.service.OrquestadorClases.ReservaCompletada;
import com.drivehub.academico.features.reservas.service.OrquestadorClases.ResultadoReserva;

import lombok.RequiredArgsConstructor;

/**
 * Las reservas del estudiante (CU14): confirmar varias franjas gastando una hora de la billetera por cada una.
 *
 * * Es una saga coordinada por Académico, franja por franja: (1) crear la reserva `PENDIENTE_PAGO`, (2) apartar
 * * la franja (`LIBRE` a `RESERVADA`, atómico), (3) y (4) descontar la hora y confirmar o compensar, que hace
 * * {@link OrquestadorClases} (CU44), el mismo que usa el proceso programado.
 * * La reserva es el registro del estado de la saga.
 * * Cada franja se procesa por separado: una que falla no frena a las demás, y se informa con su motivo.
 * * No hay transacciones entre documentos ni entre servicios: cada paso es repetible. Lo que queda a medias por
 * * una caída (la reserva sigue `PENDIENTE_PAGO`) lo retoma el orquestador al consultar sus clases o el proceso
 * * programado.
 * * La clase (CU17) se crea después a partir de la reserva: por eso el código de la reserva es el `claseCodigo` de
 * * la billetera, y CU15 devolverá la hora con ese mismo código.
 * ! No se loguean tokens ni datos de pago; solo códigos.
 */
@Service
@RequiredArgsConstructor
public class ReservaService {

    static final String NOMBRE_SIN_DATOS = "Instructor";
    static final int PAGE_SIZE_MAXIMO = 100;

    private static final Logger LOG = LoggerFactory.getLogger(ReservaService.class);
    private static final String MODULO = "reservas";
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final ReservaRepository reservaRepository;
    private final FranjaRepository franjaRepository;
    private final PagosClient pagosClient;
    private final ClientesDePagos clientesDePagos;
    private final OrquestadorClases orquestador;
    private final IdentidadClient identidadClient;
    private final ReservasProperties propiedades;
    private final Clock clock;

    /**
     * Intenta reservar cada franja pedida, en el orden recibido.
     *
     * @param bearer la cabecera `Authorization` completa del estudiante, que se reenvía a Pagos
     * @throws ValidationException `ERR_SYS_01` si no hay franjas, hay más del máximo o alguna se repite
     * @throws ForbiddenException `ERR_ACA_16` si el token no trae la escuela
     */
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ReservasResultadoResponse reservar(UUID estudianteCodigo, UUID escuelaCodigo, String bearer,
            List<UUID> franjaCodigos) {
        ReservasResultado resultado = procesarFranjas(estudianteCodigo, escuelaCodigo, bearer, franjaCodigos);
        Map<UUID, String> nombres = nombresDe(resultado, bearer);
        LocalDateTime ahora = LocalDateTime.now(clock);

        List<ReservaResponse> reservadas = resultado.reservadas().stream()
            .map(r -> aRespuesta(r, nombres, ahora)).toList();
        List<FranjaRechazadaResponse> rechazadas = resultado.rechazadas().stream()
            .map(r -> new FranjaRechazadaResponse(r.franjaCodigo(), r.fecha(), r.horaInicio(), r.horaFin(),
                r.instructorCodigo() == null ? null : nombres.getOrDefault(r.instructorCodigo(), NOMBRE_SIN_DATOS),
                r.error(), r.mensaje()))
            .toList();
        return new ReservasResultadoResponse(reservadas, rechazadas, resultado.horasDisponibles());
    }

    /**
     * Las clases del estudiante: `PROXIMAS` (confirmadas que todavía no terminaron, de la más cercana a la más
     * lejana) o `HISTORIAL` (cerradas, canceladas y confirmadas ya terminadas, de la más reciente a la más antigua).
     * Una `PENDIENTE_PAGO` no se muestra en ninguna. Nunca devuelve reservas de otro estudiante.
     *
     * @throws ValidationException `ERR_SYS_01` si la vista no es `proximas` ni `historial`
     * @throws ServicioNoDisponibleException `ERR_ACA_17` si Identidad no entrega los nombres (no se muestra a medias)
     */
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public Page<ReservaResponse> listar(UUID estudianteCodigo, String bearer, String vista, int page, int pageSize) {
        VistaReservas elegida = VistaReservas.de(vista);
        retomarPendientes(estudianteCodigo, bearer);
        LocalDateTime ahora = LocalDateTime.now(clock);
        Pageable pagina = PageRequest.of(Math.max(page, 1) - 1, Math.min(Math.max(pageSize, 1), PAGE_SIZE_MAXIMO));

        String horaActual = ahora.toLocalTime().format(HORA);
        Page<Reserva> reservas = elegida == VistaReservas.PROXIMAS
            ? reservaRepository.proximas(estudianteCodigo, ahora.toLocalDate(), horaActual, pagina)
            : reservaRepository.historial(estudianteCodigo, ahora.toLocalDate(), horaActual, pagina);

        List<UUID> instructores = reservas.getContent().stream().map(Reserva::getInstructorCodigo).distinct().toList();
        Map<UUID, String> nombres = instructores.isEmpty() ? Map.of()
            : identidadClient.nombresDeInstructores(instructores, bearer);
        return reservas.map(r -> aRespuesta(r, nombres, ahora));
    }

    // ------------------------------------------------------------------ reservas a medias

    /**
     * Retoma las reservas `PENDIENTE_PAGO` de este estudiante que llevan más de `minutosParaRetomar` así: la franja
     * se apartó pero la confirmación no terminó (corte de red, caída del servicio).
     *
     * * Es la misma resolución que hace el proceso programado (CU44): `OrquestadorClases.resolver`, pero con el
     * * Bearer del estudiante. Completa si es reciente, deshace si es vieja o si la franja ya no es suya.
     * * Si Pagos no responde no se toca nada: se reintenta la próxima vez.
     * * Un fallo retomando una reserva nunca hace fallar el listado ni frena a las demás.
     */
    private void retomarPendientes(UUID estudianteCodigo, String bearer) {
        Instant limite = clock.instant().minus(Duration.ofMinutes(propiedades.minutosParaRetomar()));
        ClienteDePagos cliente = clientesDePagos.conBearer(bearer);
        for (Reserva pendiente : reservaRepository.pendientesDeEstudiante(estudianteCodigo, limite)) {
            try {
                orquestador.resolver(pendiente, cliente);
            } catch (RuntimeException e) {
                LOG.warn("No se pudo retomar una reserva a medias", kv("module", MODULO),
                    kv("reservaCodigo", pendiente.getCodigoReserva()), kv("tipoError", e.getClass().getSimpleName()));
            }
        }
    }

    private ReservasResultado procesarFranjas(UUID estudianteCodigo, UUID escuelaCodigo, String bearer,
            List<UUID> franjaCodigos) {
        if (escuelaCodigo == null) {
            throw new ForbiddenException(AcademicoErrorCodes.TOKEN_SIN_ESCUELA,
                "Tu cuenta no pertenece a ninguna escuela");
        }
        validarPeticion(franjaCodigos);

        List<Reserva> reservadas = new ArrayList<>();
        List<FranjaRechazada> rechazadas = new ArrayList<>();
        for (UUID franjaCodigo : franjaCodigos) {
            Franja franja = franjaRepository
                .findByCodigoFranjaAndEscuelaCodigoAndDeletedAtIsNull(franjaCodigo, escuelaCodigo).orElse(null);
            try {
                reservadas.add(procesar(estudianteCodigo, escuelaCodigo, bearer, franjaCodigo, franja));
            } catch (BusinessException e) {
                LOG.warn("Franja no reservada", kv("module", MODULO), kv("estudianteCodigo", estudianteCodigo),
                    kv("franjaCodigo", franjaCodigo), kv("errorCode", e.getErrorCode()));
                rechazadas.add(rechazo(franjaCodigo, franja, e));
            }
        }
        return new ReservasResultado(reservadas, rechazadas, saldoDe(bearer));
    }

    // ------------------------------------------------------------------ una franja

    private Reserva procesar(UUID estudianteCodigo, UUID escuelaCodigo, String bearer, UUID franjaCodigo,
            Franja franja) {
        if (franja == null) {
            throw franjaNoDisponible();
        }
        LocalDateTime ahora = LocalDateTime.now(clock);
        LocalDate hoy = ahora.toLocalDate();
        String horaActual = ahora.toLocalTime().format(HORA);
        if (franja.getFecha().isBefore(hoy)
                || (franja.getFecha().equals(hoy) && franja.getHoraInicio().compareTo(horaActual) <= 0)) {
            throw new ConflictException(AcademicoErrorCodes.FRANJA_YA_EMPEZO, "La clase ya empezó");
        }

        // * Repetir la confirmación de una franja que ya es del estudiante no hace nada y cuenta como reservada
        Optional<Reserva> yaSuya = reservaRepository.findByFranjaIdAndEstudianteCodigoAndEstadoAndDeletedAtIsNull(
            franja.getId(), estudianteCodigo, EstadoReserva.CONFIRMADA);
        if (yaSuya.isPresent()) {
            return yaSuya.get();
        }
        if (reservaRepository.existeSolape(estudianteCodigo, franja.getFecha(), franja.getHoraInicio())) {
            throw new ConflictException(AcademicoErrorCodes.CLASE_SOLAPADA, "Ya tienes una clase reservada a esa hora");
        }

        Reserva reserva = reservaRepository.save(new Reserva(estudianteCodigo, franja.getInstructorCodigo(),
            franja.getId(), franja.getFecha(), franja.getHoraInicio(), franja.getHoraFin(),
            propiedades.horasPorClase(), clock.instant()));

        if (franjaRepository.apartar(franjaCodigo, escuelaCodigo, reserva.getId(), hoy, horaActual).isEmpty()) {
            // * Nunca fue visible para nadie: es un dato derivado, se borra del todo en vez de dejarla cancelada
            reservaRepository.delete(reserva);
            throw franjaNoDisponible();
        }

        // * La saga es del orquestador: Pagos no responde sale como ServicioNoDisponibleException (ERR_ACA_17)
        ReservaCompletada completada = orquestador.completarReserva(reserva, franja, clientesDePagos.conBearer(bearer));
        if (completada.resultado() == ResultadoReserva.SIN_HORAS) {
            throw new ConflictException(AcademicoErrorCodes.HORAS_INSUFICIENTES, "No tienes horas suficientes");
        }
        return completada.reserva();
    }

    // ------------------------------------------------------------------ apoyo

    private void validarPeticion(List<UUID> franjaCodigos) {
        int maximo = propiedades.maximoPorConfirmacion();
        if (franjaCodigos == null || franjaCodigos.isEmpty() || franjaCodigos.size() > maximo
                || franjaCodigos.stream().anyMatch(c -> c == null)
                || franjaCodigos.stream().distinct().count() != franjaCodigos.size()) {
            throw new ValidationException(AcademicoErrorCodes.VALIDACION,
                "Elige entre 1 y " + maximo + " clases, sin repetir");
        }
    }

    /**
     * Los nombres de los instructores del resultado. La reserva ya está hecha: si Identidad no responde no se
     * deshace nada, el nombre se muestra como «Instructor».
     */
    private Map<UUID, String> nombresDe(ReservasResultado resultado, String bearer) {
        List<UUID> instructores = Stream.concat(
                resultado.reservadas().stream().map(Reserva::getInstructorCodigo),
                resultado.rechazadas().stream().map(FranjaRechazada::instructorCodigo))
            .filter(Objects::nonNull).distinct().toList();
        if (instructores.isEmpty()) {
            return Map.of();
        }
        try {
            return identidadClient.nombresDeInstructores(instructores, bearer);
        } catch (ServicioNoDisponibleException e) {
            return Map.of();
        }
    }

    private ReservaResponse aRespuesta(Reserva reserva, Map<UUID, String> nombres, LocalDateTime ahora) {
        return ReservaResponse.de(reserva, nombres.getOrDefault(reserva.getInstructorCodigo(), NOMBRE_SIN_DATOS),
            terminada(reserva, ahora));
    }

    /** La clase ya terminó: es de un día anterior a hoy, o de hoy con hora de fin que no es posterior a ahora. */
    static boolean terminada(Reserva reserva, LocalDateTime ahora) {
        LocalDate hoy = ahora.toLocalDate();
        return reserva.getFecha().isBefore(hoy) || (reserva.getFecha().equals(hoy)
            && reserva.getHoraFin().compareTo(ahora.toLocalTime().format(HORA)) <= 0);
    }

    private Integer saldoDe(String bearer) {
        try {
            return pagosClient.saldo(bearer);
        } catch (ServicioNoDisponibleException e) {
            return null;
        }
    }

    private static ConflictException franjaNoDisponible() {
        return new ConflictException(AcademicoErrorCodes.FRANJA_NO_DISPONIBLE, "La franja ya no está disponible");
    }

    private static FranjaRechazada rechazo(UUID franjaCodigo, Franja franja, BusinessException e) {
        return new FranjaRechazada(franjaCodigo, franja == null ? null : franja.getFecha(),
            franja == null ? null : franja.getHoraInicio(), franja == null ? null : franja.getHoraFin(),
            franja == null ? null : franja.getInstructorCodigo(), e.getErrorCode(), e.getMessage());
    }
}
