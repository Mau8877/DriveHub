package com.drivehub.academico.features.reservas.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Clock;
import java.time.Duration;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.drivehub.academico.config.OrquestadorProperties;
import com.drivehub.academico.features.reservas.client.ClienteDePagos;
import com.drivehub.academico.features.reservas.enums.EstadoReserva;
import com.drivehub.academico.features.reservas.model.Reserva;
import com.drivehub.academico.features.reservas.repository.ReservaRepository;

/**
 * El orquestador de la saga de clases y horas (CU44): termina lo que quedó a medias entre Académico (franjas y
 * reservas) y Pagos (billetera de horas).
 *
 * * No tiene sesión (corre desde un proceso programado): por eso NO lleva `@PreAuthorize` y habla con Pagos con el
 * * cliente de servicio. En el flujo en vivo de un estudiante se le pasa el cliente con su Bearer.
 * * La lógica de cada operación vive en los {@link ManejadorDeOperacion}; este núcleo solo elige el manejador por el
 * * estado de la reserva, cuenta los intentos fallidos y marca para revisión.
 * * Nunca descuenta ni devuelve dos veces: toda llamada a Pagos lleva una clave derivada del código de la reserva.
 * ? Para CU15 (Leonel): la guía completa está en el README de las UT de CU44
 * ? (`docs/UnidadesTrabajo/CU44 - Orquestar clases y horas/UT-BACKEND/README.md`, «Contrato para CU15»).
 * * TODO: CU44 UT 17 y 18: agregar los manejadores de cancelación y reprogramación de CU15 cuando existan; no editar
 * * TODO: este núcleo, solo sumar un `@Component` que implemente {@link ManejadorDeOperacion}.
 */
@Service
public class OrquestadorClases {

    private static final Logger LOG = LoggerFactory.getLogger(OrquestadorClases.class);
    private static final String MODULO = "reservas";
    private static final Duration ALERTA_PAGOS_CAIDO = Duration.ofHours(24);

    private final Map<EstadoReserva, ManejadorDeOperacion> manejadorPorEstado = new EnumMap<>(EstadoReserva.class);
    private final Set<EstadoReserva> estadosTransitorios;
    private final CompensadorDeReserva compensador;
    private final ReservaRepository reservaRepository;
    private final ClienteDePagos clienteDeServicio;
    private final OrquestadorProperties propiedades;
    private final Clock clock;

    public OrquestadorClases(List<ManejadorDeOperacion> manejadores, CompensadorDeReserva compensador,
            ReservaRepository reservaRepository, @Qualifier("clienteDeServicio") ClienteDePagos clienteDeServicio,
            OrquestadorProperties propiedades, Clock clock) {
        for (ManejadorDeOperacion manejador : manejadores) {
            for (EstadoReserva estado : manejador.estados()) {
                ManejadorDeOperacion anterior = manejadorPorEstado.put(estado, manejador);
                if (anterior != null) {
                    // ! Dos manejadores sobre el mismo estado se pisarían en silencio: mejor no arrancar
                    throw new IllegalStateException("Dos manejadores declaran el estado " + estado + ": "
                        + anterior.getClass().getSimpleName() + " y " + manejador.getClass().getSimpleName());
                }
            }
        }
        this.estadosTransitorios = Set.copyOf(new HashSet<>(manejadorPorEstado.keySet()));
        this.compensador = compensador;
        this.reservaRepository = reservaRepository;
        this.clienteDeServicio = clienteDeServicio;
        this.propiedades = propiedades;
        this.clock = clock;
    }

    /** Los estados que algún manejador atiende: lo que el repositorio y el proceso programado tienen que buscar. */
    public Set<EstadoReserva> estadosTransitorios() {
        return estadosTransitorios;
    }

    /**
     * Resuelve UNA reserva con el manejador de su estado y el cliente de Pagos recibido (el del estudiante en el flujo
     * en vivo, el de servicio en el proceso programado). Una reserva en un estado que ningún manejador atiende no se
     * toca.
     *
     * * Vuelve a leer la reserva antes de actuar: si otro proceso (o el estudiante) ya la resolvió, devuelve cómo
     * * quedó y no descuenta ni devuelve nada.
     */
    public ResultadoOrquestacion resolver(Reserva reserva, ClienteDePagos cliente) {
        Optional<Reserva> actual = reservaRepository.findByCodigoReservaAndEstudianteCodigoAndDeletedAtIsNull(
            reserva.getCodigoReserva(), reserva.getEstudianteCodigo());
        if (actual.isEmpty()) {
            return ResultadoOrquestacion.PENDIENTE;
        }
        EstadoReserva estado = actual.get().getEstado();
        ManejadorDeOperacion manejador = manejadorPorEstado.get(estado);
        if (manejador == null) {
            return switch (estado) {
                case CANCELADA -> ResultadoOrquestacion.CANCELADA;
                case CONFIRMADA, COMPLETADA, NO_ASISTIO -> ResultadoOrquestacion.CONFIRMADA;
                default -> ResultadoOrquestacion.PENDIENTE;
            };
        }
        ResultadoOrquestacion resultado = manejador.resolver(actual.get(), cliente);
        LOG.info("Reserva a medias procesada", kv("module", MODULO),
            kv("reservaCodigo", reserva.getCodigoReserva()), kv("resultado", resultado));
        return resultado;
    }

    /**
     * Deshace una reserva a medias: devuelve la hora, libera la franja y la pasa de `PENDIENTE_PAGO` a `CANCELADA`
     * con el `motivo` en su historial. Repetirlo no devuelve dos veces. Pagos caído: no toca nada.
     *
     * * * Esta es la operación de cancelar: CU15 la reutiliza cuando una clase se cancela. No escribir otra copia
     * * * de devolver la hora.
     * * ! Pagos siempre devuelve lo que se le pide: las reglas de plazo de CU15 se deciden ANTES de llamar acá.
     */
    public ResultadoOrquestacion deshacerReserva(Reserva reserva, String motivo, ClienteDePagos cliente) {
        ResultadoOrquestacion resultado = compensador.deshacer(reserva, motivo, cliente);
        LOG.info("Reserva deshecha", kv("module", MODULO), kv("reservaCodigo", reserva.getCodigoReserva()),
            kv("resultado", resultado));
        return resultado;
    }

    /**
     * Resuelve una reserva con la credencial de servicio y cuenta los fallos por causas PROPIAS: al llegar a
     * `maximoIntentos` la marca para revisión manual y deja de retomarse sola. Nunca lanza una excepción.
     *
     * * Que Pagos no responda o rechace la credencial NO cuenta como intento: se reintenta en la próxima pasada,
     * * aunque la caída dure horas.
     */
    public ResultadoOrquestacion procesar(Reserva reserva) {
        ResultadoOrquestacion resultado;
        try {
            resultado = resolver(reserva, clienteDeServicio);
        } catch (RuntimeException e) {
            LOG.warn("Falló el orquestador con una reserva", kv("module", MODULO),
                kv("reservaCodigo", reserva.getCodigoReserva()), kv("tipoError", e.getClass().getSimpleName()));
            resultado = ResultadoOrquestacion.PENDIENTE;
        }
        return switch (resultado) {
            case CONFIRMADA, CANCELADA, EN_REVISION -> resultado;
            case PAGOS_NO_RESPONDE -> {
                avisarSiPagosLlevaMuchoCaido(reserva);
                yield resultado;
            }
            case PENDIENTE -> registrarFallo(reserva);
        };
    }

    private ResultadoOrquestacion registrarFallo(Reserva reserva) {
        try {
            Optional<Reserva> actualizada = reservaRepository.registrarIntentoFallido(reserva.getCodigoReserva(),
                estadosTransitorios);
            if (actualizada.isEmpty()) {
                return ResultadoOrquestacion.PENDIENTE;
            }
            int intentos = actualizada.get().getIntentosRetomar();
            if (intentos < propiedades.maximoIntentos()) {
                return ResultadoOrquestacion.PENDIENTE;
            }
            Optional<Reserva> marcada = reservaRepository.marcarParaRevision(reserva.getCodigoReserva(),
                "No se pudo resolver tras " + intentos + " intentos", clock.instant(), estadosTransitorios);
            if (marcada.isEmpty()) {
                return ResultadoOrquestacion.PENDIENTE;
            }
            LOG.warn("La reserva requiere revisión manual: el orquestador no pudo resolverla", kv("module", MODULO),
                kv("reservaCodigo", reserva.getCodigoReserva()), kv("intentos", intentos));
            return ResultadoOrquestacion.EN_REVISION;
        } catch (RuntimeException e) {
            LOG.warn("No se pudo registrar el intento fallido", kv("module", MODULO),
                kv("reservaCodigo", reserva.getCodigoReserva()), kv("tipoError", e.getClass().getSimpleName()));
            return ResultadoOrquestacion.PENDIENTE;
        }
    }

    private void avisarSiPagosLlevaMuchoCaido(Reserva reserva) {
        if (reserva.getCreatedAt() != null
                && Duration.between(reserva.getCreatedAt(), clock.instant()).compareTo(ALERTA_PAGOS_CAIDO) > 0) {
            LOG.error("Una reserva lleva más de 24 horas esperando a Pagos: revisar el servicio o la credencial",
                kv("module", MODULO), kv("reservaCodigo", reserva.getCodigoReserva()));
        }
    }
}
