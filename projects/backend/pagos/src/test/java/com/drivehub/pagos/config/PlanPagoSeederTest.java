package com.drivehub.pagos.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.drivehub.pagos.features.planes.model.PlanPago;
import com.drivehub.pagos.features.planes.repository.PlanPagoRepository;

@ExtendWith(MockitoExtension.class)
class PlanPagoSeederTest {

    @Mock
    private PlanPagoRepository planRepository;

    @InjectMocks
    private PlanPagoSeeder seeder;

    @Test
    void run_sinPlanes_creaLosCincoDeLaEscuelaExample_deMasAMenosHoras() {
        when(planRepository.existsByEscuelaCodigoAndNombreIgnoreCase(any(), any())).thenReturn(false);

        seeder.run(null);

        ArgumentCaptor<PlanPago> creados = ArgumentCaptor.forClass(PlanPago.class);
        verify(planRepository, times(5)).saveAndFlush(creados.capture());
        List<PlanPago> planes = creados.getAllValues();
        assertThat(planes).allSatisfy(p -> {
            assertThat(p.getEscuelaCodigo()).isEqualTo(PlanPagoSeeder.CODIGO_ESCUELA_EXAMPLE);
            assertThat(p.getHoras()).isBetween((short) 1, (short) 500);
            assertThat(p.getCostoBs()).isGreaterThan(BigDecimal.ZERO);
        });
        assertThat(planes.stream().map(PlanPago::getNombre).distinct()).hasSize(5);
        assertThat(planes).allMatch(PlanPago::isActivo);
        assertThat(planes.stream().map(p -> (int) p.getHoras())).containsExactly(15, 10, 7, 5, 1);
        // * Se crean de más a menos horas: el listado va del más reciente al más antiguo y los muestra de menos a más
        assertThat(planes.stream().map(PlanPago::getCostoBs))
            .containsExactly(new BigDecimal("700.00"), new BigDecimal("500.00"), new BigDecimal("370.00"),
                new BigDecimal("275.00"), new BigDecimal("60.00"));
        // * Cuantas más horas, menos cuesta cada una
        for (int i = 0; i < planes.size() - 1; i++) {
            BigDecimal masHoras = planes.get(i).getCostoBs().divide(BigDecimal.valueOf(planes.get(i).getHoras()), 4,
                java.math.RoundingMode.HALF_UP);
            BigDecimal menosHoras = planes.get(i + 1).getCostoBs()
                .divide(BigDecimal.valueOf(planes.get(i + 1).getHoras()), 4, java.math.RoundingMode.HALF_UP);
            assertThat(masHoras).isLessThan(menosHoras);
        }
    }

    @Test
    void run_conLosPlanesYaCreados_noCreaNada() {
        when(planRepository.existsByEscuelaCodigoAndNombreIgnoreCase(any(), any())).thenReturn(true);

        seeder.run(null);

        verify(planRepository, never()).saveAndFlush(any());
    }
}
