package com.drivehub.identidad.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.drivehub.identidad.domain.model.Perfil;
import com.drivehub.identidad.domain.model.SexoPersona;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.in.PersonaInstructor;

class PersonaInstructorMapeadorTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 23);
    private static final Clock RELOJ = Clock.fixed(HOY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private final PerfilArmador perfilArmador = mock(PerfilArmador.class);
    private final PersonaInstructorMapeador mapeador = new PersonaInstructorMapeador(RELOJ, perfilArmador);

    private static Usuario instructor(LocalDate nacimiento, Instant creadoEn) {
        return Usuario.builder()
            .codigo(UUID.randomUUID()).email("ivan@gmail.com").creadoEn(creadoEn)
            .perfil(Perfil.builder().nombres("Iván Luis").apellidos("Pérez").telefono("70000000")
                .fechaNacimiento(nacimiento).ci("AB12345").sexo(SexoPersona.MASCULINO)
                .direccion("Av. Arce 2345").latitud(new BigDecimal("-17.783300")).longitud(new BigDecimal("-63.182100"))
                .build())
            .build();
    }

    @Test
    void aPersona_calculaNombreCompletoEdadYFechaDeAlta() {
        PersonaInstructor persona = mapeador.aPersona(
            instructor(LocalDate.of(1990, 1, 1), Instant.parse("2026-05-04T15:30:00Z")));

        assertThat(persona.nombreCompleto()).isEqualTo("Iván Luis Pérez");
        assertThat(persona.edad()).isEqualTo(36);
        assertThat(persona.fechaAlta()).isEqualTo(LocalDate.of(2026, 5, 4));
        assertThat(persona.ci()).isEqualTo("AB12345");
        assertThat(persona.email()).isEqualTo("ivan@gmail.com");
        assertThat(persona.direccion()).isEqualTo("Av. Arce 2345");
    }

    @Test
    void edad_enElDiaExactoDelCumpleanosYElAnterior() {
        Instant alta = Instant.parse("2026-05-04T15:30:00Z");

        assertThat(mapeador.aPersona(instructor(LocalDate.of(1990, 10, 23), alta)).edad()).isEqualTo(36);
        assertThat(mapeador.aPersona(instructor(LocalDate.of(1990, 10, 24), alta)).edad()).isEqualTo(35);
    }

    @Test
    void fechaAlta_seCalculaEnLaZonaDelReloj() {
        Clock bolivia = Clock.fixed(HOY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneId.of("America/La_Paz"));
        PersonaInstructorMapeador conZona = new PersonaInstructorMapeador(bolivia, perfilArmador);

        PersonaInstructor persona = conZona.aPersona(
            instructor(LocalDate.of(1990, 1, 1), Instant.parse("2026-05-05T02:00:00Z")));

        assertThat(persona.fechaAlta()).isEqualTo(LocalDate.of(2026, 5, 4));
    }

    @Test
    void fotoUrl_esLaFirmadaOnullSiNoTieneFoto() {
        Usuario conFoto = instructor(LocalDate.of(1990, 1, 1), Instant.parse("2026-05-04T15:30:00Z"));
        when(perfilArmador.urlDeFoto(conFoto)).thenReturn("https://minio/foto-firmada");
        Usuario sinFoto = instructor(LocalDate.of(1990, 1, 1), Instant.parse("2026-05-04T15:30:00Z"));

        assertThat(mapeador.aPersona(conFoto).fotoUrl()).isEqualTo("https://minio/foto-firmada");
        assertThat(mapeador.aPersona(sinFoto).fotoUrl()).isNull();
    }
}
