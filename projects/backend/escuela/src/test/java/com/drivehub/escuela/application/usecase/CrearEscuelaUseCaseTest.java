package com.drivehub.escuela.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import com.drivehub.escuela.application.dto.CrearEscuelaInput;
import com.drivehub.escuela.application.dto.EscuelaCreadaOutput;
import com.drivehub.escuela.common.exception.BusinessException;
import com.drivehub.escuela.common.exception.ConflictException;
import com.drivehub.escuela.common.exception.EscuelaErrorCodes;
import com.drivehub.escuela.domain.entity.Escuela;
import com.drivehub.escuela.domain.entity.EstadoEscuela;
import com.drivehub.escuela.domain.gateway.AdministradorCreado;
import com.drivehub.escuela.domain.gateway.AdministradorEscuelaGateway;
import com.drivehub.escuela.domain.gateway.DatosAdministrador;
import com.drivehub.escuela.domain.gateway.IdentidadNoDisponibleException;
import com.drivehub.escuela.domain.repository.EscuelaRepository;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

@ExtendWith(MockitoExtension.class)
class CrearEscuelaUseCaseTest {

    private static final String CLAVE = "claveSecreta123";

    @Mock
    private EscuelaRepository escuelaRepository;
    @Mock
    private AdministradorEscuelaGateway gateway;

    private PlatformTransactionManager transacciones;
    private CrearEscuelaUseCase useCase;
    private ListAppender<ILoggingEvent> logs;

    @BeforeEach
    void preparar() {
        transacciones = mock(PlatformTransactionManager.class);
        lenient().when(transacciones.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        useCase = new CrearEscuelaUseCase(escuelaRepository, gateway, new TransactionTemplate(transacciones));
        logs = new ListAppender<>();
        logs.start();
        ((Logger) LoggerFactory.getLogger(CrearEscuelaUseCase.class)).addAppender(logs);
    }

    @AfterEach
    void quitarAppender() {
        ((Logger) LoggerFactory.getLogger(CrearEscuelaUseCase.class)).detachAppender(logs);
    }

    private static DatosAdministrador admin() {
        return new DatosAdministrador("Marta", "Gómez", LocalDate.of(1985, 5, 5), "marta@gmail.com", CLAVE, null);
    }

    private static CrearEscuelaInput input(String nombre, String slug) {
        return new CrearEscuelaInput(nombre, slug, admin());
    }

    private void repositorioGuardaTalCual() {
        when(escuelaRepository.guardar(any(Escuela.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static AdministradorCreado creado(UUID codigo) {
        return new AdministradorCreado(codigo, "marta@gmail.com", "Marta", "Gómez");
    }

    @Test
    void ejecutar_conDatosValidos_guardaLaEscuelaCreaAlAdministradorYSoloEntoncesConfirma() {
        repositorioGuardaTalCual();
        when(gateway.crearAdministrador(any(), any())).thenAnswer(inv -> creado(UUID.randomUUID()));

        EscuelaCreadaOutput resultado = useCase.ejecutar(input("  Autos del Sur ", " Autos-Sur "));

        ArgumentCaptor<Escuela> guardada = ArgumentCaptor.forClass(Escuela.class);
        InOrder orden = inOrder(escuelaRepository, gateway, transacciones);
        orden.verify(escuelaRepository).guardar(guardada.capture());
        orden.verify(gateway).crearAdministrador(eq(guardada.getValue().getCodigo()), any(DatosAdministrador.class));
        orden.verify(transacciones).commit(any());
        verify(transacciones, never()).rollback(any());
        verify(gateway, never()).revertirAdministrador(any());

        assertThat(guardada.getValue().getSlug()).isEqualTo("autos-sur");
        assertThat(guardada.getValue().getNombre()).isEqualTo("Autos del Sur");
        assertThat(resultado.codigo()).isEqualTo(guardada.getValue().getCodigo());
        assertThat(resultado.slug()).isEqualTo("autos-sur");
        assertThat(resultado.estado()).isEqualTo(EstadoEscuela.ACTIVA);
        assertThat(resultado.administrador().email()).isEqualTo("marta@gmail.com");
    }

    @Test
    void ejecutar_conSlugEnUso_lanzaErrEsc03SinTocarIdentidadNiLaTransaccion() {
        when(escuelaRepository.existePorSlug("autos-sur")).thenReturn(true);

        assertThatThrownBy(() -> useCase.ejecutar(input("Autos del Sur", "autos-sur")))
            .isInstanceOfSatisfying(BusinessException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(EscuelaErrorCodes.SLUG_EN_USO));
        verifyNoInteractions(gateway);
        verifyNoInteractions(transacciones);
    }

    @Test
    void ejecutar_conNombreOSlugInvalidos_lanzaErrSys01SinConsultarNada() {
        String slugLargo = "a".repeat(61);
        for (CrearEscuelaInput malo : new CrearEscuelaInput[] {
                input("ab", "autos-sur"), input(" ", "autos-sur"), input("a".repeat(121), "autos-sur"),
                input("Autos", "ab"), input("Autos", "con espacios"), input("Autos", "-borde"),
                input("Autos", "borde-"), input("Autos", "doble--guion"), input("Autos", "ñandú"),
                input("Autos", slugLargo), input("Autos", null), input(null, "autos-sur")}) {
            assertThatThrownBy(() -> useCase.ejecutar(malo))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                    assertThat(ex.getErrorCode()).isEqualTo("ERR_SYS_01"));
        }
        verifyNoInteractions(escuelaRepository);
        verifyNoInteractions(gateway);
    }

    @Test
    void ejecutar_siIdentidadRechazaPorNegocio_deshaceLaTransaccionSinCompensarYRelanzaElErrorDeIdentidad() {
        repositorioGuardaTalCual();
        when(gateway.crearAdministrador(any(), any()))
            .thenThrow(new ConflictException("ERR_IDE_01", "Ya existe una cuenta con ese correo"));

        assertThatThrownBy(() -> useCase.ejecutar(input("Autos del Sur", "autos-sur")))
            .isInstanceOfSatisfying(ConflictException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo("ERR_IDE_01"));

        verify(transacciones).rollback(any());
        verify(transacciones, never()).commit(any());
        verify(gateway, never()).revertirAdministrador(any());
    }

    @Test
    void ejecutar_siElSlugChocaAlGuardar_lanzaErrEsc03SinLlamarAIdentidad() {
        when(escuelaRepository.guardar(any(Escuela.class)))
            .thenThrow(new ConflictException(EscuelaErrorCodes.SLUG_EN_USO, "Ya existe una escuela con ese slug"));

        assertThatThrownBy(() -> useCase.ejecutar(input("Autos del Sur", "autos-sur")))
            .isInstanceOfSatisfying(BusinessException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(EscuelaErrorCodes.SLUG_EN_USO));

        verify(gateway, never()).crearAdministrador(any(), any());
        verify(gateway, never()).revertirAdministrador(any());
        verify(transacciones).rollback(any());
    }

    @Test
    void ejecutar_siIdentidadNoResponde_deshaceLaTransaccionRevierteAlAdministradorYRelanzaElError() {
        repositorioGuardaTalCual();
        IdentidadNoDisponibleException caida = new IdentidadNoDisponibleException("sin respuesta");
        when(gateway.crearAdministrador(any(), any())).thenThrow(caida);

        assertThatThrownBy(() -> useCase.ejecutar(input("Autos del Sur", "autos-sur"))).isSameAs(caida);

        ArgumentCaptor<Escuela> guardada = ArgumentCaptor.forClass(Escuela.class);
        verify(escuelaRepository).guardar(guardada.capture());
        verify(transacciones).rollback(any());
        verify(gateway).revertirAdministrador(guardada.getValue().getCodigo());
    }

    @Test
    void ejecutar_siFallaLaConfirmacionDespuesDeCrearAlAdministrador_loRevierte() {
        repositorioGuardaTalCual();
        when(gateway.crearAdministrador(any(), any())).thenAnswer(inv -> creado(UUID.randomUUID()));
        TransactionSystemException falla = new TransactionSystemException("no se pudo confirmar");
        doThrow(falla).when(transacciones).commit(any());

        assertThatThrownBy(() -> useCase.ejecutar(input("Autos del Sur", "autos-sur"))).isSameAs(falla);

        ArgumentCaptor<Escuela> guardada = ArgumentCaptor.forClass(Escuela.class);
        verify(escuelaRepository).guardar(guardada.capture());
        verify(gateway).revertirAdministrador(guardada.getValue().getCodigo());
    }

    @Test
    void ejecutar_siLaCompensacionTambienFalla_relanzaElErrorOriginalYRegistraUnError() {
        repositorioGuardaTalCual();
        IdentidadNoDisponibleException original = new IdentidadNoDisponibleException("sin respuesta");
        when(gateway.crearAdministrador(any(), any())).thenThrow(original);
        doThrow(new IdentidadNoDisponibleException("tampoco responde")).when(gateway).revertirAdministrador(any());

        assertThatThrownBy(() -> useCase.ejecutar(input("Autos del Sur", "autos-sur"))).isSameAs(original);

        assertThat(logs.list).anySatisfy(evento -> {
            assertThat(evento.getLevel()).isEqualTo(Level.ERROR);
            assertThat(evento.getFormattedMessage()).contains("Compensación fallida");
        });
    }

    @Test
    void ejecutar_nuncaEscribeLaContrasenaEnLosRegistros() {
        repositorioGuardaTalCual();
        when(gateway.crearAdministrador(any(), any())).thenThrow(new IdentidadNoDisponibleException("sin respuesta"));
        doThrow(new IdentidadNoDisponibleException("tampoco responde")).when(gateway).revertirAdministrador(any());

        assertThatThrownBy(() -> useCase.ejecutar(input("Autos del Sur", "autos-sur")))
            .isInstanceOf(IdentidadNoDisponibleException.class);

        assertThat(logs.list).isNotEmpty();
        for (ILoggingEvent evento : logs.list) {
            String completo = evento.getFormattedMessage() + Arrays.toString(evento.getArgumentArray());
            assertThat(completo).doesNotContain(CLAVE);
        }
    }
}
