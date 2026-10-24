package com.drivehub.escuela.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.drivehub.escuela.application.dto.InstructorDetalleOutput;
import com.drivehub.escuela.application.dto.InstructorResumenOutput;
import com.drivehub.escuela.application.dto.ListarInstructoresInput;
import com.drivehub.escuela.application.dto.PaginaOutput;
import com.drivehub.escuela.common.exception.EscuelaErrorCodes;
import com.drivehub.escuela.common.exception.NotFoundException;
import com.drivehub.escuela.domain.entity.CategoriaLicencia;
import com.drivehub.escuela.domain.entity.EstadoInstructor;
import com.drivehub.escuela.domain.entity.Instructor;
import com.drivehub.escuela.domain.gateway.IdentidadNoDisponibleException;
import com.drivehub.escuela.domain.gateway.InstructorCuentaGateway;
import com.drivehub.escuela.domain.gateway.PersonaInstructor;
import com.drivehub.escuela.domain.repository.InstructorRepository;
import com.drivehub.escuela.domain.repository.Pagina;

/**
 * La autorización por rol NO se prueba acá (un caso de uso sin Spring no ejecuta @PreAuthorize): se
 * ejerce en la integración de los endpoints.
 */
@ExtendWith(MockitoExtension.class)
class ConsultaDeInstructoresUseCasesTest {

    private static final UUID ESCUELA = UUID.randomUUID();
    private static final LocalDate HOY = LocalDate.of(2026, 10, 23);
    private static final Clock RELOJ = Clock.fixed(HOY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @Mock
    private InstructorRepository repositorio;
    @Mock
    private InstructorCuentaGateway cuentas;

    private ListarInstructoresUseCase listar;
    private ObtenerInstructorUseCase obtener;
    private InstructorMapeador mapeador;

    @BeforeEach
    void crearCasosDeUso() {
        mapeador = new InstructorMapeador(RELOJ);
        listar = new ListarInstructoresUseCase(repositorio, cuentas, mapeador);
        obtener = new ObtenerInstructorUseCase(repositorio, cuentas, mapeador);
    }

    private static Instructor ficha(UUID usuarioCodigo) {
        Set<CategoriaLicencia> categorias = new LinkedHashSet<>(List.of(
            CategoriaLicencia.builder().sigla("A").descripcion("Profesional A").build(),
            CategoriaLicencia.builder().sigla("B").descripcion("Profesional B").build()));
        return Instructor.builder().codigo(UUID.randomUUID()).usuarioCodigo(usuarioCodigo).escuelaCodigo(ESCUELA)
            .nroLicencia("LIC-1").licenciaVence(LocalDate.of(2030, 1, 1)).estado(EstadoInstructor.ACTIVO)
            .fechaContratacion(LocalDate.of(2026, 1, 1)).categorias(categorias)
            .creadoEn(Instant.parse("2026-05-04T15:30:00Z")).build();
    }

    private static PersonaInstructor persona(UUID codigo, LocalDate nacimiento) {
        return new PersonaInstructor(codigo, "Ivan", "Pérez", "Ivan Pérez", "http://minio/foto", 99, nacimiento,
            "MASCULINO", "AB12345", "ivan@gmail.com", "70000000", "Av. Arce", new BigDecimal("-17.78"),
            new BigDecimal("-63.18"), LocalDate.of(2026, 5, 4));
    }

    private static ListarInstructoresInput entrada(Integer pagina, Integer tamano, String busqueda, EstadoInstructor estado) {
        return new ListarInstructoresInput(ESCUELA, pagina, tamano, busqueda, estado);
    }

    @Test
    void listar_sinBusqueda_usaLaEscuelaDelInputLosValoresPorDefectoYNoBuscaEnIdentidad() {
        UUID usuario = UUID.randomUUID();
        Instructor f = ficha(usuario);
        when(repositorio.listar(ESCUELA, null, null, 1, 20)).thenReturn(new Pagina<>(List.of(f), 1));
        when(cuentas.consultar(List.of(usuario))).thenReturn(List.of(persona(usuario, LocalDate.of(1990, 1, 1))));

        PaginaOutput<InstructorResumenOutput> pagina = listar.ejecutar(entrada(null, null, "   ", null));

        verify(cuentas, never()).buscarCodigos(any());
        assertThat(pagina.pagina()).isEqualTo(1);
        assertThat(pagina.tamano()).isEqualTo(20);
        assertThat(pagina.totalElementos()).isEqualTo(1);
        assertThat(pagina.totalPaginas()).isEqualTo(1);
        InstructorResumenOutput fila = pagina.elementos().get(0);
        assertThat(fila.codigo()).isEqualTo(f.getCodigo());
        assertThat(fila.nombreCompleto()).isEqualTo("Ivan Pérez");
        assertThat(fila.fotoUrl()).isEqualTo("http://minio/foto");
        assertThat(fila.nroLicencia()).isEqualTo("LIC-1");
        assertThat(fila.estado()).isEqualTo(EstadoInstructor.ACTIVO);
        assertThat(fila.categorias()).extracting("sigla").containsExactly("A", "B");
    }

    @Test
    void listar_limitaElTamanoACienYCorrigePaginaInvalida() {
        when(repositorio.listar(ESCUELA, EstadoInstructor.LICENCIA, null, 1, 100)).thenReturn(new Pagina<>(List.of(), 0));

        PaginaOutput<InstructorResumenOutput> pagina = listar.ejecutar(entrada(0, 5000, null, EstadoInstructor.LICENCIA));

        assertThat(pagina.tamano()).isEqualTo(100);
        assertThat(pagina.totalPaginas()).isZero();
        verifyNoInteractions(cuentas);
    }

    @Test
    void listar_conBusqueda_filtraLaTablaPorLosCodigosQueDevuelveIdentidad() {
        UUID usuario = UUID.randomUUID();
        Instructor f = ficha(usuario);
        when(cuentas.buscarCodigos("ivan")).thenReturn(List.of(usuario));
        when(repositorio.listar(ESCUELA, null, List.of(usuario), 2, 5)).thenReturn(new Pagina<>(List.of(f), 6));
        when(cuentas.consultar(List.of(usuario))).thenReturn(List.of(persona(usuario, LocalDate.of(1990, 1, 1))));

        PaginaOutput<InstructorResumenOutput> pagina = listar.ejecutar(entrada(2, 5, "  ivan ", null));

        assertThat(pagina.elementos()).hasSize(1);
        assertThat(pagina.totalElementos()).isEqualTo(6);
        assertThat(pagina.totalPaginas()).isEqualTo(2);
    }

    @Test
    void listar_conBusquedaSinCoincidencias_noConsultaLaTabla() {
        when(cuentas.buscarCodigos("nadie")).thenReturn(List.of());

        PaginaOutput<InstructorResumenOutput> pagina = listar.ejecutar(entrada(null, null, "nadie", null));

        assertThat(pagina.elementos()).isEmpty();
        assertThat(pagina.totalElementos()).isZero();
        assertThat(pagina.totalPaginas()).isZero();
        verifyNoInteractions(repositorio);
    }

    @Test
    void listar_conservaElOrdenDeLasFichasAunqueIdentidadDevuelvaLasPersonasEnOtro() {
        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();
        Instructor f1 = ficha(u1);
        Instructor f2 = ficha(u2);
        when(repositorio.listar(eq(ESCUELA), any(), any(), eq(1), eq(20))).thenReturn(new Pagina<>(List.of(f1, f2), 2));
        when(cuentas.consultar(List.of(u1, u2))).thenReturn(List.of(
            persona(u2, LocalDate.of(1990, 1, 1)), persona(u1, LocalDate.of(1990, 1, 1))));

        PaginaOutput<InstructorResumenOutput> pagina = listar.ejecutar(entrada(null, null, null, null));

        assertThat(pagina.elementos()).extracting(InstructorResumenOutput::codigo)
            .containsExactly(f1.getCodigo(), f2.getCodigo());
    }

    @Test
    void listar_siIdentidadNoDevuelveALaPersona_laFilaSigueConSusDatosDeFichaYLosDePersonaEnNulo() {
        UUID usuario = UUID.randomUUID();
        when(repositorio.listar(ESCUELA, null, null, 1, 20)).thenReturn(new Pagina<>(List.of(ficha(usuario)), 1));
        when(cuentas.consultar(List.of(usuario))).thenReturn(List.of());

        InstructorResumenOutput fila = listar.ejecutar(entrada(null, null, null, null)).elementos().get(0);

        assertThat(fila.nombreCompleto()).isNull();
        assertThat(fila.fotoUrl()).isNull();
        assertThat(fila.nroLicencia()).isEqualTo("LIC-1");
    }

    @Test
    void listar_conIdentidadCaida_laExcepcionSube() {
        UUID usuario = UUID.randomUUID();
        when(repositorio.listar(ESCUELA, null, null, 1, 20)).thenReturn(new Pagina<>(List.of(ficha(usuario)), 1));
        when(cuentas.consultar(any())).thenThrow(new IdentidadNoDisponibleException("caída"));

        assertThatThrownBy(() -> listar.ejecutar(entrada(null, null, null, null)))
            .isInstanceOf(IdentidadNoDisponibleException.class);
    }

    @Test
    void obtener_devuelveElDetalleConLaEdadCalculadaYLaFechaDeAltaDeLaFicha() {
        UUID usuario = UUID.randomUUID();
        Instructor f = ficha(usuario);
        when(repositorio.buscarPorCodigo(ESCUELA, f.getCodigo())).thenReturn(Optional.of(f));
        when(cuentas.consultar(List.of(usuario))).thenReturn(List.of(persona(usuario, LocalDate.of(1990, 10, 23))));

        InstructorDetalleOutput detalle = obtener.ejecutar(ESCUELA, f.getCodigo());

        assertThat(detalle.codigo()).isEqualTo(f.getCodigo());
        assertThat(detalle.nombres()).isEqualTo("Ivan");
        assertThat(detalle.ci()).isEqualTo("AB12345");
        assertThat(detalle.email()).isEqualTo("ivan@gmail.com");
        assertThat(detalle.edad()).isEqualTo(36);
        assertThat(detalle.fechaAlta()).isEqualTo(LocalDate.of(2026, 5, 4));
        assertThat(detalle.licenciaVence()).isEqualTo(LocalDate.of(2030, 1, 1));
        assertThat(detalle.fechaContratacion()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(detalle.categorias()).extracting("sigla").containsExactly("A", "B");
    }

    @Test
    void obtener_conInstructorInexistenteODeOtraEscuela_lanzaErrEsc09SinPedirLaPersona() {
        UUID codigo = UUID.randomUUID();
        when(repositorio.buscarPorCodigo(ESCUELA, codigo)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> obtener.ejecutar(ESCUELA, codigo)).isInstanceOfSatisfying(NotFoundException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(EscuelaErrorCodes.INSTRUCTOR_NO_ENCONTRADO));
        verifyNoInteractions(cuentas);
    }

    @Test
    void obtener_siIdentidadNoDevuelveALaPersona_losDatosDePersonaVanEnNulo() {
        UUID usuario = UUID.randomUUID();
        Instructor f = ficha(usuario);
        when(repositorio.buscarPorCodigo(ESCUELA, f.getCodigo())).thenReturn(Optional.of(f));
        when(cuentas.consultar(List.of(usuario))).thenReturn(List.of());

        InstructorDetalleOutput detalle = obtener.ejecutar(ESCUELA, f.getCodigo());

        assertThat(detalle.nombres()).isNull();
        assertThat(detalle.edad()).isNull();
        assertThat(detalle.nroLicencia()).isEqualTo("LIC-1");
    }

    @Test
    void mapeador_edadEnElDiaExactoDelCumpleanosYElAnterior() {
        Instructor f = ficha(UUID.randomUUID());

        assertThat(mapeador.aDetalle(f, persona(f.getUsuarioCodigo(), LocalDate.of(1990, 10, 23))).edad()).isEqualTo(36);
        assertThat(mapeador.aDetalle(f, persona(f.getUsuarioCodigo(), LocalDate.of(1990, 10, 24))).edad()).isEqualTo(35);
    }

    @Test
    void mapeador_fechaDeAltaSeCalculaEnLaZonaDelReloj() {
        Clock laPaz = Clock.fixed(HOY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneId.of("America/La_Paz"));
        Instructor f = ficha(UUID.randomUUID()).toBuilder().creadoEn(Instant.parse("2026-05-05T02:00:00Z")).build();

        assertThat(new InstructorMapeador(laPaz).aDetalle(f, null).fechaAlta()).isEqualTo(LocalDate.of(2026, 5, 4));
    }
}
