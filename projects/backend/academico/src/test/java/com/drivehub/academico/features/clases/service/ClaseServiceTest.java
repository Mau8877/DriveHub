package com.drivehub.academico.features.clases.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.prepost.PreAuthorize;

import com.drivehub.academico.common.exception.AcademicoErrorCodes;
import com.drivehub.academico.common.exception.BusinessException;
import com.drivehub.academico.common.exception.ConflictException;
import com.drivehub.academico.common.exception.NotFoundException;
import com.drivehub.academico.common.exception.ServicioNoDisponibleException;
import com.drivehub.academico.features.clases.dto.ActualizarClaseRequest;
import com.drivehub.academico.features.clases.dto.CerrarClaseRequest;
import com.drivehub.academico.features.clases.dto.ClaseResponse;
import com.drivehub.academico.features.clases.enums.Asistencia;
import com.drivehub.academico.features.clases.enums.EstadoClase;
import com.drivehub.academico.features.clases.model.Clase;
import com.drivehub.academico.features.clases.repository.ClaseRepository;
import com.drivehub.academico.features.reservas.client.IdentidadClient;
import com.drivehub.academico.features.reservas.enums.EstadoReserva;
import com.drivehub.academico.features.reservas.model.Reserva;
import com.drivehub.academico.features.reservas.service.CierreDeReservaService;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * La autorización por rol NO se ejecuta acá (un servicio sin Spring no corre @PreAuthorize): se comprueba por
 * reflexión que esté declarada.
 */
@ExtendWith(MockitoExtension.class)
class ClaseServiceTest {

    private static final String BEARER = "Bearer token";
    private static final LocalDate FECHA = LocalDate.of(2026, 10, 26);
    private static final Instant INSTANTE = Instant.parse("2026-10-26T13:00:00Z");

    @Mock
    private ClaseRepository claseRepository;
    @Mock
    private CierreDeReservaService cierre;
    @Mock
    private IdentidadClient identidadClient;

    private final UUID instructor = UUID.randomUUID();
    private final UUID estudiante = UUID.randomUUID();
    private final UUID escuela = UUID.randomUUID();
    private ClaseService service;
    private ListAppender<ILoggingEvent> logs;

    @BeforeEach
    void setUp() {
        service = new ClaseService(claseRepository, cierre, identidadClient);
        logs = new ListAppender<>();
        logs.start();
        ((Logger) LoggerFactory.getLogger(ClaseService.class)).addAppender(logs);
    }

    @AfterEach
    void cerrarLogs() {
        ((Logger) LoggerFactory.getLogger(ClaseService.class)).detachAppender(logs);
    }

    private Reserva reserva() {
        Reserva reserva = new Reserva(estudiante, instructor, 7L, FECHA, "08:00", "09:00", 1, INSTANTE);
        reserva.inicializarCodigo();
        reserva.setId(50L);
        reserva.setEstado(EstadoReserva.CONFIRMADA);
        return reserva;
    }

    private Clase claseGuardada(Asistencia asistencia) {
        Clase clase = new Clase(50L, estudiante, instructor, FECHA, asistencia);
        clase.inicializarCodigo();
        return clase;
    }

    private void elNombreEs(String nombre) {
        when(identidadClient.nombresDeEstudiantes(List.of(estudiante), BEARER)).thenReturn(Map.of(estudiante, nombre));
    }

    private void elCierreEsPosible(Reserva reserva, Asistencia asistencia) {
        when(cierre.buscarCerrable(instructor, reserva.getCodigoReserva())).thenReturn(reserva);
        when(claseRepository.findByReservaIdAndDeletedAtIsNull(50L)).thenReturn(Optional.empty());
        when(claseRepository.save(any(Clase.class))).thenAnswer(i -> {
            Clase c = i.getArgument(0);
            c.inicializarCodigo();
            return c;
        });
        when(cierre.marcarCerrada(instructor, reserva.getCodigoReserva(), asistencia))
            .thenReturn(Optional.of(reserva));
    }

    private static void assertError(Runnable accion, String codigo) {
        assertThatThrownBy(accion::run).isInstanceOfSatisfying(BusinessException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(codigo));
    }

    private ClaseResponse cerrar(CerrarClaseRequest request) {
        return service.cerrar(instructor, escuela, BEARER, request);
    }

    // ================================================================ cerrar

    @Test
    void cerrar_presenteGuardaLaClaseConSusDatosYMarcaLaReserva() {
        Reserva reserva = reserva();
        elCierreEsPosible(reserva, Asistencia.PRESENTE);
        elNombreEs("Eva Estudiante");

        ClaseResponse respuesta = cerrar(new CerrarClaseRequest(reserva.getCodigoReserva(), Asistencia.PRESENTE,
            "  Giros en U  ", "Muy bien", 1000, 1042));

        ArgumentCaptor<Clase> guardada = ArgumentCaptor.forClass(Clase.class);
        verify(claseRepository).save(guardada.capture());
        Clase clase = guardada.getValue();
        assertThat(clase.getReservaId()).isEqualTo(50L);
        assertThat(clase.getEstudianteCodigo()).isEqualTo(estudiante);
        assertThat(clase.getInstructorCodigo()).isEqualTo(instructor);
        assertThat(clase.getFecha()).isEqualTo(FECHA);
        assertThat(clase.getAsistencia()).isEqualTo(Asistencia.PRESENTE);
        assertThat(clase.getTema()).isEqualTo("Giros en U");
        assertThat(clase.getObservaciones()).isEqualTo("Muy bien");
        assertThat(clase.getKmInicio()).isEqualTo(1000);
        assertThat(clase.getKmFin()).isEqualTo(1042);
        assertThat(clase.getEstado()).isEqualTo(EstadoClase.CERRADA);
        assertThat(clase.getVehiculoCodigo()).isNull();
        assertThat(clase.getInicioReal()).isNull();
        verify(cierre).marcarCerrada(instructor, reserva.getCodigoReserva(), Asistencia.PRESENTE);
        assertThat(respuesta.estudianteNombre()).isEqualTo("Eva Estudiante");
        assertThat(respuesta.estado()).isEqualTo(EstadoClase.CERRADA);
    }

    @Test
    void cerrar_tardeTambienMarcaLaReserva() {
        Reserva reserva = reserva();
        elCierreEsPosible(reserva, Asistencia.TARDE);
        elNombreEs("Eva Estudiante");

        ClaseResponse respuesta = cerrar(new CerrarClaseRequest(reserva.getCodigoReserva(), Asistencia.TARDE, null,
            "Llegó 15 minutos tarde", null, null));

        assertThat(respuesta.asistencia()).isEqualTo(Asistencia.TARDE);
        verify(cierre).marcarCerrada(instructor, reserva.getCodigoReserva(), Asistencia.TARDE);
    }

    @Test
    void cerrar_ausenteGuardaSoloLasObservaciones() {
        Reserva reserva = reserva();
        elCierreEsPosible(reserva, Asistencia.AUSENTE);
        elNombreEs("Eva Estudiante");

        ClaseResponse respuesta = cerrar(new CerrarClaseRequest(reserva.getCodigoReserva(), Asistencia.AUSENTE, null,
            "No avisó", null, null));

        assertThat(respuesta.asistencia()).isEqualTo(Asistencia.AUSENTE);
        assertThat(respuesta.observaciones()).isEqualTo("No avisó");
        assertThat(respuesta.tema()).isNull();
        assertThat(respuesta.kmInicio()).isNull();
        verify(cierre).marcarCerrada(instructor, reserva.getCodigoReserva(), Asistencia.AUSENTE);
    }

    @Test
    void cerrar_soloConLaAsistenciaCierraIgualYLosBlancosSeGuardanNull() {
        Reserva reserva = reserva();
        elCierreEsPosible(reserva, Asistencia.PRESENTE);
        elNombreEs("Eva Estudiante");

        ClaseResponse respuesta = cerrar(new CerrarClaseRequest(reserva.getCodigoReserva(), Asistencia.PRESENTE, "   ",
            "", null, null));

        assertThat(respuesta.tema()).isNull();
        assertThat(respuesta.observaciones()).isNull();
        assertThat(respuesta.kmInicio()).isNull();
        assertThat(respuesta.kmFin()).isNull();
    }

    @Test
    void cerrar_unCierreInterrumpido_reutilizaLaClaseYTerminaElCierreSinDuplicarla() {
        Reserva reserva = reserva();
        Clase previa = claseGuardada(Asistencia.PRESENTE);
        previa.setTema("Tema viejo");
        when(cierre.buscarCerrable(instructor, reserva.getCodigoReserva())).thenReturn(reserva);
        when(claseRepository.findByReservaIdAndDeletedAtIsNull(50L)).thenReturn(Optional.of(previa));
        when(claseRepository.save(previa)).thenReturn(previa);
        when(cierre.marcarCerrada(instructor, reserva.getCodigoReserva(), Asistencia.TARDE))
            .thenReturn(Optional.of(reserva));
        elNombreEs("Eva Estudiante");

        ClaseResponse respuesta = cerrar(new CerrarClaseRequest(reserva.getCodigoReserva(), Asistencia.TARDE,
            "Tema nuevo", null, null, null));

        assertThat(respuesta.codigo()).isEqualTo(previa.getCodigoClase());
        assertThat(respuesta.tema()).isEqualTo("Tema nuevo");
        assertThat(respuesta.asistencia()).isEqualTo(Asistencia.TARDE);
        verify(claseRepository).save(previa);
        verify(claseRepository, never()).delete(any(Clase.class));
        assertThat(logs.list).anySatisfy(e -> assertThat(e.getFormattedMessage()).contains("retomado"));
    }

    @Test
    void cerrar_siLaReservaCambioMientrasTanto_borraLaClaseQueCreoYLanzaNoCerrable() {
        Reserva reserva = reserva();
        elCierreEsPosible(reserva, Asistencia.PRESENTE);
        when(cierre.marcarCerrada(instructor, reserva.getCodigoReserva(), Asistencia.PRESENTE))
            .thenReturn(Optional.empty());

        assertError(() -> cerrar(new CerrarClaseRequest(reserva.getCodigoReserva(), Asistencia.PRESENTE, "Tema", null,
            null, null)), AcademicoErrorCodes.CLASE_NO_CERRABLE);

        ArgumentCaptor<Clase> borrada = ArgumentCaptor.forClass(Clase.class);
        verify(claseRepository).delete(borrada.capture());
        assertThat(borrada.getValue().getReservaId()).isEqualTo(50L);
        assertThat(logs.list).anySatisfy(e -> assertThat(e.getLevel().toString()).isEqualTo("WARN"));
    }

    @Test
    void cerrar_siLaReservaCambioPeroLaClaseYaExistia_noLaBorra() {
        Reserva reserva = reserva();
        Clase previa = claseGuardada(Asistencia.PRESENTE);
        when(cierre.buscarCerrable(instructor, reserva.getCodigoReserva())).thenReturn(reserva);
        when(claseRepository.findByReservaIdAndDeletedAtIsNull(50L)).thenReturn(Optional.of(previa));
        when(claseRepository.save(previa)).thenReturn(previa);
        when(cierre.marcarCerrada(instructor, reserva.getCodigoReserva(), Asistencia.PRESENTE))
            .thenReturn(Optional.empty());

        assertError(() -> cerrar(new CerrarClaseRequest(reserva.getCodigoReserva(), Asistencia.PRESENTE, null, null,
            null, null)), AcademicoErrorCodes.CLASE_NO_CERRABLE);

        verify(claseRepository, never()).delete(any(Clase.class));
    }

    @Test
    void cerrar_otraPeticionCreoLaClaseALaVez_lanzaNoCerrableSinMarcarLaReserva() {
        Reserva reserva = reserva();
        when(cierre.buscarCerrable(instructor, reserva.getCodigoReserva())).thenReturn(reserva);
        when(claseRepository.findByReservaIdAndDeletedAtIsNull(50L)).thenReturn(Optional.empty());
        when(claseRepository.save(any(Clase.class))).thenThrow(new DuplicateKeyException("reservaId"));

        assertError(() -> cerrar(new CerrarClaseRequest(reserva.getCodigoReserva(), Asistencia.PRESENTE, null, null,
            null, null)), AcademicoErrorCodes.CLASE_NO_CERRABLE);

        verify(cierre, never()).marcarCerrada(any(), any(), any());
    }

    @Test
    void cerrar_siLaReservaNoSePuedeCerrar_salenSusErroresYNoSeGuardaNada() {
        UUID codigo = UUID.randomUUID();
        when(cierre.buscarCerrable(instructor, codigo)).thenThrow(
            new NotFoundException(AcademicoErrorCodes.CLASE_INSTRUCTOR_NO_ENCONTRADA, "No encontramos esa clase"));

        assertError(() -> cerrar(new CerrarClaseRequest(codigo, Asistencia.PRESENTE, null, null, null, null)),
            AcademicoErrorCodes.CLASE_INSTRUCTOR_NO_ENCONTRADA);

        doThrow(new ConflictException(AcademicoErrorCodes.CLASE_NO_CERRABLE, "Esa clase no se puede cerrar"))
            .when(cierre).buscarCerrable(instructor, codigo);

        assertError(() -> cerrar(new CerrarClaseRequest(codigo, Asistencia.PRESENTE, null, null, null, null)),
            AcademicoErrorCodes.CLASE_NO_CERRABLE);

        verifyNoInteractions(claseRepository);
    }

    @Test
    void cerrar_conDatosInvalidos_lanzaDatosInvalidosSinTocarLaBaseNiBuscarLaReserva() {
        UUID codigo = UUID.randomUUID();

        assertError(() -> cerrar(new CerrarClaseRequest(codigo, Asistencia.AUSENTE, "Tema", null, null, null)),
            AcademicoErrorCodes.DATOS_DE_CLASE_INVALIDOS);
        assertError(() -> cerrar(new CerrarClaseRequest(codigo, Asistencia.PRESENTE, null, null, 200, 100)),
            AcademicoErrorCodes.DATOS_DE_CLASE_INVALIDOS);

        verifyNoInteractions(cierre, claseRepository);
    }

    @Test
    void cerrar_conIdentidadCaida_respondeConEstudianteSinError() {
        Reserva reserva = reserva();
        elCierreEsPosible(reserva, Asistencia.PRESENTE);
        when(identidadClient.nombresDeEstudiantes(List.of(estudiante), BEARER))
            .thenThrow(new ServicioNoDisponibleException(AcademicoErrorCodes.SERVICIO_NO_DISPONIBLE, "caído"));

        ClaseResponse respuesta = cerrar(new CerrarClaseRequest(reserva.getCodigoReserva(), Asistencia.PRESENTE, null,
            null, null, null));

        assertThat(respuesta.estudianteNombre()).isEqualTo("Estudiante");
    }

    @Test
    void cerrar_siIdentidadNoDevuelveAlEstudiante_respondeConEstudiante() {
        Reserva reserva = reserva();
        elCierreEsPosible(reserva, Asistencia.PRESENTE);
        when(identidadClient.nombresDeEstudiantes(List.of(estudiante), BEARER)).thenReturn(Map.of());

        assertThat(cerrar(new CerrarClaseRequest(reserva.getCodigoReserva(), Asistencia.PRESENTE, null, null, null,
            null)).estudianteNombre()).isEqualTo("Estudiante");
    }

    @Test
    void cerrar_sinEscuelaEnElToken_lanzaProhibido() {
        assertError(() -> service.cerrar(instructor, null, BEARER,
            new CerrarClaseRequest(UUID.randomUUID(), Asistencia.PRESENTE, null, null, null, null)),
            AcademicoErrorCodes.TOKEN_SIN_ESCUELA);

        verifyNoInteractions(cierre, claseRepository);
    }

    @Test
    void cerrar_loguearElCodigoYLaAsistenciaPeroNuncaElTextoDeLasObservaciones() {
        Reserva reserva = reserva();
        elCierreEsPosible(reserva, Asistencia.PRESENTE);
        elNombreEs("Eva Estudiante");

        cerrar(new CerrarClaseRequest(reserva.getCodigoReserva(), Asistencia.PRESENTE, "TemaSecretoXYZ",
            "ObservacionSecretaXYZ", null, null));

        assertThat(logs.list).isNotEmpty().allSatisfy(e -> {
            assertThat(e.getFormattedMessage()).doesNotContain("SecretoXYZ").doesNotContain("SecretaXYZ");
            assertThat(java.util.Arrays.toString(e.getArgumentArray())).doesNotContain("SecretoXYZ").doesNotContain("SecretaXYZ");
        });
        assertThat(logs.list).anySatisfy(e -> assertThat(e.getFormattedMessage()).contains("Clase cerrada"));
    }

    // ================================================================ obtener

    @Test
    void obtener_devuelveLaClasePropiaConElNombre() {
        Clase clase = claseGuardada(Asistencia.PRESENTE);
        clase.setTema("Tema");
        when(claseRepository.findByCodigoClaseAndInstructorCodigoAndDeletedAtIsNull(clase.getCodigoClase(),
            instructor)).thenReturn(Optional.of(clase));
        elNombreEs("Eva Estudiante");

        ClaseResponse respuesta = service.obtener(instructor, escuela, BEARER, clase.getCodigoClase());

        assertThat(respuesta.codigo()).isEqualTo(clase.getCodigoClase());
        assertThat(respuesta.tema()).isEqualTo("Tema");
        assertThat(respuesta.estudianteNombre()).isEqualTo("Eva Estudiante");
        assertThat(logs.list).isEmpty();
    }

    @Test
    void obtener_inexistenteBorradaODeOtroInstructor_lanzaNoEncontrada() {
        UUID codigo = UUID.randomUUID();
        when(claseRepository.findByCodigoClaseAndInstructorCodigoAndDeletedAtIsNull(codigo, instructor))
            .thenReturn(Optional.empty());

        assertError(() -> service.obtener(instructor, escuela, BEARER, codigo),
            AcademicoErrorCodes.CLASE_INSTRUCTOR_NO_ENCONTRADA);
    }

    @Test
    void obtener_conIdentidadCaida_respondeConEstudianteSinError() {
        Clase clase = claseGuardada(Asistencia.PRESENTE);
        when(claseRepository.findByCodigoClaseAndInstructorCodigoAndDeletedAtIsNull(clase.getCodigoClase(),
            instructor)).thenReturn(Optional.of(clase));
        when(identidadClient.nombresDeEstudiantes(List.of(estudiante), BEARER))
            .thenThrow(new ServicioNoDisponibleException(AcademicoErrorCodes.SERVICIO_NO_DISPONIBLE, "caído"));

        assertThat(service.obtener(instructor, escuela, BEARER, clase.getCodigoClase()).estudianteNombre())
            .isEqualTo("Estudiante");
    }

    // ================================================================ actualizar

    private Clase laClaseExiste(Asistencia asistencia) {
        Clase clase = claseGuardada(asistencia);
        when(claseRepository.findByCodigoClaseAndInstructorCodigoAndDeletedAtIsNull(clase.getCodigoClase(),
            instructor)).thenReturn(Optional.of(clase));
        return clase;
    }

    private ClaseResponse actualizar(Clase clase, ActualizarClaseRequest request) {
        return service.actualizar(instructor, escuela, BEARER, clase.getCodigoClase(), request);
    }

    @Test
    void actualizar_cambiaSoloElTemaYDejaLoDemas() {
        Clase clase = laClaseExiste(Asistencia.PRESENTE);
        clase.setTema("Viejo");
        clase.setObservaciones("Obs");
        clase.setKmInicio(100);
        clase.setKmFin(150);
        when(claseRepository.save(clase)).thenReturn(clase);
        elNombreEs("Eva Estudiante");

        ClaseResponse respuesta = actualizar(clase, new ActualizarClaseRequest("Nuevo", null, null, null));

        assertThat(respuesta.tema()).isEqualTo("Nuevo");
        assertThat(respuesta.observaciones()).isEqualTo("Obs");
        assertThat(respuesta.kmInicio()).isEqualTo(100);
        assertThat(respuesta.kmFin()).isEqualTo(150);
        assertThat(respuesta.asistencia()).isEqualTo(Asistencia.PRESENTE);
    }

    @Test
    void actualizar_cambiaSoloUnKilometrajeYLoValidaContraElOtroGuardado() {
        Clase clase = laClaseExiste(Asistencia.PRESENTE);
        clase.setKmInicio(100);
        clase.setKmFin(150);

        assertError(() -> actualizar(clase, new ActualizarClaseRequest(null, null, null, 90)),
            AcademicoErrorCodes.DATOS_DE_CLASE_INVALIDOS);

        verify(claseRepository, never()).save(any(Clase.class));
        assertThat(clase.getKmFin()).isEqualTo(150);
    }

    @Test
    void actualizar_unBlancoBorraElTextoYNullNoLoBorra() {
        Clase clase = laClaseExiste(Asistencia.PRESENTE);
        clase.setTema("Tema");
        clase.setObservaciones("Obs");
        when(claseRepository.save(clase)).thenReturn(clase);
        elNombreEs("Eva Estudiante");

        ClaseResponse respuesta = actualizar(clase, new ActualizarClaseRequest("   ", null, null, null));

        assertThat(respuesta.tema()).isNull();
        assertThat(respuesta.observaciones()).isEqualTo("Obs");
    }

    @Test
    void actualizar_unaClaseAusenteSoloAdmiteObservaciones() {
        Clase clase = laClaseExiste(Asistencia.AUSENTE);
        when(claseRepository.save(clase)).thenReturn(clase);
        elNombreEs("Eva Estudiante");

        assertError(() -> actualizar(clase, new ActualizarClaseRequest("Tema", null, null, null)),
            AcademicoErrorCodes.DATOS_DE_CLASE_INVALIDOS);
        assertError(() -> actualizar(clase, new ActualizarClaseRequest(null, null, 10, null)),
            AcademicoErrorCodes.DATOS_DE_CLASE_INVALIDOS);

        ClaseResponse respuesta = actualizar(clase, new ActualizarClaseRequest(null, "Avisó tarde", null, null));
        assertThat(respuesta.observaciones()).isEqualTo("Avisó tarde");
    }

    @Test
    void actualizar_conUnCuerpoVacio_noGuardaNiLoguea() {
        Clase clase = laClaseExiste(Asistencia.PRESENTE);
        clase.setTema("Tema");
        elNombreEs("Eva Estudiante");

        ClaseResponse respuesta = actualizar(clase, new ActualizarClaseRequest(null, null, null, null));

        assertThat(respuesta.tema()).isEqualTo("Tema");
        verify(claseRepository, never()).save(any(Clase.class));
        assertThat(logs.list).isEmpty();
    }

    @Test
    void actualizar_deOtroInstructorOInexistente_lanzaNoEncontrada() {
        UUID codigo = UUID.randomUUID();
        when(claseRepository.findByCodigoClaseAndInstructorCodigoAndDeletedAtIsNull(codigo, instructor))
            .thenReturn(Optional.empty());

        assertError(() -> service.actualizar(instructor, escuela, BEARER, codigo,
            new ActualizarClaseRequest("x", null, null, null)), AcademicoErrorCodes.CLASE_INSTRUCTOR_NO_ENCONTRADA);

        verify(claseRepository, never()).save(any(Clase.class));
    }

    @Test
    void actualizar_loguearLosCamposCambiadosPeroNoSusValores() {
        Clase clase = laClaseExiste(Asistencia.PRESENTE);
        when(claseRepository.save(clase)).thenReturn(clase);
        elNombreEs("Eva Estudiante");

        actualizar(clase, new ActualizarClaseRequest("TemaSecretoXYZ", "ObservacionSecretaXYZ", null, 77777));

        assertThat(logs.list).singleElement().satisfies(e -> {
            String todo = e.getFormattedMessage() + java.util.Arrays.toString(e.getArgumentArray());
            assertThat(todo).contains("tema").contains("observaciones").contains("kmFin");
            assertThat(todo).doesNotContain("SecretoXYZ").doesNotContain("SecretaXYZ").doesNotContain("77777");
            assertThat(e.getFormattedMessage()).contains("Clase corregida");
        });
    }

    @Test
    void actualizar_sinEscuelaEnElToken_lanzaProhibido() {
        assertError(() -> service.actualizar(instructor, null, BEARER, UUID.randomUUID(),
            new ActualizarClaseRequest(null, null, null, null)), AcademicoErrorCodes.TOKEN_SIN_ESCUELA);
        assertError(() -> service.obtener(instructor, null, BEARER, UUID.randomUUID()),
            AcademicoErrorCodes.TOKEN_SIN_ESCUELA);

        verifyNoInteractions(claseRepository);
    }

    // ================================================================ codigosPorReserva

    @Test
    void codigosPorReserva_unaSolaConsultaYSoloLasQueTienenClase() {
        Clase a = claseGuardada(Asistencia.PRESENTE);
        when(claseRepository.findByReservaIdInAndDeletedAtIsNull(List.of(50L, 51L))).thenReturn(List.of(a));

        Map<Long, UUID> codigos = service.codigosPorReserva(List.of(50L, 51L));

        assertThat(codigos).containsOnlyKeys(50L).containsEntry(50L, a.getCodigoClase());
        verify(claseRepository).findByReservaIdInAndDeletedAtIsNull(List.of(50L, 51L));
    }

    @Test
    void codigosPorReserva_sinReservasNoConsulta() {
        assertThat(service.codigosPorReserva(List.of())).isEmpty();

        verifyNoInteractions(claseRepository);
    }

    // ================================================================ seguridad

    @Test
    void losTresMetodosExigenElRolInstructor() throws NoSuchMethodException {
        Method cerrar = ClaseService.class.getMethod("cerrar", UUID.class, UUID.class, String.class,
            CerrarClaseRequest.class);
        Method obtener = ClaseService.class.getMethod("obtener", UUID.class, UUID.class, String.class, UUID.class);
        Method actualizar = ClaseService.class.getMethod("actualizar", UUID.class, UUID.class, String.class,
            UUID.class, ActualizarClaseRequest.class);

        for (Method metodo : List.of(cerrar, obtener, actualizar)) {
            assertThat(metodo.getAnnotation(PreAuthorize.class)).isNotNull();
            assertThat(metodo.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('INSTRUCTOR')");
        }
    }

    @Test
    void cerrar_noLlamaAPagosNiTocaLaFranja() {
        // * el servicio solo conoce el repositorio de clases, el cierre de reservas e Identidad
        assertThat(ClaseService.class.getDeclaredFields())
            .extracting(f -> f.getType().getSimpleName())
            .filteredOn(n -> n.contains("Pagos") || n.contains("Franja"))
            .isEmpty();
    }
}
