package com.drivehub.pagos.features.billetera;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import com.drivehub.pagos.PagosPropertiesPrueba;
import com.drivehub.pagos.common.exception.NotFoundException;
import com.drivehub.pagos.common.exception.PagosErrorCodes;
import com.drivehub.pagos.common.exception.ServicioNoDisponibleException;
import com.drivehub.pagos.config.BearerActual;
import com.drivehub.pagos.features.billetera.dto.HoraAlumnoResponse;
import com.drivehub.pagos.features.billetera.dto.HorasAlumnoDetalleResponse;
import com.drivehub.pagos.features.billetera.repository.BilleteraRepository;
import com.drivehub.pagos.features.billetera.repository.MovimientoHorasRepository;
import com.drivehub.pagos.features.billetera.service.HorasAlumnosService;
import com.drivehub.pagos.features.pagos.client.EscuelaClient;
import com.drivehub.pagos.features.pagos.client.IdentidadClient;
import com.drivehub.pagos.features.pagos.client.IdentidadClient.NombreEstudiante;
import com.drivehub.pagos.features.pagos.enums.EstadoPago;
import com.drivehub.pagos.features.pagos.enums.EstadoPagoVisible;
import com.drivehub.pagos.features.pagos.enums.EstadoReversion;
import com.drivehub.pagos.features.pagos.model.Pago;
import com.drivehub.pagos.features.pagos.repository.PagoRepository;
import com.drivehub.pagos.features.pagos.repository.ReversionPagoRepository;
import com.drivehub.pagos.features.planes.model.PlanPago;

/**
 * La autorización por rol NO se prueba acá (un servicio sin Spring no ejecuta @PreAuthorize): ver
 * `HorasAlumnosAutorizacionTest`.
 */
@ExtendWith(MockitoExtension.class)
class HorasAlumnosServiceTest {

    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID INSTRUCTOR = UUID.randomUUID();
    private static final String BEARER = "Bearer token-del-instructor";

    @Mock
    private EscuelaClient escuela;
    @Mock
    private IdentidadClient identidad;
    @Mock
    private BearerActual bearerActual;
    @Mock
    private PagoRepository pagos;
    @Mock
    private BilleteraRepository billeteras;
    @Mock
    private MovimientoHorasRepository movimientos;
    @Mock
    private ReversionPagoRepository reversiones;

    private HorasAlumnosService servicio;

    @BeforeEach
    void preparar() {
        servicio = new HorasAlumnosService(escuela, identidad, bearerActual, pagos, billeteras, movimientos,
            reversiones);
        when(bearerActual.cabecera()).thenReturn(BEARER);
    }

    private static UUID alumno(int n) {
        return new UUID(0, n);
    }

    private void losAlumnosSon(Map<UUID, NombreEstudiante> nombres) {
        List<UUID> codigos = List.copyOf(nombres.keySet());
        when(escuela.alumnosDelInstructor(BEARER)).thenReturn(codigos);
        when(identidad.nombresDe(codigos, BEARER)).thenReturn(nombres);
    }

    private static List<Object[]> filas(Object[]... filas) {
        return List.of(filas);
    }

    private static Object[] fila(Object... valores) {
        return valores;
    }

    // ------------------------------------------------------------------ listar

    @Test
    void listar_ordenaPorApellidosYNombres_sinDistinguirMayusculasNiTildes() {
        losAlumnosSon(Map.of(
            alumno(1), new NombreEstudiante("Luis", "Zapata"),
            alumno(2), new NombreEstudiante("Ana", "Álvarez"),
            alumno(3), new NombreEstudiante("Beto", "Álvarez"),
            alumno(4), new NombreEstudiante("Carla", "mamani")));

        Page<HoraAlumnoResponse> pagina = servicio.listar(ESCUELA, INSTRUCTOR, null, null, null);

        assertThat(pagina.getContent()).extracting(HoraAlumnoResponse::nombres)
            .containsExactly("Ana", "Beto", "Carla", "Luis");
        assertThat(pagina.getTotalElements()).isEqualTo(4);
        assertThat(pagina.getSize()).isEqualTo(20);
    }

    @Test
    void listar_unAlumnoSinComprasNiBilletera_apareceConCeros() {
        losAlumnosSon(Map.of(alumno(1), new NombreEstudiante("Ana", "Pérez")));

        HoraAlumnoResponse fila = servicio.listar(ESCUELA, INSTRUCTOR, null, null, null).getContent().get(0);

        assertThat(fila.estudianteCodigo()).isEqualTo(alumno(1));
        assertThat(fila.nombres()).isEqualTo("Ana");
        assertThat(fila.apellidos()).isEqualTo("Pérez");
        assertThat(fila.horasCompradas()).isZero();
        assertThat(fila.horasGastadasConmigo()).isZero();
        assertThat(fila.horasRestantes()).isZero();
    }

    @Test
    void listar_calculaLosTresTotales_conUnaConsultaPorTotalParaTodosLosDeLaPagina() {
        losAlumnosSon(Map.of(
            alumno(1), new NombreEstudiante("Ana", "Alfa"),
            alumno(2), new NombreEstudiante("Beto", "Beta")));
        when(pagos.horasCompradasPor(eq(ESCUELA), anyCollection())).thenReturn(filas(fila(alumno(1), 20L)));
        when(movimientos.horasGastadasConInstructor(eq(ESCUELA), eq(INSTRUCTOR), anyCollection()))
            .thenReturn(filas(fila(alumno(1), 6L)));
        when(billeteras.horasDisponiblesDe(eq(ESCUELA), anyCollection()))
            .thenReturn(filas(fila(alumno(1), 14), fila(alumno(2), 3)));

        List<HoraAlumnoResponse> filas = servicio.listar(ESCUELA, INSTRUCTOR, null, null, null).getContent();

        assertThat(filas).extracting(HoraAlumnoResponse::horasCompradas).containsExactly(20, 0);
        assertThat(filas).extracting(HoraAlumnoResponse::horasGastadasConmigo).containsExactly(6, 0);
        assertThat(filas).extracting(HoraAlumnoResponse::horasRestantes).containsExactly(14, 3);
        verify(pagos).horasCompradasPor(eq(ESCUELA), anyCollection());
        verify(movimientos).horasGastadasConInstructor(eq(ESCUELA), eq(INSTRUCTOR), anyCollection());
        verify(billeteras).horasDisponiblesDe(eq(ESCUELA), anyCollection());
    }

    @Test
    void listar_lasHorasGastadasSonSoloLasDelInstructorDelToken() {
        losAlumnosSon(Map.of(alumno(1), new NombreEstudiante("Ana", "Alfa")));

        servicio.listar(ESCUELA, INSTRUCTOR, null, null, null);

        // * El repositorio recibe al instructor del token: lo gastado con otros no se puede pedir
        verify(movimientos).horasGastadasConInstructor(eq(ESCUELA), eq(INSTRUCTOR), anyCollection());
    }

    @Test
    void listar_unNetoNegativoDeHorasGastadas_seMuestraComoCero() {
        losAlumnosSon(Map.of(alumno(1), new NombreEstudiante("Ana", "Alfa")));
        when(movimientos.horasGastadasConInstructor(eq(ESCUELA), eq(INSTRUCTOR), anyCollection()))
            .thenReturn(filas(fila(alumno(1), -2L)));

        assertThat(servicio.listar(ESCUELA, INSTRUCTOR, null, null, null).getContent().get(0).horasGastadasConmigo())
            .isZero();
    }

    @Test
    void listar_buscaPorNombreCompleto_sinDistinguirMayusculasNiTildes() {
        losAlumnosSon(Map.of(
            alumno(1), new NombreEstudiante("José Luis", "Pérez"),
            alumno(2), new NombreEstudiante("Marta", "Gómez")));

        assertThat(servicio.listar(ESCUELA, INSTRUCTOR, "luis perez", null, null).getContent())
            .extracting(HoraAlumnoResponse::estudianteCodigo).containsExactly(alumno(1));
        assertThat(servicio.listar(ESCUELA, INSTRUCTOR, "GOMEZ", null, null).getContent())
            .extracting(HoraAlumnoResponse::estudianteCodigo).containsExactly(alumno(2));
        assertThat(servicio.listar(ESCUELA, INSTRUCTOR, "nadie", null, null).getContent()).isEmpty();
        assertThat(servicio.listar(ESCUELA, INSTRUCTOR, "   ", null, null).getTotalElements()).isEqualTo(2);
    }

    @Test
    void listar_paginaEnMemoria_yCalculaLosTotalesSoloDeLaPagina() {
        Map<UUID, NombreEstudiante> todos = new java.util.LinkedHashMap<>();
        for (int i = 1; i <= 5; i++) {
            todos.put(alumno(i), new NombreEstudiante("Nombre" + i, "Apellido" + i));
        }
        losAlumnosSon(todos);

        Page<HoraAlumnoResponse> segunda = servicio.listar(ESCUELA, INSTRUCTOR, null, 2, 2);

        assertThat(segunda.getContent()).extracting(HoraAlumnoResponse::estudianteCodigo)
            .containsExactly(alumno(3), alumno(4));
        assertThat(segunda.getTotalElements()).isEqualTo(5);
        assertThat(segunda.getTotalPages()).isEqualTo(3);
        assertThat(segunda.getNumber()).isEqualTo(1);
        verify(pagos).horasCompradasPor(eq(ESCUELA), org.mockito.ArgumentMatchers.argThat(
            c -> c.size() == 2 && c.contains(alumno(3)) && c.contains(alumno(4))));
    }

    @Test
    void listar_conUnTamanoMayorAlTopeUsaCien_yUnaPaginaPasadaDelFinalDevuelveVacio() {
        losAlumnosSon(Map.of(alumno(1), new NombreEstudiante("Ana", "Alfa")));

        assertThat(servicio.listar(ESCUELA, INSTRUCTOR, null, 1, 500).getSize()).isEqualTo(100);
        Page<HoraAlumnoResponse> pasada = servicio.listar(ESCUELA, INSTRUCTOR, null, 9, 20);
        assertThat(pasada.getContent()).isEmpty();
        assertThat(pasada.getTotalElements()).isEqualTo(1);
    }

    @Test
    void listar_unInstructorSinAlumnos_devuelveUnaPaginaVaciaSinLlamarAIdentidadNiALasConsultas() {
        when(escuela.alumnosDelInstructor(BEARER)).thenReturn(List.of());

        Page<HoraAlumnoResponse> pagina = servicio.listar(ESCUELA, INSTRUCTOR, null, null, null);

        assertThat(pagina.getContent()).isEmpty();
        assertThat(pagina.getTotalElements()).isZero();
        verifyNoInteractions(identidad, pagos, billeteras, movimientos);
    }

    @Test
    void listar_unAlumnoQueIdentidadNoDevuelve_noSeMuestra() {
        UUID conocido = alumno(1);
        UUID perdido = alumno(2);
        when(escuela.alumnosDelInstructor(BEARER)).thenReturn(List.of(conocido, perdido));
        when(identidad.nombresDe(List.of(conocido, perdido), BEARER))
            .thenReturn(Map.of(conocido, new NombreEstudiante("Ana", "Alfa")));

        assertThat(servicio.listar(ESCUELA, INSTRUCTOR, null, null, null).getContent())
            .extracting(HoraAlumnoResponse::estudianteCodigo).containsExactly(conocido);
    }

    @Test
    void listar_conEscuelaOIdentidadCaidas_propagaErrPag15() {
        when(escuela.alumnosDelInstructor(BEARER)).thenThrow(caido());
        assertThatThrownBy(() -> servicio.listar(ESCUELA, INSTRUCTOR, null, null, null))
            .isInstanceOfSatisfying(ServicioNoDisponibleException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo("ERR_PAG_15"));

        List<UUID> codigos = List.of(alumno(1));
        doReturn(codigos).when(escuela).alumnosDelInstructor(BEARER);
        when(identidad.nombresDe(codigos, BEARER)).thenThrow(caido());
        assertThatThrownBy(() -> servicio.listar(ESCUELA, INSTRUCTOR, null, null, null))
            .isInstanceOf(ServicioNoDisponibleException.class);
        verifyNoInteractions(pagos, billeteras, movimientos);
    }

    private static ServicioNoDisponibleException caido() {
        return new ServicioNoDisponibleException(PagosErrorCodes.SERVICIO_DEPENDIENTE_NO_DISPONIBLE, "caído");
    }

    // ------------------------------------------------------------------ detalle

    private Pago pago(EstadoPago estado, int id) {
        PlanPago plan = new PlanPago(ESCUELA, "Plan 10 horas", (short) 10, new BigDecimal("450.00"));
        Pago pago = new Pago(ESCUELA, alumno(1), plan, new BigDecimal("38.23"),
            new BigDecimal(PagosPropertiesPrueba.TIPO_CAMBIO), "Ana", "Escuela", "clave-" + id,
            Instant.parse("2026-10-25T12:00:00Z"));
        pago.setEstado(estado);
        ReflectionTestUtils.setField(pago, "id", (long) id);
        ReflectionTestUtils.setField(pago, "codigo", new UUID(1, id));
        ReflectionTestUtils.setField(pago, "createdAt", Instant.parse("2026-10-25T12:00:00Z"));
        return pago;
    }

    @Test
    void detalle_deUnAlumnoAjenoOInexistente_lanzaErrPag12SinConsultarNada() {
        when(escuela.esMiAlumno(alumno(9), BEARER)).thenReturn(false);

        assertThatThrownBy(() -> servicio.detalle(ESCUELA, INSTRUCTOR, alumno(9)))
            .isInstanceOfSatisfying(NotFoundException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo("ERR_PAG_12"));
        verify(identidad, never()).nombresDe(any(), any());
        verifyNoInteractions(pagos, billeteras, movimientos);
    }

    @Test
    void detalle_conUnAlumnoQueIdentidadNoConoce_lanzaErrPag12() {
        when(escuela.esMiAlumno(alumno(1), BEARER)).thenReturn(true);
        when(identidad.nombresDe(List.of(alumno(1)), BEARER)).thenReturn(Map.of());

        assertThatThrownBy(() -> servicio.detalle(ESCUELA, INSTRUCTOR, alumno(1)))
            .isInstanceOfSatisfying(NotFoundException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo("ERR_PAG_12"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void detalle_armaElResumen_losConsumosPorClase_yLosPagosConSuEstadoVisible() {
        when(escuela.esMiAlumno(alumno(1), BEARER)).thenReturn(true);
        when(identidad.nombresDe(List.of(alumno(1)), BEARER))
            .thenReturn(Map.of(alumno(1), new NombreEstudiante("Ana", "Pérez")));
        when(pagos.horasCompradasPor(eq(ESCUELA), anyCollection())).thenReturn(filas(fila(alumno(1), 20L)));
        when(movimientos.horasGastadasConInstructor(eq(ESCUELA), eq(INSTRUCTOR), anyCollection()))
            .thenReturn(filas(fila(alumno(1), 3L)));
        when(billeteras.horasDisponiblesDe(eq(ESCUELA), anyCollection())).thenReturn(filas(fila(alumno(1), 17)));
        UUID clase = UUID.randomUUID();
        Instant fecha = Instant.parse("2026-10-20T15:00:00Z");
        when(movimientos.consumosPorClase(ESCUELA, INSTRUCTOR, alumno(1)))
            .thenReturn(filas(fila(clase, fecha, 3L)));
        Pago confirmado = pago(EstadoPago.CONFIRMADO, 1);
        Pago enProceso = pago(EstadoPago.CONFIRMADO, 2);
        Pago fallido = pago(EstadoPago.FALLIDO, 3);
        when(pagos.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(confirmado, enProceso, fallido)));
        when(reversiones.pagoIdsConReversion(anyCollection(), eq(EstadoReversion.SOLICITADA)))
            .thenReturn(List.of(2L));

        HorasAlumnoDetalleResponse detalle = servicio.detalle(ESCUELA, INSTRUCTOR, alumno(1));

        assertThat(detalle.resumen().nombres()).isEqualTo("Ana");
        assertThat(detalle.resumen().horasCompradas()).isEqualTo(20);
        assertThat(detalle.resumen().horasGastadasConmigo()).isEqualTo(3);
        assertThat(detalle.resumen().horasRestantes()).isEqualTo(17);
        assertThat(detalle.consumos()).hasSize(1);
        assertThat(detalle.consumos().get(0).claseCodigo()).isEqualTo(clase);
        assertThat(detalle.consumos().get(0).fecha()).isEqualTo(fecha);
        assertThat(detalle.consumos().get(0).horas()).isEqualTo(3);
        assertThat(detalle.pagos()).extracting(p -> p.estado()).containsExactly(EstadoPagoVisible.CONFIRMADO,
            EstadoPagoVisible.REEMBOLSO_EN_PROCESO, EstadoPagoVisible.FALLIDO);
        assertThat(detalle.pagos().get(0).planNombre()).isEqualTo("Plan 10 horas");
        assertThat(detalle.pagos().get(0).horas()).isEqualTo((short) 10);
    }

    @Test
    @SuppressWarnings("unchecked")
    void detalle_deUnAlumnoSinClasesNiCompras_muestraCerosYListasVacias() {
        when(escuela.esMiAlumno(alumno(1), BEARER)).thenReturn(true);
        when(identidad.nombresDe(List.of(alumno(1)), BEARER))
            .thenReturn(Map.of(alumno(1), new NombreEstudiante("Ana", "Pérez")));
        when(pagos.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        HorasAlumnoDetalleResponse detalle = servicio.detalle(ESCUELA, INSTRUCTOR, alumno(1));

        assertThat(detalle.resumen().horasCompradas()).isZero();
        assertThat(detalle.resumen().horasGastadasConmigo()).isZero();
        assertThat(detalle.resumen().horasRestantes()).isZero();
        assertThat(detalle.consumos()).isEmpty();
        assertThat(detalle.pagos()).isEmpty();
        verifyNoInteractions(reversiones);
    }

    @Test
    void detalle_conEscuelaCaida_propagaErrPag15() {
        when(escuela.esMiAlumno(alumno(1), BEARER)).thenThrow(caido());

        assertThatThrownBy(() -> servicio.detalle(ESCUELA, INSTRUCTOR, alumno(1)))
            .isInstanceOf(ServicioNoDisponibleException.class);
    }
}
