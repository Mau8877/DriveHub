package com.drivehub.academico.features.reservas.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.drivehub.academico.features.reservas.model.Reserva;
import com.drivehub.academico.features.reservas.repository.ReservaRepository;

/**
 * Recuerda las clases que están por empezar (CU36): cada hora avisa al estudiante y al instructor de las clases
 * confirmadas que empiezan dentro de la última hora de la ventana `app.recordatorios.horas-antes` (24 por defecto),
 * es decir, entre 23 y 24 horas desde ahora.
 *
 * * Corre sin sesión, igual que el orquestador. El aviso es de mejor esfuerzo: un fallo del servicio de
 * * notificaciones se registra y la pasada sigue.
 * * La ventana mide una hora y el proceso corre cada hora, así cada clase cae en una sola pasada. Si el servicio
 * * estuvo apagado esa hora, el recordatorio de esa clase se pierde (no se acumula). El `eventoCodigo` es
 * * determinista: aunque una clase cayera en dos pasadas, Notificaciones no la avisa dos veces.
 * * Una clase reservada con menos de ese tiempo de anticipación no recibe recordatorio.
 */
@Component
@ConditionalOnProperty(prefix = "app.recordatorios", name = "habilitado", matchIfMissing = true)
public class RecordatoriosClaseJob {

    static final int LIMITE = 500;

    private static final Logger LOG = LoggerFactory.getLogger(RecordatoriosClaseJob.class);
    private static final String MODULO = "reservas";

    private final ReservaRepository reservaRepository;
    private final AvisosReserva avisos;
    private final Clock clock;
    private final long horasAntes;
    private final AtomicBoolean enCurso = new AtomicBoolean(false);

    public RecordatoriosClaseJob(ReservaRepository reservaRepository, AvisosReserva avisos, Clock clock,
            @Value("${app.recordatorios.horas-antes:24}") long horasAntes) {
        this.reservaRepository = reservaRepository;
        this.avisos = avisos;
        this.clock = clock;
        this.horasAntes = horasAntes;
    }

    @Scheduled(cron = "${app.recordatorios.cron:0 0 * * * *}", zone = "${app.horarios.zona-horaria:America/La_Paz}")
    public void ejecutar() {
        if (!enCurso.compareAndSet(false, true)) {
            return;
        }
        try {
            recordar();
        } catch (RuntimeException e) {
            LOG.error("Falló la pasada de recordatorios de clase", kv("module", MODULO),
                kv("tipoError", e.getClass().getSimpleName()));
        } finally {
            enCurso.set(false);
        }
    }

    /** Hace una pasada y devuelve cuántas clases recordó. */
    int recordar() {
        LocalDateTime ahora = LocalDateTime.now(clock);
        LocalDateTime hasta = ahora.plusHours(horasAntes);
        LocalDateTime desde = hasta.minusHours(1);
        List<Reserva> candidatas = reservaRepository.confirmadasEnFechas(desde.toLocalDate(), hasta.toLocalDate(),
            LIMITE);
        int recordadas = 0;
        for (Reserva reserva : candidatas) {
            LocalDateTime empieza = LocalDateTime.of(reserva.getFecha(), hora(reserva));
            if (empieza.isAfter(desde) && !empieza.isAfter(hasta)) {
                avisos.recordatorio(reserva);
                recordadas++;
            }
        }
        if (recordadas > 0) {
            LOG.info("Recordatorios de clase enviados", kv("module", MODULO), kv("clases", recordadas));
        }
        return recordadas;
    }

    private static LocalTime hora(Reserva reserva) {
        return LocalTime.parse(reserva.getHoraInicio());
    }
}
