package com.drivehub.identidad.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class AccesoPorPlataformaTest {

    private Set<Rol> roles(String... nombres) {
        return Arrays.stream(nombres).map(n -> Rol.builder().nombre(n).build()).collect(Collectors.toSet());
    }

    @Test
    void web_permiteSuperAdminYAdminDeEscuela() {
        assertThat(AccesoPorPlataforma.permite(Dispositivo.WEB, roles(RolNombres.SUPER_ADMIN))).isTrue();
        assertThat(AccesoPorPlataforma.permite(Dispositivo.WEB, roles(RolNombres.ADMIN_ESCUELA))).isTrue();
    }

    @Test
    void web_rechazaEstudianteEInstructor() {
        assertThat(AccesoPorPlataforma.permite(Dispositivo.WEB, roles(RolNombres.ESTUDIANTE))).isFalse();
        assertThat(AccesoPorPlataforma.permite(Dispositivo.WEB, roles(RolNombres.INSTRUCTOR))).isFalse();
    }

    @Test
    void movil_permiteEstudianteEInstructorYRechazaAdmins() {
        assertThat(AccesoPorPlataforma.permite(Dispositivo.MOVIL, roles(RolNombres.ESTUDIANTE))).isTrue();
        assertThat(AccesoPorPlataforma.permite(Dispositivo.MOVIL, roles(RolNombres.INSTRUCTOR))).isTrue();
        assertThat(AccesoPorPlataforma.permite(Dispositivo.MOVIL, roles(RolNombres.ADMIN_ESCUELA))).isFalse();
        assertThat(AccesoPorPlataforma.permite(Dispositivo.MOVIL, roles(RolNombres.SUPER_ADMIN))).isFalse();
    }

    @Test
    void conVariosRoles_bastaUnoPermitido() {
        assertThat(AccesoPorPlataforma.permite(Dispositivo.WEB, roles(RolNombres.INSTRUCTOR, RolNombres.ADMIN_ESCUELA)))
            .isTrue();
    }

    @Test
    void plataformaCorrecta_indicaLaOpuesta() {
        assertThat(AccesoPorPlataforma.plataformaCorrecta(Dispositivo.WEB)).isEqualTo("la aplicación móvil");
        assertThat(AccesoPorPlataforma.plataformaCorrecta(Dispositivo.MOVIL)).isEqualTo("la web");
    }
}
