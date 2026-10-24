package com.drivehub.escuela.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.drivehub.escuela.common.exception.ValidationException;

class InstructorTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 23);

    private static Instructor nuevo(LocalDate vence, LocalDate contratacion) {
        return Instructor.nuevo(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "LIC-1", vence, contratacion,
            Set.of(CategoriaLicencia.builder().sigla("B").build()), HOY);
    }

    private static void assertErrSys01(Runnable accion) {
        assertThatThrownBy(accion::run).isInstanceOfSatisfying(ValidationException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo("ERR_SYS_01"));
    }

    @Test
    void nuevo_conDatosValidos_quedaActivoYConLaContratacionDeHoySiNoSeIndica() {
        Instructor instructor = nuevo(HOY.plusDays(1), null);

        assertThat(instructor.getEstado()).isEqualTo(EstadoInstructor.ACTIVO);
        assertThat(instructor.getFechaContratacion()).isEqualTo(HOY);
        assertThat(instructor.getNroLicencia()).isEqualTo("LIC-1");
    }

    @Test
    void nuevo_conContratacionPasadaOdeHoy_laConserva() {
        assertThat(nuevo(HOY.plusDays(1), HOY.minusYears(1)).getFechaContratacion()).isEqualTo(HOY.minusYears(1));
        assertThat(nuevo(HOY.plusDays(1), HOY).getFechaContratacion()).isEqualTo(HOY);
    }

    @Test
    void nuevo_conLicenciaQueVenceHoyOAntes_lanzaErrSys01() {
        assertErrSys01(() -> nuevo(HOY, null));
        assertErrSys01(() -> nuevo(HOY.minusDays(1), null));
        assertErrSys01(() -> nuevo(null, null));
    }

    @Test
    void nuevo_conContratacionFutura_lanzaErrSys01() {
        assertErrSys01(() -> nuevo(HOY.plusDays(1), HOY.plusDays(1)));
    }

    @Test
    void validarSiglas_normalizaYConservaElOrden() {
        assertThat(Instructor.validarSiglas(List.of(" b ", "a"))).containsExactly("B", "A");
    }

    @Test
    void validarSiglas_sinCategoriasConRepetidasOVacias_lanzaErrSys01() {
        assertErrSys01(() -> Instructor.validarSiglas(List.of()));
        assertErrSys01(() -> Instructor.validarSiglas(null));
        assertErrSys01(() -> Instructor.validarSiglas(List.of("A", "a")));
        assertErrSys01(() -> Instructor.validarSiglas(List.of("A", " ")));
    }

    @Test
    void nroLicencia_normalizaYValida() {
        assertThat(NroLicencia.normalizarYValidar("  ab 12-34 ")).isEqualTo("AB12-34");
        assertErrSys01(() -> NroLicencia.normalizarYValidar("AB"));
        assertErrSys01(() -> NroLicencia.normalizarYValidar("A".repeat(31)));
        assertErrSys01(() -> NroLicencia.normalizarYValidar("AB_123"));
        assertErrSys01(() -> NroLicencia.normalizarYValidar(null));
        assertThat(NroLicencia.normalizarYValidar("ABC")).isEqualTo("ABC");
        assertThat(NroLicencia.normalizarYValidar("A".repeat(30))).hasSize(30);
    }
}
