package com.drivehub.academico.features.horarios.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.annotation.Scheduled;

class ExtensionFranjasJobTest {

    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
        .withUserConfiguration(ExtensionFranjasJob.class)
        .withBean(FranjaGeneracionService.class, () -> mock(FranjaGeneracionService.class));

    @Test
    void extender_llamaALaGeneracionDiaria() {
        FranjaGeneracionService generacion = mock(FranjaGeneracionService.class);

        new ExtensionFranjasJob(generacion).extender();

        verify(generacion).extenderHorariosActivos();
    }

    @Test
    void job_porDefectoEstaHabilitado() {
        contexto.run(c -> assertThat(c).hasSingleBean(ExtensionFranjasJob.class));
    }

    @Test
    void job_conJobHabilitadoTrue_estaHabilitado() {
        contexto.withPropertyValues("app.horarios.job-habilitado=true")
            .run(c -> assertThat(c).hasSingleBean(ExtensionFranjasJob.class));
    }

    @Test
    void job_conJobHabilitadoFalse_noSeCrea() {
        contexto.withPropertyValues("app.horarios.job-habilitado=false")
            .run(c -> assertThat(c).doesNotHaveBean(ExtensionFranjasJob.class));
    }

    @Test
    void extender_estaProgramadoConElCronYLaZonaDeLaConfiguracion() throws NoSuchMethodException {
        Scheduled programado = ExtensionFranjasJob.class.getMethod("extender").getAnnotation(Scheduled.class);

        assertThat(programado).isNotNull();
        assertThat(programado.cron()).isEqualTo("${app.horarios.extension-cron}");
        assertThat(programado.zone()).isEqualTo("${app.horarios.zona-horaria}");
    }
}
