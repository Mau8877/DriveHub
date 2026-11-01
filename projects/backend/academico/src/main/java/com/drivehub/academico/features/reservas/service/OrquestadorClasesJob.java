package com.drivehub.academico.features.reservas.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.drivehub.academico.config.OrquestadorProperties;
import com.drivehub.academico.config.ReservasProperties;
import com.drivehub.academico.features.reservas.model.Reserva;
import com.drivehub.academico.features.reservas.repository.ReservaRepository;

import lombok.RequiredArgsConstructor;

/**
 * El proceso programado del orquestador (CU44): cada tanto toma las reservas a medias de TODOS los estudiantes y las
 * resuelve, sin esperar a que el estudiante consulte sus clases.
 *
 * * Corre sin sesión (no usa el `SecurityContext`): el orquestador habla con Pagos con la credencial de servicio.
 * * No conoce reservas ni cancelaciones: pide los estados al orquestador, así que un manejador nuevo (CU15) queda
 * * cubierto sin editar este job.
 * * Un fallo con una reserva no frena a las demás ni a la próxima pasada. Una pasada que sigue en curso cuando llega
 * * la siguiente hace que la nueva se omita (un solo proceso).
 * * Solo loguea cuando había algo que revisar: no llena el log cada minuto.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.orquestador", name = "habilitado", matchIfMissing = true)
public class OrquestadorClasesJob {

    private static final Logger LOG = LoggerFactory.getLogger(OrquestadorClasesJob.class);
    private static final String MODULO = "reservas";

    private final ReservaRepository reservaRepository;
    private final OrquestadorClases orquestador;
    private final OrquestadorProperties propiedades;
    private final ReservasProperties reservas;
    private final Clock clock;
    private final AtomicBoolean enCurso = new AtomicBoolean(false);

    @Scheduled(cron = "${app.orquestador.cron:0 * * * * *}", zone = "${app.horarios.zona-horaria:America/La_Paz}")
    public void ejecutar() {
        if (!enCurso.compareAndSet(false, true)) {
            return;
        }
        try {
            procesarPendientes();
        } catch (RuntimeException e) {
            LOG.error("Falló la pasada del orquestador de clases", kv("module", MODULO),
                kv("tipoError", e.getClass().getSimpleName()));
        } finally {
            enCurso.set(false);
        }
    }

    private void procesarPendientes() {
        List<Reserva> pendientes = reservaRepository.pendientesAntiguas(orquestador.estadosTransitorios(),
            clock.instant().minus(Duration.ofMinutes(reservas.minutosParaRetomar())), propiedades.lote());
        if (pendientes.isEmpty()) {
            return;
        }
        int confirmadas = 0;
        int canceladas = 0;
        int pendientesSinResolver = 0;
        int sinRespuestaDePagos = 0;
        int enRevision = 0;
        for (Reserva reserva : pendientes) {
            ResultadoOrquestacion resultado;
            try {
                resultado = orquestador.procesar(reserva);
            } catch (RuntimeException e) {
                LOG.warn("Falló el orquestador con una reserva", kv("module", MODULO),
                    kv("reservaCodigo", reserva.getCodigoReserva()), kv("tipoError", e.getClass().getSimpleName()));
                resultado = ResultadoOrquestacion.PENDIENTE;
            }
            switch (resultado) {
                case CONFIRMADA -> confirmadas++;
                case CANCELADA -> canceladas++;
                case PENDIENTE -> pendientesSinResolver++;
                case PAGOS_NO_RESPONDE -> sinRespuestaDePagos++;
                case EN_REVISION -> enRevision++;
            }
        }
        LOG.info("Pasada del orquestador de clases", kv("module", MODULO), kv("revisadas", pendientes.size()),
            kv("confirmadas", confirmadas), kv("canceladas", canceladas), kv("pendientes", pendientesSinResolver),
            kv("sinRespuestaDePagos", sinRespuestaDePagos), kv("enRevision", enRevision));
    }
}
