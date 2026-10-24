package com.drivehub.academico.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.drivehub.academico.features.preguntas.enums.TipoPregunta;
import com.drivehub.academico.features.preguntas.model.Pregunta;
import com.drivehub.academico.features.preguntas.repository.PreguntaRepository;

@ExtendWith(MockitoExtension.class)
class PreguntaSeederTest {

    @Mock
    private PreguntaRepository preguntaRepository;

    @InjectMocks
    private PreguntaSeeder seeder;

    @Test
    void run_sinPreguntas_creaLasDeEjemploDeLaEscuelaExampleCumpliendoLasMismasReglasDelServicio() {
        when(preguntaRepository.existsByEscuelaCodigoAndEnunciado(any(), any())).thenReturn(false);

        seeder.run(null);

        ArgumentCaptor<Pregunta> creadas = ArgumentCaptor.forClass(Pregunta.class);
        verify(preguntaRepository, times(12)).save(creadas.capture());
        List<Pregunta> preguntas = creadas.getAllValues();
        assertThat(preguntas).allSatisfy(p -> {
            assertThat(p.getEscuelaCodigo()).isEqualTo(PreguntaSeeder.CODIGO_ESCUELA_EXAMPLE);
            assertThat(p.isActiva()).isTrue();
            assertThat(p.getDificultad()).isBetween(1, 3);
            long correctas = p.getOpciones().stream().filter(o -> o.correcta()).count();
            if (p.getTipo() == TipoPregunta.OPCION_MULTIPLE) {
                assertThat(correctas).isGreaterThanOrEqualTo(1);
            } else {
                assertThat(correctas).isEqualTo(1);
            }
            if (p.getTipo() == TipoPregunta.VERDADERO_FALSO) {
                assertThat(p.getOpciones()).hasSize(2);
            }
        });
        assertThat(preguntas.stream().map(Pregunta::getTipo).distinct()).containsExactlyInAnyOrder(TipoPregunta.values());
        assertThat(preguntas.stream().map(Pregunta::getDificultad).distinct()).containsExactlyInAnyOrder(1, 2, 3);
        assertThat(preguntas.stream().map(Pregunta::getEnunciado).distinct()).hasSize(12);
    }

    @Test
    void run_conLasPreguntasYaCreadas_noCreaNada() {
        when(preguntaRepository.existsByEscuelaCodigoAndEnunciado(any(), any())).thenReturn(true);

        seeder.run(null);

        verify(preguntaRepository, never()).save(any());
    }
}
