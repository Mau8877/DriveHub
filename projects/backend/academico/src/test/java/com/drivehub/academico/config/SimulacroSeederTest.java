package com.drivehub.academico.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.drivehub.academico.features.simulacros.model.ReglaSimulacro;
import com.drivehub.academico.features.simulacros.model.SimulacroConfig;
import com.drivehub.academico.features.simulacros.repository.SimulacroConfigRepository;

@ExtendWith(MockitoExtension.class)
class SimulacroSeederTest {

    @Mock
    private SimulacroConfigRepository repository;

    @InjectMocks
    private SimulacroSeeder seeder;

    @Test
    void run_sinSimulacros_creaLosDeEjemploConReglasQueSumanElNumeroDePreguntas() {
        when(repository.existsByEscuelaCodigoAndNombre(any(), any())).thenReturn(false);

        seeder.run(null);

        ArgumentCaptor<SimulacroConfig> creados = ArgumentCaptor.forClass(SimulacroConfig.class);
        verify(repository, times(2)).save(creados.capture());
        assertThat(creados.getAllValues()).allSatisfy(s -> {
            assertThat(s.getEscuelaCodigo()).isEqualTo(PreguntaSeeder.CODIGO_ESCUELA_EXAMPLE);
            assertThat(s.isActivo()).isTrue();
            assertThat(s.getReglas().stream().mapToInt(ReglaSimulacro::cantidad).sum()).isEqualTo(s.getNroPreguntas());
        });
    }

    @Test
    void run_conLosSimulacrosYaCreados_noCreaNada() {
        when(repository.existsByEscuelaCodigoAndNombre(any(), any())).thenReturn(true);

        seeder.run(null);

        verify(repository, never()).save(any());
    }
}
