package com.drivehub.identidad.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.drivehub.identidad.common.exception.ValidationException;
import com.drivehub.identidad.domain.model.Perfil;
import com.drivehub.identidad.domain.model.SexoPersona;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.in.BuscarInstructoresComando;
import com.drivehub.identidad.domain.port.in.PaginaPersonasInstructor;
import com.drivehub.identidad.domain.port.in.PersonaInstructor;
import com.drivehub.identidad.domain.port.out.PaginaUsuarios;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

/**
 * La autorización por rol NO se prueba acá (un servicio sin Spring no ejecuta @PreAuthorize): se
 * ejerce en la integración de los endpoints.
 */
@ExtendWith(MockitoExtension.class)
class ConsultaDeInstructoresServicesTest {

    private static final UUID ADMIN = UUID.randomUUID();
    private static final UUID ESCUELA = UUID.randomUUID();
    private static final LocalDate HOY = LocalDate.of(2026, 10, 23);

    @Mock
    private UsuarioRepositoryPort usuarioRepository;

    private ConsultarInstructoresService consultar;
    private BuscarInstructoresService buscar;

    @BeforeEach
    void crearServicios() {
        Clock reloj = Clock.fixed(HOY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        PersonaInstructorMapeador mapeador = new PersonaInstructorMapeador(reloj, mock(PerfilArmador.class));
        EscuelaDelAdministrador escuelaDelAdministrador = new EscuelaDelAdministrador(usuarioRepository);
        consultar = new ConsultarInstructoresService(usuarioRepository, escuelaDelAdministrador, mapeador);
        buscar = new BuscarInstructoresService(usuarioRepository, escuelaDelAdministrador, mapeador);
        when(usuarioRepository.buscarPorCodigo(ADMIN)).thenReturn(Optional.of(
            Usuario.builder().codigo(ADMIN).escuelaCodigo(ESCUELA).build()));
    }

    private static Usuario instructor(UUID codigo) {
        return Usuario.builder().codigo(codigo).email("ivan@gmail.com").creadoEn(Instant.parse("2026-05-04T15:30:00Z"))
            .perfil(Perfil.builder().nombres("Ivan").apellidos("Pérez").fechaNacimiento(LocalDate.of(1990, 1, 1))
                .ci("AB12345").sexo(SexoPersona.MASCULINO).build())
            .build();
    }

    @Test
    void consultar_pasaLaEscuelaDelAdministradorYQuitaRepetidos() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        when(usuarioRepository.buscarInstructoresPorCodigos(eq(ESCUELA), any())).thenReturn(List.of(instructor(a)));

        List<PersonaInstructor> personas = consultar.consultar(ADMIN, List.of(a, b, a));

        verify(usuarioRepository).buscarInstructoresPorCodigos(ESCUELA, List.of(a, b));
        assertThat(personas).extracting(PersonaInstructor::codigo).containsExactly(a);
    }

    @Test
    void consultar_sinCodigosOConMasDeCien_lanzaErrSys01() {
        List<UUID> ciento1 = IntStream.range(0, 101).mapToObj(i -> UUID.randomUUID()).toList();

        assertThatThrownBy(() -> consultar.consultar(ADMIN, List.of()))
            .isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.getErrorCode()).isEqualTo("ERR_SYS_01"));
        assertThatThrownBy(() -> consultar.consultar(ADMIN, ciento1))
            .isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.getErrorCode()).isEqualTo("ERR_SYS_01"));
        verify(usuarioRepository, never()).buscarInstructoresPorCodigos(any(), any());
    }

    @Test
    void consultar_conExactamenteCienCodigos_loAcepta() {
        List<UUID> cien = IntStream.range(0, 100).mapToObj(i -> UUID.randomUUID()).toList();
        when(usuarioRepository.buscarInstructoresPorCodigos(eq(ESCUELA), any())).thenReturn(List.of());

        assertThat(consultar.consultar(ADMIN, cien)).isEmpty();
    }

    @Test
    void buscar_aplicaLosValoresPorDefectoYBusquedaEnBlancoEsNula() {
        when(usuarioRepository.listarInstructoresDeEscuela(ESCUELA, null, 1, 20))
            .thenReturn(new PaginaUsuarios(List.of(), 0));

        PaginaPersonasInstructor pagina = buscar.buscar(new BuscarInstructoresComando(ADMIN, null, null, "   "));

        assertThat(pagina.elementos()).isEmpty();
        assertThat(pagina.totalPaginas()).isZero();
        assertThat(pagina.pagina()).isEqualTo(1);
        assertThat(pagina.tamano()).isEqualTo(20);
    }

    @Test
    void buscar_limitaElTamanoACienYRecortaLaBusqueda() {
        UUID a = UUID.randomUUID();
        when(usuarioRepository.listarInstructoresDeEscuela(ESCUELA, "ivan", 3, 100))
            .thenReturn(new PaginaUsuarios(List.of(instructor(a)), 201));

        PaginaPersonasInstructor pagina = buscar.buscar(new BuscarInstructoresComando(ADMIN, 3, 5000, "  ivan "));

        assertThat(pagina.elementos()).extracting(PersonaInstructor::codigo).containsExactly(a);
        assertThat(pagina.totalElementos()).isEqualTo(201);
        assertThat(pagina.totalPaginas()).isEqualTo(3);
    }

    @Test
    void buscar_conPaginaInvalida_usaLaPrimera() {
        when(usuarioRepository.listarInstructoresDeEscuela(ESCUELA, null, 1, 20))
            .thenReturn(new PaginaUsuarios(List.of(), 0));

        assertThat(buscar.buscar(new BuscarInstructoresComando(ADMIN, 0, 0, null)).pagina()).isEqualTo(1);
    }
}
