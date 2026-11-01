package com.drivehub.escuela.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.drivehub.escuela.application.dto.ConsultarDisponibilidadInput;
import com.drivehub.escuela.application.dto.DisponibilidadInstructorOutput;
import com.drivehub.escuela.application.dto.PaginaOutput;
import com.drivehub.escuela.common.exception.BusinessException;
import com.drivehub.escuela.common.exception.EscuelaErrorCodes;
import com.drivehub.escuela.common.exception.ServicioNoDisponibleException;
import com.drivehub.escuela.domain.entity.AsignacionVehiculo;
import com.drivehub.escuela.domain.entity.Disponibilidad;
import com.drivehub.escuela.domain.entity.EstadoAutos;
import com.drivehub.escuela.domain.entity.EstadoInstructor;
import com.drivehub.escuela.domain.entity.EstadoVehiculo;
import com.drivehub.escuela.domain.entity.Instructor;
import com.drivehub.escuela.domain.entity.MomentoDelDia;
import com.drivehub.escuela.domain.entity.Transmision;
import com.drivehub.escuela.domain.entity.Vehiculo;
import com.drivehub.escuela.domain.gateway.HorariosGateway;
import com.drivehub.escuela.domain.gateway.IdentidadNoDisponibleException;
import com.drivehub.escuela.domain.gateway.InstructorCuentaGateway;
import com.drivehub.escuela.domain.gateway.PersonaInstructor;
import com.drivehub.escuela.domain.gateway.ResumenFranjas;
import com.drivehub.escuela.domain.repository.AsignacionDeAuto;
import com.drivehub.escuela.domain.repository.AsignacionVehiculoRepository;
import com.drivehub.escuela.domain.repository.InstructorRepository;
import com.drivehub.escuela.domain.repository.Pagina;

/**
 * CU11: la disponibilidad de los instructores de la escuela. La autorización por rol NO se prueba acá
 * (un caso de uso sin Spring no ejecuta @PreAuthorize).
 */
@ExtendWith(MockitoExtension.class)
class ConsultarDisponibilidadUseCaseTest {

    private static final ZoneId ZONA = ZoneId.of("America/La_Paz");
    private static final LocalDate HOY = LocalDate.of(2026, 10, 26);
    private static final UUID ESCUELA = UUID.randomUUID();

    @Mock
    private InstructorRepository instructores;
    @Mock
    private AsignacionVehiculoRepository asignaciones;
    @Mock
    private HorariosGateway horarios;
    @Mock
    private InstructorCuentaGateway cuentas;

    private ConsultarDisponibilidadUseCase consultar;

    // * Cuatro instructores, del alta más reciente al más antiguo
    private final UUID usuarioAna = UUID.randomUUID();
    private final UUID usuarioLuis = UUID.randomUUID();
    private final UUID usuarioMaria = UUID.randomUUID();
    private final UUID usuarioPedro = UUID.randomUUID();
    private final Instructor ana = ficha(usuarioAna, EstadoInstructor.ACTIVO);
    private final Instructor luis = ficha(usuarioLuis, EstadoInstructor.ACTIVO);
    private final Instructor maria = ficha(usuarioMaria, EstadoInstructor.LICENCIA);
    private final Instructor pedro = ficha(usuarioPedro, EstadoInstructor.ACTIVO);

    @BeforeEach
    void setUp() {
        consultar = new ConsultarDisponibilidadUseCase(instructores, asignaciones, horarios, cuentas,
            Clock.fixed(HOY.atTime(9, 0).atZone(ZONA).toInstant(), ZONA));
    }

    private static Instructor ficha(UUID usuario, EstadoInstructor estado) {
        return Instructor.builder().codigo(UUID.randomUUID()).usuarioCodigo(usuario).escuelaCodigo(ESCUELA)
            .nroLicencia("L-" + usuario).estado(estado).licenciaVence(HOY.plusYears(1)).build();
    }

    private static AsignacionDeAuto auto(UUID usuario, String placa, String marca, String modelo,
            EstadoVehiculo estado) {
        Vehiculo vehiculo = Vehiculo.builder().codigo(UUID.randomUUID()).escuelaCodigo(ESCUELA).placa(placa)
            .marca(marca).modelo(modelo).transmision(Transmision.MANUAL).estado(estado).build();
        return new AsignacionDeAuto(vehiculo, AsignacionVehiculo.builder().codigo(UUID.randomUUID())
            .vehiculoCodigo(vehiculo.getCodigo()).instructorCodigo(UUID.randomUUID())
            .fechaInicio(HOY.minusDays(10)).build(), usuario);
    }

    private static PersonaInstructor persona(UUID usuario, String nombre) {
        return new PersonaInstructor(usuario, null, null, nombre, null, 30, null, null, null, null, null, null,
            null, null, null);
    }

    private static ConsultarDisponibilidadInput input() {
        return new ConsultarDisponibilidadInput(ESCUELA, null, null, null, null, null, null, null);
    }

    private static ConsultarDisponibilidadInput input(String busqueda, Disponibilidad disponibilidad,
            EstadoAutos estadoAutos) {
        return new ConsultarDisponibilidadInput(ESCUELA, null, null, busqueda, null, null, disponibilidad,
            estadoAutos);
    }

    /**
     * Ana: dos libres y un auto disponible. Luis: franjas pero ninguna libre y un auto en mantenimiento.
     * María (licencia): franjas libres y sin auto. Pedro: sin franjas y sin auto.
     */
    private void escenarioBase() {
        when(instructores.listar(eq(ESCUELA), eq(null), eq(null), eq(1), eq(500))).thenReturn(
            new Pagina<>(List.of(ana, luis, maria, pedro), 4));
        when(asignaciones.vigentesDeInstructores(eq(ESCUELA), any())).thenReturn(List.of(
            auto(usuarioAna, "ABC-123", "Toyota", "Yaris", EstadoVehiculo.DISPONIBLE),
            auto(usuarioLuis, "XYZ-999", "Suzuki", "Swift", EstadoVehiculo.MANTENIMIENTO)));
        when(horarios.resumenDelDia(any(), any())).thenReturn(Map.of(
            usuarioAna, new ResumenFranjas(4, 2, 2, 0),
            usuarioLuis, new ResumenFranjas(3, 0, 3, 0),
            usuarioMaria, new ResumenFranjas(2, 2, 0, 0)));
    }

    private void conNombres() {
        when(cuentas.consultar(any())).thenAnswer(invocacion -> {
            java.util.Collection<UUID> pedidos = invocacion.getArgument(0);
            return pedidos.stream().map(u -> persona(u, "Nombre de " + (u.equals(usuarioAna) ? "Ana"
                : u.equals(usuarioLuis) ? "Luis" : u.equals(usuarioMaria) ? "Maria" : "Pedro"))).toList();
        });
    }

    private static DisponibilidadInstructorOutput de(PaginaOutput<DisponibilidadInstructorOutput> pagina,
            Instructor ficha) {
        return pagina.elementos().stream().filter(e -> e.instructorCodigo().equals(ficha.getCodigo()))
            .findFirst().orElseThrow();
    }

    private static void assertError(Runnable accion, String codigo) {
        assertThatThrownBy(accion::run).isInstanceOfSatisfying(BusinessException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(codigo));
    }

    @Test
    void ejecutar_calculaLaDisponibilidadYElEstadoDeLosAutosDeCadaInstructor() {
        escenarioBase();
        conNombres();

        PaginaOutput<DisponibilidadInstructorOutput> pagina = consultar.ejecutar(input());

        assertThat(pagina.totalElementos()).isEqualTo(4);
        DisponibilidadInstructorOutput deAna = de(pagina, ana);
        assertThat(deAna.disponibilidad()).isEqualTo(Disponibilidad.DISPONIBLE);
        assertThat(deAna.estadoAutos()).isEqualTo(EstadoAutos.DISPONIBLE);
        assertThat(deAna.libres()).isEqualTo(2);
        assertThat(deAna.reservadas()).isEqualTo(2);
        assertThat(deAna.nombreCompleto()).isEqualTo("Nombre de Ana");
        assertThat(deAna.estadoInstructor()).isEqualTo(EstadoInstructor.ACTIVO);
        assertThat(deAna.autos()).extracting(a -> a.placa()).containsExactly("ABC-123");

        DisponibilidadInstructorOutput deLuis = de(pagina, luis);
        assertThat(deLuis.disponibilidad()).isEqualTo(Disponibilidad.COMPLETO);
        assertThat(deLuis.estadoAutos()).isEqualTo(EstadoAutos.NO_DISPONIBLE);

        // * Licencia: no está disponible aunque tenga franjas libres
        DisponibilidadInstructorOutput deMaria = de(pagina, maria);
        assertThat(deMaria.disponibilidad()).isEqualTo(Disponibilidad.NO_DISPONIBLE);
        assertThat(deMaria.estadoAutos()).isEqualTo(EstadoAutos.SIN_AUTO);
        assertThat(deMaria.autos()).isEmpty();

        // * Sin entrada en el resumen: no tiene franjas
        DisponibilidadInstructorOutput dePedro = de(pagina, pedro);
        assertThat(dePedro.disponibilidad()).isEqualTo(Disponibilidad.SIN_HORARIO);
        assertThat(dePedro.libres()).isZero();
        assertThat(dePedro.reservadas()).isZero();
    }

    @Test
    void ejecutar_ordenaDisponiblesPrimeroYNoDisponiblesAlFinal() {
        escenarioBase();
        conNombres();

        List<UUID> orden = consultar.ejecutar(input()).elementos().stream()
            .map(DisponibilidadInstructorOutput::instructorCodigo).toList();

        assertThat(orden).containsExactly(ana.getCodigo(), luis.getCodigo(), pedro.getCodigo(), maria.getCodigo());
    }

    @Test
    void ejecutar_dentroDeUnMismoGrupo_ganaElQueTieneMasFranjasLibres() {
        when(instructores.listar(eq(ESCUELA), eq(null), eq(null), eq(1), eq(500)))
            .thenReturn(new Pagina<>(List.of(ana, luis), 2));
        when(asignaciones.vigentesDeInstructores(eq(ESCUELA), any())).thenReturn(List.of());
        when(horarios.resumenDelDia(any(), any())).thenReturn(Map.of(
            usuarioAna, new ResumenFranjas(2, 1, 1, 0), usuarioLuis, new ResumenFranjas(5, 5, 0, 0)));
        conNombres();

        List<UUID> orden = consultar.ejecutar(input()).elementos().stream()
            .map(DisponibilidadInstructorOutput::instructorCodigo).toList();

        assertThat(orden).containsExactly(luis.getCodigo(), ana.getCodigo());
    }

    @Test
    void ejecutar_sinFecha_consultaHoyYPasaElMomentoDelDia() {
        escenarioBase();
        conNombres();

        consultar.ejecutar(new ConsultarDisponibilidadInput(ESCUELA, null, null, null, null, MomentoDelDia.TARDE,
            null, null));

        verify(horarios).resumenDelDia(HOY, MomentoDelDia.TARDE);
    }

    @Test
    void ejecutar_conUnaFechaFutura_laPasaAAcademico() {
        escenarioBase();
        conNombres();

        consultar.ejecutar(new ConsultarDisponibilidadInput(ESCUELA, null, null, null, HOY.plusDays(3), null, null,
            null));

        verify(horarios).resumenDelDia(HOY.plusDays(3), null);
    }

    @Test
    void ejecutar_conUnaFechaAnteriorAHoy_fallaConErrSys01SinConsultarNada() {
        assertError(() -> consultar.ejecutar(new ConsultarDisponibilidadInput(ESCUELA, null, null, null,
            HOY.minusDays(1), null, null, null)), "ERR_SYS_01");

        verifyNoMoreInteractions(instructores, asignaciones, horarios, cuentas);
    }

    @Test
    void ejecutar_filtraPorDisponibilidad() {
        escenarioBase();
        conNombres();

        PaginaOutput<DisponibilidadInstructorOutput> pagina = consultar.ejecutar(
            input(null, Disponibilidad.COMPLETO, null));

        assertThat(pagina.elementos()).extracting(DisponibilidadInstructorOutput::instructorCodigo)
            .containsExactly(luis.getCodigo());
        assertThat(pagina.totalElementos()).isEqualTo(1);
    }

    @Test
    void ejecutar_filtraPorEstadoDeLosAutos() {
        escenarioBase();
        conNombres();

        assertThat(consultar.ejecutar(input(null, null, EstadoAutos.SIN_AUTO)).elementos())
            .extracting(DisponibilidadInstructorOutput::instructorCodigo)
            .containsExactlyInAnyOrder(maria.getCodigo(), pedro.getCodigo());
        assertThat(consultar.ejecutar(input(null, null, EstadoAutos.NO_DISPONIBLE)).elementos())
            .extracting(DisponibilidadInstructorOutput::instructorCodigo).containsExactly(luis.getCodigo());
    }

    @Test
    void ejecutar_combinaLosDosFiltros() {
        escenarioBase();
        conNombres();

        assertThat(consultar.ejecutar(input(null, Disponibilidad.DISPONIBLE, EstadoAutos.DISPONIBLE)).elementos())
            .extracting(DisponibilidadInstructorOutput::instructorCodigo).containsExactly(ana.getCodigo());
        assertThat(consultar.ejecutar(input(null, Disponibilidad.DISPONIBLE, EstadoAutos.SIN_AUTO)).elementos())
            .isEmpty();
    }

    @Test
    void ejecutar_buscarPorNombre_pideLosCodigosAIdentidadYTraeSoloAEsaPersona() {
        escenarioBase();
        conNombres();
        when(cuentas.buscarCodigos("luis")).thenReturn(List.of(usuarioLuis));

        PaginaOutput<DisponibilidadInstructorOutput> pagina = consultar.ejecutar(input("luis", null, null));

        assertThat(pagina.elementos()).extracting(DisponibilidadInstructorOutput::instructorCodigo)
            .containsExactly(luis.getCodigo());
    }

    @Test
    void ejecutar_buscarPorPlacaMarcaOModelo_sinDistinguirMayusculasNiTildes() {
        escenarioBase();
        conNombres();
        when(cuentas.buscarCodigos(any())).thenReturn(List.of());

        assertThat(consultar.ejecutar(input("abc-123", null, null)).elementos())
            .extracting(DisponibilidadInstructorOutput::instructorCodigo).containsExactly(ana.getCodigo());
        assertThat(consultar.ejecutar(input("SUZUKI", null, null)).elementos())
            .extracting(DisponibilidadInstructorOutput::instructorCodigo).containsExactly(luis.getCodigo());
        assertThat(consultar.ejecutar(input("yarís", null, null)).elementos())
            .extracting(DisponibilidadInstructorOutput::instructorCodigo).containsExactly(ana.getCodigo());
    }

    @Test
    void ejecutar_buscarSinCoincidencias_devuelveLaPaginaVaciaSinError() {
        escenarioBase();
        when(cuentas.buscarCodigos("zzz")).thenReturn(List.of());

        PaginaOutput<DisponibilidadInstructorOutput> pagina = consultar.ejecutar(input("zzz", null, null));

        assertThat(pagina.elementos()).isEmpty();
        assertThat(pagina.totalElementos()).isZero();
        assertThat(pagina.totalPaginas()).isZero();
        verify(cuentas, never()).consultar(any());
    }

    @Test
    void ejecutar_sinBusqueda_noLlamaALaBusquedaDeIdentidad() {
        escenarioBase();
        conNombres();

        consultar.ejecutar(input());

        verify(cuentas, never()).buscarCodigos(any());
    }

    @Test
    void ejecutar_escuelaSinInstructores_devuelveVacioSinPedirNadaMas() {
        when(instructores.listar(eq(ESCUELA), eq(null), eq(null), eq(1), eq(500)))
            .thenReturn(new Pagina<>(List.of(), 0));

        PaginaOutput<DisponibilidadInstructorOutput> pagina = consultar.ejecutar(input());

        assertThat(pagina.elementos()).isEmpty();
        assertThat(pagina.totalElementos()).isZero();
        verify(asignaciones, never()).vigentesDeInstructores(any(), any());
        verify(horarios, never()).resumenDelDia(any(), any());
    }

    @Test
    void ejecutar_pagina_cortaElResultadoYPideLosNombresSoloDeEsaPagina() {
        escenarioBase();
        when(cuentas.consultar(any())).thenAnswer(i -> ((java.util.Collection<UUID>) i.getArgument(0)).stream()
            .map(u -> persona(u, "Persona")).toList());

        PaginaOutput<DisponibilidadInstructorOutput> segunda = consultar.ejecutar(
            new ConsultarDisponibilidadInput(ESCUELA, 2, 3, null, null, null, null, null));

        assertThat(segunda.pagina()).isEqualTo(2);
        assertThat(segunda.tamano()).isEqualTo(3);
        assertThat(segunda.totalElementos()).isEqualTo(4);
        assertThat(segunda.totalPaginas()).isEqualTo(2);
        // * Ordenados: ana, luis, pedro | maria. La segunda página solo trae a María
        assertThat(segunda.elementos()).extracting(DisponibilidadInstructorOutput::instructorCodigo)
            .containsExactly(maria.getCodigo());
        verify(cuentas).consultar(List.of(usuarioMaria));
    }

    @Test
    void ejecutar_unaPaginaMasAllaDelFinal_devuelveVacia() {
        escenarioBase();

        PaginaOutput<DisponibilidadInstructorOutput> pagina = consultar.ejecutar(
            new ConsultarDisponibilidadInput(ESCUELA, 9, 20, null, null, null, null, null));

        assertThat(pagina.elementos()).isEmpty();
        assertThat(pagina.totalElementos()).isEqualTo(4);
        verify(cuentas, never()).consultar(any());
    }

    @Test
    void ejecutar_tamanoMayorAlMaximo_seLimitaA100() {
        escenarioBase();
        conNombres();

        assertThat(consultar.ejecutar(new ConsultarDisponibilidadInput(ESCUELA, null, 5000, null, null, null, null,
            null)).tamano()).isEqualTo(100);
    }

    @Test
    void ejecutar_unNombreQueIdentidadNoDevuelve_quedaNuloSinRomperLaConsulta() {
        escenarioBase();
        when(cuentas.consultar(any())).thenReturn(List.of(persona(usuarioAna, "Ana Pérez")));

        PaginaOutput<DisponibilidadInstructorOutput> pagina = consultar.ejecutar(input());

        assertThat(de(pagina, ana).nombreCompleto()).isEqualTo("Ana Pérez");
        assertThat(de(pagina, luis).nombreCompleto()).isNull();
        assertThat(pagina.totalElementos()).isEqualTo(4);
    }

    @Test
    void ejecutar_siAcademicoNoResponde_sePropagaErrEsc20SinDevolverUnListadoParcial() {
        when(instructores.listar(eq(ESCUELA), eq(null), eq(null), eq(1), eq(500)))
            .thenReturn(new Pagina<>(List.of(ana), 1));
        when(asignaciones.vigentesDeInstructores(eq(ESCUELA), any())).thenReturn(List.of());
        when(horarios.resumenDelDia(any(), any())).thenThrow(new ServicioNoDisponibleException(
            EscuelaErrorCodes.HORARIOS_NO_DISPONIBLES, "No pudimos consultar los horarios"));

        assertError(() -> consultar.ejecutar(input()), "ERR_ESC_20");
        verify(cuentas, never()).consultar(any());
    }

    @Test
    void ejecutar_siIdentidadNoResponde_sePropagaLaFalla() {
        escenarioBase();
        when(cuentas.consultar(any())).thenThrow(new IdentidadNoDisponibleException("sin conexión"));

        assertThatThrownBy(() -> consultar.ejecutar(input())).isInstanceOf(IdentidadNoDisponibleException.class);
    }

    @Test
    void ejecutar_esDeSoloLectura_noEscribeEnNingunRepositorio() {
        escenarioBase();
        conNombres();

        consultar.ejecutar(input());

        verify(instructores).listar(any(), any(), any(), eq(1), eq(500));
        verifyNoMoreInteractions(instructores);
        verify(asignaciones).vigentesDeInstructores(any(), any());
        verifyNoMoreInteractions(asignaciones);
    }
}
