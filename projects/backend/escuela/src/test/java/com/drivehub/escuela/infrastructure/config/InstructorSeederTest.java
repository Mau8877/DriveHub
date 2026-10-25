package com.drivehub.escuela.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.drivehub.escuela.domain.entity.CategoriaLicencia;
import com.drivehub.escuela.domain.entity.EstadoInstructor;
import com.drivehub.escuela.domain.entity.Instructor;
import com.drivehub.escuela.domain.repository.CategoriaLicenciaRepository;
import com.drivehub.escuela.domain.repository.InstructorRepository;

@ExtendWith(MockitoExtension.class)
class InstructorSeederTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-24T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private InstructorRepository instructorRepository;

    @Mock
    private CategoriaLicenciaRepository categoriaLicenciaRepository;

    private InstructorSeeder seeder;

    @BeforeEach
    void preparar() {
        seeder = new InstructorSeeder(instructorRepository, categoriaLicenciaRepository, RELOJ);
    }

    @Test
    void run_sinFicha_creaLaDeLaCuentaDeEjemploEnLaEscuelaDeEjemploConVigenciaFutura() {
        CategoriaLicencia particular = CategoriaLicencia.builder().sigla("P").build();
        CategoriaLicencia profesional = CategoriaLicencia.builder().sigla("B").build();
        when(instructorRepository.existePorNroLicencia(InstructorSeeder.NRO_LICENCIA_EXAMPLE)).thenReturn(false);
        when(categoriaLicenciaRepository.buscarPorSiglas(InstructorSeeder.CATEGORIAS_EXAMPLE))
            .thenReturn(List.of(particular, profesional));

        seeder.run(null);

        ArgumentCaptor<Instructor> creado = ArgumentCaptor.forClass(Instructor.class);
        verify(instructorRepository).guardar(creado.capture());
        Instructor ficha = creado.getValue();
        assertThat(ficha.getCodigo()).isNotNull();
        assertThat(ficha.getUsuarioCodigo()).isEqualTo(InstructorSeeder.CODIGO_USUARIO_INSTRUCTOR_EXAMPLE);
        assertThat(ficha.getEscuelaCodigo()).isEqualTo(EscuelaSeeder.CODIGO_ESCUELA_EXAMPLE);
        assertThat(ficha.getNroLicencia()).isEqualTo("SEED-LIC-001");
        assertThat(ficha.getLicenciaVence()).isEqualTo(LocalDate.of(2029, 10, 24));
        assertThat(ficha.getFechaContratacion()).isEqualTo(LocalDate.of(2026, 10, 24));
        assertThat(ficha.getEstado()).isEqualTo(EstadoInstructor.ACTIVO);
        assertThat(ficha.getCategorias()).containsExactlyInAnyOrder(particular, profesional);
    }

    @Test
    void run_conLaFichaYaCreada_noCreaNada() {
        when(instructorRepository.existePorNroLicencia(InstructorSeeder.NRO_LICENCIA_EXAMPLE)).thenReturn(true);

        seeder.run(null);

        verify(instructorRepository, never()).guardar(any());
    }

    @Test
    void run_sinCategoriasEnElCatalogo_noCreaNada() {
        when(instructorRepository.existePorNroLicencia(InstructorSeeder.NRO_LICENCIA_EXAMPLE)).thenReturn(false);
        when(categoriaLicenciaRepository.buscarPorSiglas(InstructorSeeder.CATEGORIAS_EXAMPLE)).thenReturn(List.of());

        seeder.run(null);

        verify(instructorRepository, never()).guardar(any());
    }
}
