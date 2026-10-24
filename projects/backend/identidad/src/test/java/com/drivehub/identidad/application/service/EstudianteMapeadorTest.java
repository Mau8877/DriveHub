package com.drivehub.identidad.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.drivehub.identidad.domain.model.EstadoEstudiante;
import com.drivehub.identidad.domain.model.EstadoUsuario;
import com.drivehub.identidad.domain.model.Perfil;
import com.drivehub.identidad.domain.model.SexoPersona;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.in.EstudianteDetalle;
import com.drivehub.identidad.domain.port.in.EstudianteResumen;

class EstudianteMapeadorTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 23);
    private static final Clock RELOJ = Clock.fixed(HOY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private final EstudianteMapeador mapeador = new EstudianteMapeador(RELOJ);

    private static Usuario estudiante(LocalDate nacimiento, EstadoUsuario estado) {
        return Usuario.builder()
            .codigo(UUID.randomUUID()).email("ana@gmail.com").estado(estado)
            .creadoEn(Instant.parse("2026-05-04T15:30:00Z"))
            .perfil(Perfil.builder().nombres("Ana María").apellidos("Pérez").telefono("70000000")
                .fechaNacimiento(nacimiento).ci("AB12345").sexo(SexoPersona.FEMENINO)
                .direccion("Av. Arce 2345").latitud(new BigDecimal("-17.783300")).longitud(new BigDecimal("-63.182100"))
                .categoriaSigla("SIN").build())
            .build();
    }

    @Test
    void aResumen_calculaNombreCompletoEdadYFechaDeIncorporacion() {
        EstudianteResumen resumen = mapeador.aResumen(estudiante(LocalDate.of(2000, 1, 1), EstadoUsuario.ACTIVO));

        assertThat(resumen.nombreCompleto()).isEqualTo("Ana María Pérez");
        assertThat(resumen.edad()).isEqualTo(26);
        assertThat(resumen.categoriaSigla()).isEqualTo("SIN");
        assertThat(resumen.estado()).isEqualTo(EstadoEstudiante.ACTIVO);
        assertThat(resumen.fechaIncorporacion()).isEqualTo(LocalDate.of(2026, 5, 4));
    }

    @Test
    void edad_enElDiaExactoDelCumpleanos_yaCumplio() {
        assertThat(mapeador.aResumen(estudiante(LocalDate.of(2000, 10, 23), EstadoUsuario.ACTIVO)).edad())
            .isEqualTo(26);
    }

    @Test
    void edad_elDiaAnteriorAlCumpleanos_todaviaNo() {
        assertThat(mapeador.aResumen(estudiante(LocalDate.of(2000, 10, 24), EstadoUsuario.ACTIVO)).edad())
            .isEqualTo(25);
    }

    @Test
    void edad_deUnNacidoUn29DeFebrero_seCuentaConLaRegla() {
        // * 2004 fue bisiesto; el 23 de octubre de 2026 ya pasó el cumpleaños de ese año
        assertThat(mapeador.aResumen(estudiante(LocalDate.of(2004, 2, 29), EstadoUsuario.ACTIVO)).edad())
            .isEqualTo(22);
    }

    @Test
    void estado_deUnaCuentaBloqueada_seMuestraActivo() {
        assertThat(mapeador.aResumen(estudiante(LocalDate.of(2000, 1, 1), EstadoUsuario.BLOQUEADO)).estado())
            .isEqualTo(EstadoEstudiante.ACTIVO);
    }

    @Test
    void estado_deUnaCuentaInactiva_seMuestraInactivo() {
        assertThat(mapeador.aResumen(estudiante(LocalDate.of(2000, 1, 1), EstadoUsuario.INACTIVO)).estado())
            .isEqualTo(EstadoEstudiante.INACTIVO);
    }

    @Test
    void fechaDeIncorporacion_seCalculaEnLaZonaDelReloj() {
        // * 02:00 UTC del 5 de mayo es todavía el 4 de mayo en Bolivia (UTC-4)
        Clock bolivia = Clock.fixed(RELOJ.instant(), ZoneId.of("America/La_Paz"));
        Usuario tarde = estudiante(LocalDate.of(2000, 1, 1), EstadoUsuario.ACTIVO).toBuilder()
            .creadoEn(Instant.parse("2026-05-05T02:00:00Z")).build();

        assertThat(new EstudianteMapeador(bolivia).aResumen(tarde).fechaIncorporacion())
            .isEqualTo(LocalDate.of(2026, 5, 4));
    }

    @Test
    void aDetalle_traeLosDatosDeLaPersonaYNadaDeAcceso() {
        EstudianteDetalle detalle = mapeador.aDetalle(estudiante(LocalDate.of(2000, 1, 1), EstadoUsuario.ACTIVO));

        assertThat(detalle.nombres()).isEqualTo("Ana María");
        assertThat(detalle.apellidos()).isEqualTo("Pérez");
        assertThat(detalle.nombreCompleto()).isEqualTo("Ana María Pérez");
        assertThat(detalle.fechaNacimiento()).isEqualTo(LocalDate.of(2000, 1, 1));
        assertThat(detalle.sexo()).isEqualTo(SexoPersona.FEMENINO);
        assertThat(detalle.ci()).isEqualTo("AB12345");
        assertThat(detalle.email()).isEqualTo("ana@gmail.com");
        assertThat(detalle.telefono()).isEqualTo("70000000");
        assertThat(detalle.direccion()).isEqualTo("Av. Arce 2345");
        assertThat(detalle.latitud()).isEqualByComparingTo("-17.7833");
        assertThat(detalle.longitud()).isEqualByComparingTo("-63.1821");
        assertThat(detalle.categoriaSigla()).isEqualTo("SIN");
        assertThat(detalle.fechaIncorporacion()).isEqualTo(LocalDate.of(2026, 5, 4));
    }
}
