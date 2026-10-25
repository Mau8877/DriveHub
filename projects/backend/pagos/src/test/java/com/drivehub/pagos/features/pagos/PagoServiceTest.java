package com.drivehub.pagos.features.pagos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import com.drivehub.pagos.PagosPropertiesPrueba;
import com.drivehub.pagos.common.exception.BusinessException;
import com.drivehub.pagos.common.exception.ConflictException;
import com.drivehub.pagos.common.exception.NotFoundException;
import com.drivehub.pagos.common.exception.ServicioNoDisponibleException;
import com.drivehub.pagos.common.exception.ValidationException;
import com.drivehub.pagos.config.BearerActual;
import com.drivehub.pagos.config.PagosProperties;
import com.drivehub.pagos.features.pagos.client.IdentidadClient;
import com.drivehub.pagos.features.pagos.dto.EstadoPagoResponse;
import com.drivehub.pagos.features.pagos.enums.ConceptoPago;
import com.drivehub.pagos.features.pagos.enums.EstadoPago;
import com.drivehub.pagos.features.pagos.enums.MetodoPago;
import com.drivehub.pagos.features.pagos.model.Pago;
import com.drivehub.pagos.features.pagos.model.TransaccionPasarela;
import com.drivehub.pagos.features.pagos.pasarela.PasarelaPago;
import com.drivehub.pagos.features.pagos.pasarela.SesionCobro;
import com.drivehub.pagos.features.pagos.pasarela.SolicitudCobro;
import com.drivehub.pagos.features.pagos.repository.PagoRepository;
import com.drivehub.pagos.features.pagos.repository.TransaccionPasarelaRepository;
import com.drivehub.pagos.features.pagos.service.PagoService;
import com.drivehub.pagos.features.planes.model.PlanPago;
import com.drivehub.pagos.features.planes.repository.PlanPagoRepository;
import com.drivehub.pagos.features.planes.service.ConversionMoneda;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

@ExtendWith(MockitoExtension.class)
class PagoServiceTest {

    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();
    private static final UUID PLAN = UUID.randomUUID();
    private static final String CLAVE = "clave-de-la-compra";
    private static final Instant AHORA = Instant.parse("2026-10-25T12:00:00Z");
    private static final String URL_PAGO = "https://checkout.stripe.com/c/pay/cs_test_1";

    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private TransaccionPasarelaRepository transaccionRepository;
    @Mock
    private PlanPagoRepository planRepository;
    @Mock
    private PasarelaPago pasarela;
    @Mock
    private IdentidadClient identidad;
    @Mock
    private BearerActual bearer;

    private PagoService servicio;
    private ListAppender<ILoggingEvent> logs;
    private final AtomicLong ids = new AtomicLong(100);
    private Pago ultimoPersistido;

    @BeforeEach
    void preparar() {
        PagosProperties propiedades = PagosPropertiesPrueba.conPasarela(PagosPropertiesPrueba.pasarela("", ""));
        servicio = new PagoService(pagoRepository, transaccionRepository, planRepository,
            new ConversionMoneda(propiedades), propiedades, pasarela, identidad, bearer,
            Clock.fixed(AHORA, ZoneOffset.UTC), mock(PlatformTransactionManager.class));
        logs = new ListAppender<>();
        logs.start();
        ((Logger) LoggerFactory.getLogger(PagoService.class)).addAppender(logs);
    }

    @AfterEach
    void limpiarLogs() {
        ((Logger) LoggerFactory.getLogger(PagoService.class)).detachAppender(logs);
    }

    // ------------------------------------------------------------------ utilidades

    private static PlanPago plan(boolean activo) {
        PlanPago plan = new PlanPago(ESCUELA, "Plan 10 horas", (short) 10, new BigDecimal("450.00"));
        plan.setActivo(activo);
        ReflectionTestUtils.setField(plan, "codigo", PLAN);
        return plan;
    }

    /** Lo que haría la base al guardar: asigna el id interno y el codigo público. */
    private Pago persistido(Pago pago) {
        ReflectionTestUtils.setField(pago, "id", ids.incrementAndGet());
        ReflectionTestUtils.setField(pago, "codigo", UUID.randomUUID());
        ultimoPersistido = pago;
        return pago;
    }

    private Pago pagoExistente(UUID estudiante, EstadoPago estado, Instant expira) {
        PlanPago plan = plan(true);
        Pago pago = new Pago(ESCUELA, estudiante, plan, new BigDecimal("38.23"), new BigDecimal(PagosPropertiesPrueba.TIPO_CAMBIO),
            "Ana Ruiz", "Escuela Example", CLAVE, expira);
        pago.setEstado(estado);
        return persistido(pago);
    }

    private static TransaccionPasarela sesionGuardada(Pago pago) {
        TransaccionPasarela transaccion = new TransaccionPasarela(pago, "STRIPE", "CREADA");
        transaccion.setReferenciaExterna("cs_test_1");
        transaccion.setUrlPago(URL_PAGO);
        return transaccion;
    }

    private void compraNueva() {
        when(pagoRepository.findByIdempotencyKey(CLAVE)).thenReturn(Optional.empty());
        when(planRepository.findByCodigoAndEscuelaCodigo(PLAN, ESCUELA)).thenReturn(Optional.of(plan(true)));
        when(bearer.cabecera()).thenReturn("Bearer token");
        when(identidad.perfilPropio("Bearer token"))
            .thenReturn(new IdentidadClient.PerfilEstudiante("Ana Ruiz", "Escuela Example"));
        when(pagoRepository.saveAndFlush(any(Pago.class))).thenAnswer(i -> persistido(i.getArgument(0)));
    }

    private static BusinessException error(Runnable accion) {
        try {
            accion.run();
        } catch (BusinessException e) {
            return e;
        }
        throw new AssertionError("Se esperaba una BusinessException");
    }

    // ------------------------------------------------------------------ iniciar: compra nueva

    @Test
    void iniciar_guardaElPagoPendienteConTodoCopiado_yPideLaSesionDeCobro() {
        compraNueva();
        when(pasarela.crearSesionCobro(any())).thenReturn(new SesionCobro("cs_test_1", URL_PAGO,
            AHORA.plusSeconds(1860)));

        PagoService.ResultadoIniciar resultado = servicio.iniciar(ESCUELA, ESTUDIANTE, PLAN, CLAVE);

        ArgumentCaptor<Pago> guardado = ArgumentCaptor.forClass(Pago.class);
        verify(pagoRepository).saveAndFlush(guardado.capture());
        Pago pago = guardado.getValue();
        assertThat(pago.getEscuelaCodigo()).isEqualTo(ESCUELA);
        assertThat(pago.getEstudianteCodigo()).isEqualTo(ESTUDIANTE);
        assertThat(pago.getConcepto()).isEqualTo(ConceptoPago.HORAS);
        assertThat(pago.getMetodo()).isEqualTo(MetodoPago.TARJETA);
        assertThat(pago.getEstado()).isEqualTo(EstadoPago.PENDIENTE);
        assertThat(pago.getPlanNombre()).isEqualTo("Plan 10 horas");
        assertThat(pago.getHoras()).isEqualTo((short) 10);
        assertThat(pago.getMonto()).isEqualByComparingTo("38.23");
        assertThat(pago.getMoneda()).isEqualTo("USD");
        assertThat(pago.getMontoBs()).isEqualByComparingTo("450.00");
        assertThat(pago.getTipoCambio()).isEqualByComparingTo(PagosPropertiesPrueba.TIPO_CAMBIO);
        assertThat(pago.getEstudianteNombre()).isEqualTo("Ana Ruiz");
        assertThat(pago.getEscuelaNombre()).isEqualTo("Escuela Example");
        assertThat(pago.getIdempotencyKey()).isEqualTo(CLAVE);
        // * 30 minutos de vigencia y un minuto de holgura para la pasarela
        assertThat(pago.getExpiraEn()).isEqualTo(AHORA.plusSeconds(31 * 60));

        assertThat(resultado.creado()).isTrue();
        assertThat(resultado.pago().estado()).isEqualTo(EstadoPago.PENDIENTE);
        assertThat(resultado.pago().urlPago()).isEqualTo(URL_PAGO);
        assertThat(resultado.pago().montoUsd()).isEqualByComparingTo("38.23");
        assertThat(resultado.pago().montoBs()).isEqualByComparingTo("450.00");
        assertThat(resultado.pago().horas()).isEqualTo((short) 10);
    }

    @Test
    void iniciar_mandaALaPasarelaElMontoEnCentavosLaClaveDelPagoYLaPaginaDeRetorno() {
        compraNueva();
        when(pasarela.crearSesionCobro(any())).thenReturn(new SesionCobro("cs_test_1", URL_PAGO, AHORA));

        servicio.iniciar(ESCUELA, ESTUDIANTE, PLAN, CLAVE);

        ArgumentCaptor<SolicitudCobro> solicitud = ArgumentCaptor.forClass(SolicitudCobro.class);
        verify(pasarela).crearSesionCobro(solicitud.capture());
        assertThat(solicitud.getValue().montoCentavosUsd()).isEqualTo(3823L);
        assertThat(solicitud.getValue().idempotencyKey()).isEqualTo(CLAVE);
        assertThat(solicitud.getValue().nombreProducto()).isEqualTo("Plan 10 horas");
        assertThat(solicitud.getValue().expiraEn()).isEqualTo(AHORA.plusSeconds(31 * 60));
        assertThat(solicitud.getValue().urlExito()).isEqualTo(PagosPropertiesPrueba.URL_RETORNO);
        assertThat(solicitud.getValue().urlCancelacion()).isEqualTo(solicitud.getValue().urlExito());
    }

    @Test
    void iniciar_guardaLaSesionDeLaPasarela() {
        compraNueva();
        when(pasarela.crearSesionCobro(any())).thenReturn(new SesionCobro("cs_test_1", URL_PAGO, AHORA));

        servicio.iniciar(ESCUELA, ESTUDIANTE, PLAN, CLAVE);

        ArgumentCaptor<TransaccionPasarela> transaccion = ArgumentCaptor.forClass(TransaccionPasarela.class);
        verify(transaccionRepository).saveAndFlush(transaccion.capture());
        assertThat(transaccion.getValue().getProveedor()).isEqualTo("STRIPE");
        assertThat(transaccion.getValue().getEstado()).isEqualTo("CREADA");
        assertThat(transaccion.getValue().getReferenciaExterna()).isEqualTo("cs_test_1");
        assertThat(transaccion.getValue().getUrlPago()).isEqualTo(URL_PAGO);
    }

    // ------------------------------------------------------------------ iniciar: validaciones

    @Test
    void iniciar_conLaClaveVaciaNulaOMuyLarga_esErrSys01() {
        for (String clave : new String[] {null, "", "   ", "x".repeat(65)}) {
            BusinessException ex = error(() -> servicio.iniciar(ESCUELA, ESTUDIANTE, PLAN, clave));

            assertThat(ex).isInstanceOf(ValidationException.class);
            assertThat(ex.getErrorCode()).isEqualTo("ERR_SYS_01");
        }
        verifyNoInteractions(pasarela, identidad);
    }

    @Test
    void iniciar_conUnPlanInexistenteODeOtraEscuela_esErrPag05_sinCrearNada() {
        when(pagoRepository.findByIdempotencyKey(CLAVE)).thenReturn(Optional.empty());
        when(planRepository.findByCodigoAndEscuelaCodigo(PLAN, ESCUELA)).thenReturn(Optional.empty());

        BusinessException ex = error(() -> servicio.iniciar(ESCUELA, ESTUDIANTE, PLAN, CLAVE));

        assertThat(ex).isInstanceOf(NotFoundException.class);
        assertThat(ex.getErrorCode()).isEqualTo("ERR_PAG_05");
        verify(pagoRepository, never()).saveAndFlush(any());
        verifyNoInteractions(pasarela, identidad);
    }

    @Test
    void iniciar_conUnPlanDesactivado_esErrPag05() {
        when(pagoRepository.findByIdempotencyKey(CLAVE)).thenReturn(Optional.empty());
        when(planRepository.findByCodigoAndEscuelaCodigo(PLAN, ESCUELA)).thenReturn(Optional.of(plan(false)));

        BusinessException ex = error(() -> servicio.iniciar(ESCUELA, ESTUDIANTE, PLAN, CLAVE));

        assertThat(ex.getErrorCode()).isEqualTo("ERR_PAG_05");
        verify(pagoRepository, never()).saveAndFlush(any());
    }

    @Test
    void iniciar_conIdentidadCaida_esErrPag15_sinCrearElPago() {
        when(pagoRepository.findByIdempotencyKey(CLAVE)).thenReturn(Optional.empty());
        when(planRepository.findByCodigoAndEscuelaCodigo(PLAN, ESCUELA)).thenReturn(Optional.of(plan(true)));
        when(bearer.cabecera()).thenReturn("Bearer token");
        when(identidad.perfilPropio("Bearer token")).thenThrow(
            new ServicioNoDisponibleException("ERR_PAG_15", "No pudimos verificar tus datos"));

        BusinessException ex = error(() -> servicio.iniciar(ESCUELA, ESTUDIANTE, PLAN, CLAVE));

        assertThat(ex.getErrorCode()).isEqualTo("ERR_PAG_15");
        verify(pagoRepository, never()).saveAndFlush(any());
        verifyNoInteractions(pasarela);
    }

    // ------------------------------------------------------------------ iniciar: reintentos

    @Test
    void iniciar_conLaMismaClaveYLaSesionCreada_devuelveElMismoPagoSinTocarLaPasarela() {
        Pago existente = pagoExistente(ESTUDIANTE, EstadoPago.PENDIENTE, AHORA.plusSeconds(1800));
        when(pagoRepository.findByIdempotencyKey(CLAVE)).thenReturn(Optional.of(existente));
        when(transaccionRepository.findFirstByPagoIdOrderByIdDesc(existente.getId()))
            .thenReturn(Optional.of(sesionGuardada(existente)));

        PagoService.ResultadoIniciar resultado = servicio.iniciar(ESCUELA, ESTUDIANTE, PLAN, CLAVE);

        assertThat(resultado.creado()).isFalse();
        assertThat(resultado.pago().codigo()).isEqualTo(existente.getCodigo());
        assertThat(resultado.pago().urlPago()).isEqualTo(URL_PAGO);
        verify(pagoRepository, never()).saveAndFlush(any());
        verifyNoInteractions(pasarela, identidad);
    }

    @Test
    void iniciar_conLaMismaClaveDeUnPagoYaFallido_noOfreceLaPaginaDePago() {
        Pago existente = pagoExistente(ESTUDIANTE, EstadoPago.FALLIDO, AHORA.minusSeconds(60));
        when(pagoRepository.findByIdempotencyKey(CLAVE)).thenReturn(Optional.of(existente));
        when(transaccionRepository.findFirstByPagoIdOrderByIdDesc(existente.getId()))
            .thenReturn(Optional.of(sesionGuardada(existente)));

        PagoService.ResultadoIniciar resultado = servicio.iniciar(ESCUELA, ESTUDIANTE, PLAN, CLAVE);

        assertThat(resultado.pago().estado()).isEqualTo(EstadoPago.FALLIDO);
        assertThat(resultado.pago().urlPago()).isNull();
    }

    @Test
    void iniciar_conLaClaveDeOtroEstudiante_esErrPag18() {
        Pago existente = pagoExistente(UUID.randomUUID(), EstadoPago.PENDIENTE, AHORA.plusSeconds(1800));
        when(pagoRepository.findByIdempotencyKey(CLAVE)).thenReturn(Optional.of(existente));

        BusinessException ex = error(() -> servicio.iniciar(ESCUELA, ESTUDIANTE, PLAN, CLAVE));

        assertThat(ex).isInstanceOf(ConflictException.class);
        assertThat(ex.getErrorCode()).isEqualTo("ERR_PAG_18");
    }

    @Test
    void iniciar_conLaClaveDeOtroPlan_esErrPag18() {
        Pago existente = pagoExistente(ESTUDIANTE, EstadoPago.PENDIENTE, AHORA.plusSeconds(1800));
        when(pagoRepository.findByIdempotencyKey(CLAVE)).thenReturn(Optional.of(existente));

        BusinessException ex = error(() -> servicio.iniciar(ESCUELA, ESTUDIANTE, UUID.randomUUID(), CLAVE));

        assertThat(ex.getErrorCode()).isEqualTo("ERR_PAG_18");
    }

    @Test
    void iniciar_conLaMismaClaveYSinSesionPeroVigente_retomaLaCreacionDeLaSesion() {
        Pago existente = pagoExistente(ESTUDIANTE, EstadoPago.PENDIENTE, AHORA.plusSeconds(1800));
        when(pagoRepository.findByIdempotencyKey(CLAVE)).thenReturn(Optional.of(existente));
        when(transaccionRepository.findFirstByPagoIdOrderByIdDesc(existente.getId())).thenReturn(Optional.empty());
        when(pasarela.crearSesionCobro(any())).thenReturn(new SesionCobro("cs_test_1", URL_PAGO, AHORA));
        when(pagoRepository.getReferenceById(existente.getId())).thenReturn(existente);

        PagoService.ResultadoIniciar resultado = servicio.iniciar(ESCUELA, ESTUDIANTE, PLAN, CLAVE);

        assertThat(resultado.creado()).isFalse();
        assertThat(resultado.pago().urlPago()).isEqualTo(URL_PAGO);
        verify(transaccionRepository).saveAndFlush(any(TransaccionPasarela.class));
        verify(pagoRepository, never()).saveAndFlush(any());
    }

    @Test
    void iniciar_tras_unaFallaDeLaPasarela_conLaMismaClave_reabreElPagoYReintenta() {
        Pago existente = pagoExistente(ESTUDIANTE, EstadoPago.FALLIDO, AHORA.plusSeconds(1800));
        when(pagoRepository.findByIdempotencyKey(CLAVE)).thenReturn(Optional.of(existente));
        when(transaccionRepository.findFirstByPagoIdOrderByIdDesc(existente.getId())).thenReturn(Optional.empty());
        when(pagoRepository.findById(existente.getId())).thenReturn(Optional.of(existente));
        when(pagoRepository.saveAndFlush(existente)).thenReturn(existente);
        when(pagoRepository.getReferenceById(existente.getId())).thenReturn(existente);
        when(pasarela.crearSesionCobro(any())).thenReturn(new SesionCobro("cs_test_1", URL_PAGO, AHORA));

        PagoService.ResultadoIniciar resultado = servicio.iniciar(ESCUELA, ESTUDIANTE, PLAN, CLAVE);

        assertThat(existente.getEstado()).isEqualTo(EstadoPago.PENDIENTE);
        assertThat(resultado.pago().estado()).isEqualTo(EstadoPago.PENDIENTE);
        assertThat(resultado.pago().urlPago()).isEqualTo(URL_PAGO);
        assertThat(resultado.creado()).isFalse();
    }

    @Test
    void iniciar_conLaMismaClaveSinSesionYYaVencido_loDejaFallidoSinLlamarALaPasarela() {
        Pago existente = pagoExistente(ESTUDIANTE, EstadoPago.PENDIENTE, AHORA.minusSeconds(1));
        when(pagoRepository.findByIdempotencyKey(CLAVE)).thenReturn(Optional.of(existente));
        when(transaccionRepository.findFirstByPagoIdOrderByIdDesc(existente.getId())).thenReturn(Optional.empty());
        when(pagoRepository.findById(existente.getId())).thenReturn(Optional.of(existente));
        when(pagoRepository.saveAndFlush(existente)).thenReturn(existente);

        PagoService.ResultadoIniciar resultado = servicio.iniciar(ESCUELA, ESTUDIANTE, PLAN, CLAVE);

        assertThat(existente.getEstado()).isEqualTo(EstadoPago.FALLIDO);
        assertThat(resultado.pago().urlPago()).isNull();
        verifyNoInteractions(pasarela);
    }

    @Test
    void iniciar_conUnaCarreraEnLaClave_elQuePierdeSigueComoReintento() {
        Pago ganador = pagoExistente(ESTUDIANTE, EstadoPago.PENDIENTE, AHORA.plusSeconds(1800));
        when(pagoRepository.findByIdempotencyKey(CLAVE)).thenReturn(Optional.empty(), Optional.of(ganador));
        when(planRepository.findByCodigoAndEscuelaCodigo(PLAN, ESCUELA)).thenReturn(Optional.of(plan(true)));
        when(bearer.cabecera()).thenReturn("Bearer token");
        when(identidad.perfilPropio("Bearer token"))
            .thenReturn(new IdentidadClient.PerfilEstudiante("Ana Ruiz", "Escuela Example"));
        when(pagoRepository.saveAndFlush(any(Pago.class))).thenThrow(new DataIntegrityViolationException("clave"));
        when(transaccionRepository.findFirstByPagoIdOrderByIdDesc(ganador.getId()))
            .thenReturn(Optional.of(sesionGuardada(ganador)));

        PagoService.ResultadoIniciar resultado = servicio.iniciar(ESCUELA, ESTUDIANTE, PLAN, CLAVE);

        assertThat(resultado.creado()).isFalse();
        assertThat(resultado.pago().codigo()).isEqualTo(ganador.getCodigo());
        verifyNoInteractions(pasarela);
    }

    @Test
    void iniciar_conUnaViolacionQueNoEsLaClave_sePropaga() {
        when(pagoRepository.findByIdempotencyKey(CLAVE)).thenReturn(Optional.empty(), Optional.empty());
        when(planRepository.findByCodigoAndEscuelaCodigo(PLAN, ESCUELA)).thenReturn(Optional.of(plan(true)));
        when(bearer.cabecera()).thenReturn("Bearer token");
        when(identidad.perfilPropio("Bearer token"))
            .thenReturn(new IdentidadClient.PerfilEstudiante("Ana Ruiz", "Escuela Example"));
        when(pagoRepository.saveAndFlush(any(Pago.class))).thenThrow(new DataIntegrityViolationException("otra"));

        assertThatThrownBy(() -> servicio.iniciar(ESCUELA, ESTUDIANTE, PLAN, CLAVE))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ------------------------------------------------------------------ iniciar: la pasarela

    @Test
    void iniciar_siLaPasarelaFalla_dejaElPagoFallidoYRelanzaErrPag06() {
        compraNueva();
        when(pasarela.crearSesionCobro(any())).thenThrow(
            new ServicioNoDisponibleException("ERR_PAG_06", "No pudimos conectar con el pago"));
        when(pagoRepository.findById(any())).thenAnswer(i -> Optional.of(ultimoPersistido));

        BusinessException ex = error(() -> servicio.iniciar(ESCUELA, ESTUDIANTE, PLAN, CLAVE));

        assertThat(ex).isInstanceOf(ServicioNoDisponibleException.class);
        assertThat(ex.getErrorCode()).isEqualTo("ERR_PAG_06");
        assertThat(ultimoPersistido.getEstado()).isEqualTo(EstadoPago.FALLIDO);
        verify(transaccionRepository, never()).saveAndFlush(any());
    }

    @Test
    void iniciar_siOtroIntentoYaGuardoLaMismaSesion_usaLaGuardada() {
        compraNueva();
        when(pasarela.crearSesionCobro(any())).thenReturn(new SesionCobro("cs_test_1", URL_PAGO, AHORA));
        when(transaccionRepository.saveAndFlush(any(TransaccionPasarela.class)))
            .thenThrow(new DataIntegrityViolationException("referencia"));
        when(pagoRepository.findById(any())).thenAnswer(i -> Optional.of(ultimoPersistido));
        when(transaccionRepository.findFirstByPagoIdOrderByIdDesc(any()))
            .thenReturn(Optional.empty())
            .thenAnswer(i -> Optional.of(sesionGuardada(ultimoPersistido)));

        PagoService.ResultadoIniciar resultado = servicio.iniciar(ESCUELA, ESTUDIANTE, PLAN, CLAVE);

        assertThat(resultado.pago().urlPago()).isEqualTo(URL_PAGO);
    }

    @Test
    void iniciar_siLaSesionNoQuedoGuardada_esErrPag17() {
        compraNueva();
        when(pasarela.crearSesionCobro(any())).thenReturn(new SesionCobro("cs_test_1", URL_PAGO, AHORA));
        when(transaccionRepository.saveAndFlush(any(TransaccionPasarela.class)))
            .thenThrow(new DataIntegrityViolationException("referencia"));
        when(pagoRepository.findById(any())).thenAnswer(i -> Optional.of(ultimoPersistido));
        when(transaccionRepository.findFirstByPagoIdOrderByIdDesc(any())).thenReturn(Optional.empty());

        BusinessException ex = error(() -> servicio.iniciar(ESCUELA, ESTUDIANTE, PLAN, CLAVE));

        assertThat(ex).isInstanceOf(ConflictException.class);
        assertThat(ex.getErrorCode()).isEqualTo("ERR_PAG_17");
    }

    // ------------------------------------------------------------------ estado

    @Test
    void estado_devuelveElEstadoDelPagoPropio() {
        Pago pago = pagoExistente(ESTUDIANTE, EstadoPago.CONFIRMADO, AHORA);
        when(pagoRepository.findByCodigoAndEstudianteCodigoAndEscuelaCodigo(pago.getCodigo(), ESTUDIANTE, ESCUELA))
            .thenReturn(Optional.of(pago));

        EstadoPagoResponse respuesta = servicio.estado(ESCUELA, ESTUDIANTE, pago.getCodigo());

        assertThat(respuesta.codigo()).isEqualTo(pago.getCodigo());
        assertThat(respuesta.estado()).isEqualTo(EstadoPago.CONFIRMADO);
    }

    @Test
    void estado_deUnPagoAjenoOInexistente_esErrPag07() {
        UUID codigo = UUID.randomUUID();
        when(pagoRepository.findByCodigoAndEstudianteCodigoAndEscuelaCodigo(codigo, ESTUDIANTE, ESCUELA))
            .thenReturn(Optional.empty());

        BusinessException ex = error(() -> servicio.estado(ESCUELA, ESTUDIANTE, codigo));

        assertThat(ex).isInstanceOf(NotFoundException.class);
        assertThat(ex.getErrorCode()).isEqualTo("ERR_PAG_07");
    }

    // ------------------------------------------------------------------ logs

    @Test
    void iniciar_registraSoloCodigos_nuncaNombresMontosNiLaDireccionDePago() {
        compraNueva();
        when(pasarela.crearSesionCobro(any())).thenReturn(new SesionCobro("cs_test_1", URL_PAGO, AHORA));

        servicio.iniciar(ESCUELA, ESTUDIANTE, PLAN, CLAVE);

        assertThat(logs.list).isNotEmpty();
        String todo = logs.list.stream().map(e -> e.getFormattedMessage() + " " + e.getArgumentArray().length
            + java.util.Arrays.toString(e.getArgumentArray())).reduce("", String::concat);
        assertThat(todo).doesNotContain("Ana Ruiz").doesNotContain("Escuela Example").doesNotContain("38.23")
            .doesNotContain("checkout.stripe.com").doesNotContain(CLAVE);
    }
}
