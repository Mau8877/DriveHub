package com.drivehub.academico.features.reservas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;

import com.drivehub.academico.config.OrquestadorProperties;
import com.drivehub.academico.config.ReservasProperties;
import com.drivehub.academico.features.reservas.enums.EstadoReserva;
import com.drivehub.academico.features.reservas.model.Reserva;
import com.drivehub.academico.features.reservas.repository.ReservaRepository;

@ExtendWith(MockitoExtension.class)
class OrquestadorClasesJobTest {

    private static final Instant AHORA = Instant.parse("2026-10-26T13:00:00Z");
    private static final Set<EstadoReserva> ESTADOS = Set.of(EstadoReserva.PENDIENTE_PAGO);

    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private OrquestadorClases orquestador;

    private final Clock reloj = Clock.fixed(AHORA, ZoneId.of("America/La_Paz"));

    private OrquestadorClasesJob job(int lote, int minutosParaRetomar) {
        return new OrquestadorClasesJob(reservaRepository, orquestador,
            new OrquestadorProperties(60, 5, true, "0 * * * * *", lote),
            new ReservasProperties(1, 10, minutosParaRetomar), reloj);
    }

    private static Reserva reserva() {
        Reserva reserva = new Reserva(UUID.randomUUID(), UUID.randomUUID(), 1L, LocalDate.of(2026, 10, 27), "08:00",
            "09:00", 1, AHORA);
        reserva.inicializarCodigo();
        return reserva;
    }

    @Test
    void procesaTodasLasPendientes_conElUmbralYElLoteConfigurados() {
        Reserva a = reserva();
        Reserva b = reserva();
        when(orquestador.estadosTransitorios()).thenReturn(ESTADOS);
        when(reservaRepository.pendientesAntiguas(ESTADOS, AHORA.minusSeconds(3 * 60), 25))
            .thenReturn(List.of(a, b));
        when(orquestador.procesar(any())).thenReturn(ResultadoOrquestacion.CONFIRMADA);

        job(25, 3).ejecutar();

        verify(orquestador).procesar(a);
        verify(orquestador).procesar(b);
    }

    @Test
    void unaExcepcionEnUnaReserva_noImpideProcesarLaSiguiente() {
        Reserva a = reserva();
        Reserva b = reserva();
        when(orquestador.estadosTransitorios()).thenReturn(ESTADOS);
        when(reservaRepository.pendientesAntiguas(eq(ESTADOS), any(), anyInt())).thenReturn(List.of(a, b));
        when(orquestador.procesar(a)).thenThrow(new IllegalStateException("boom"));
        when(orquestador.procesar(b)).thenReturn(ResultadoOrquestacion.CANCELADA);

        job(50, 2).ejecutar();

        verify(orquestador).procesar(b);
    }

    @Test
    void sinPendientes_noProcesaNada() {
        when(orquestador.estadosTransitorios()).thenReturn(ESTADOS);
        when(reservaRepository.pendientesAntiguas(eq(ESTADOS), any(), anyInt())).thenReturn(List.of());

        job(50, 2).ejecutar();

        verify(orquestador, never()).procesar(any());
    }

    @Test
    void unaFallaAlBuscar_noSePropaga() {
        when(orquestador.estadosTransitorios()).thenReturn(ESTADOS);
        when(reservaRepository.pendientesAntiguas(anySet(), any(), anyInt()))
            .thenThrow(new IllegalStateException("mongo caído"));

        job(50, 2).ejecutar();

        verify(orquestador, never()).procesar(any());
    }

    @Test
    void correSinSesion_conElSecurityContextVacio() {
        SecurityContextHolder.clearContext();
        when(orquestador.estadosTransitorios()).thenReturn(ESTADOS);
        when(reservaRepository.pendientesAntiguas(eq(ESTADOS), any(), anyInt())).thenReturn(List.of(reserva()));
        when(orquestador.procesar(any())).thenReturn(ResultadoOrquestacion.PAGOS_NO_RESPONDE);

        job(50, 2).ejecutar();

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(orquestador, times(1)).procesar(any());
    }

    @Test
    void unaPasadaEnCurso_haceQueLaSiguienteSeOmita() throws Exception {
        CountDownLatch dentro = new CountDownLatch(1);
        CountDownLatch soltar = new CountDownLatch(1);
        when(orquestador.estadosTransitorios()).thenReturn(ESTADOS);
        when(reservaRepository.pendientesAntiguas(eq(ESTADOS), any(), anyInt())).thenAnswer(i -> {
            dentro.countDown();
            soltar.await(5, TimeUnit.SECONDS);
            return List.of();
        });
        OrquestadorClasesJob trabajo = job(50, 2);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            pool.submit(trabajo::ejecutar);
            dentro.await(5, TimeUnit.SECONDS);

            trabajo.ejecutar();

            verify(reservaRepository, times(1)).pendientesAntiguas(anySet(), any(), anyInt());
        } finally {
            soltar.countDown();
            pool.shutdown();
            pool.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    // ---------------------------------------------------------------- configuración

    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of())
        .withBean(ReservaRepository.class, () -> org.mockito.Mockito.mock(ReservaRepository.class))
        .withBean(OrquestadorClases.class, () -> org.mockito.Mockito.mock(OrquestadorClases.class))
        .withBean(OrquestadorProperties.class, () -> new OrquestadorProperties(60, 5, true, "0 * * * * *", 50))
        .withBean(ReservasProperties.class, () -> new ReservasProperties(1, 10, 2))
        .withBean(Clock.class, Clock::systemUTC)
        .withUserConfiguration(OrquestadorClasesJob.class);

    @Test
    void conHabilitadoFalse_elJobNoExiste() {
        contexto.withPropertyValues("app.orquestador.habilitado=false")
            .run(c -> assertThat(c).doesNotHaveBean(OrquestadorClasesJob.class));
    }

    @Test
    void porDefectoYConHabilitadoTrue_elJobExiste() {
        contexto.run(c -> assertThat(c).hasSingleBean(OrquestadorClasesJob.class));
        contexto.withPropertyValues("app.orquestador.habilitado=true")
            .run(c -> assertThat(c).hasSingleBean(OrquestadorClasesJob.class));
    }

    @Test
    void ejecutar_estaProgramadoConElCronYLaZonaDeLaConfiguracion() throws NoSuchMethodException {
        Scheduled programado = OrquestadorClasesJob.class.getMethod("ejecutar").getAnnotation(Scheduled.class);

        assertThat(programado).isNotNull();
        assertThat(programado.cron()).startsWith("${app.orquestador.cron");
        assertThat(programado.zone()).startsWith("${app.horarios.zona-horaria");
    }
}
