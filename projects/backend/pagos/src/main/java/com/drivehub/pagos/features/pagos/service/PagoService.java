package com.drivehub.pagos.features.pagos.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.drivehub.pagos.common.exception.ConflictException;
import com.drivehub.pagos.common.exception.NotFoundException;
import com.drivehub.pagos.common.exception.PagosErrorCodes;
import com.drivehub.pagos.common.exception.ServicioNoDisponibleException;
import com.drivehub.pagos.common.exception.ValidationException;
import com.drivehub.pagos.config.BearerActual;
import com.drivehub.pagos.config.PagosProperties;
import com.drivehub.pagos.features.pagos.client.IdentidadClient;
import com.drivehub.pagos.features.pagos.dto.EstadoPagoResponse;
import com.drivehub.pagos.features.pagos.dto.PagoIniciadoResponse;
import com.drivehub.pagos.features.pagos.enums.EstadoPago;
import com.drivehub.pagos.features.pagos.model.Pago;
import com.drivehub.pagos.features.pagos.model.TransaccionPasarela;
import com.drivehub.pagos.features.pagos.pasarela.PasarelaPago;
import com.drivehub.pagos.features.pagos.pasarela.SesionCobro;
import com.drivehub.pagos.features.pagos.pasarela.SolicitudCobro;
import com.drivehub.pagos.features.pagos.repository.PagoRepository;
import com.drivehub.pagos.features.pagos.repository.TransaccionPasarelaRepository;
import com.drivehub.pagos.features.planes.model.PlanPago;
import com.drivehub.pagos.features.planes.repository.PlanPagoRepository;
import com.drivehub.pagos.features.planes.service.ConversionMoneda;

/**
 * La compra de un plan de horas (CU26): crea el pago pendiente con todo lo comprado copiado y pide a la
 * pasarela la página donde el estudiante paga. Confirmar el pago y acreditar las horas es de CU27.
 *
 * * Cada intento de compra lleva una clave de idempotencia (la del pago, única): reintentarla devuelve el
 * * mismo pago y la misma página de pago, nunca un segundo cobro.
 * * NUNCA se llama a la pasarela con una transacción abierta: se guarda el pago, se llama, y recién después
 * * se guarda la sesión. Por eso las transacciones son explícitas (`TransactionTemplate`).
 * * La escuela y el estudiante son siempre los del token (los pasa el controller).
 * ! No se loguean nombres, montos ni la dirección de pago; solo códigos.
 */
@Service
public class PagoService {

    static final int CLAVE_MAXIMA = 64;
    static final Duration HOLGURA_DE_VIGENCIA = Duration.ofMinutes(1);
    static final int TAMANO_LOTE_VENCIMIENTO = 100;

    private static final Logger LOG = LoggerFactory.getLogger(PagoService.class);
    private static final String MODULO = "pagos";
    private static final String PROVEEDOR = "STRIPE";
    private static final String SESION_CREADA = "CREADA";

    private final PagoRepository pagoRepository;
    private final TransaccionPasarelaRepository transaccionRepository;
    private final PlanPagoRepository planRepository;
    private final ConversionMoneda conversion;
    private final PagosProperties propiedades;
    private final PasarelaPago pasarela;
    private final IdentidadClient identidad;
    private final BearerActual bearer;
    private final Clock reloj;
    private final TransactionTemplate escritura;
    private final TransactionTemplate lectura;

    public PagoService(PagoRepository pagoRepository, TransaccionPasarelaRepository transaccionRepository,
            PlanPagoRepository planRepository, ConversionMoneda conversion, PagosProperties propiedades,
            PasarelaPago pasarela, IdentidadClient identidad, BearerActual bearer, Clock reloj,
            PlatformTransactionManager transactionManager) {
        this.pagoRepository = pagoRepository;
        this.transaccionRepository = transaccionRepository;
        this.planRepository = planRepository;
        this.conversion = conversion;
        this.propiedades = propiedades;
        this.pasarela = pasarela;
        this.identidad = identidad;
        this.bearer = bearer;
        this.reloj = reloj;
        this.escritura = new TransactionTemplate(transactionManager);
        this.lectura = new TransactionTemplate(transactionManager);
        this.lectura.setReadOnly(true);
    }

    /**
     * El resultado de iniciar la compra: lo que se le muestra al estudiante y si el pago se creó recién (201)
     * o es el de un reintento (200).
     */
    public record ResultadoIniciar(PagoIniciadoResponse pago, boolean creado) {
    }

    /**
     * Una vista plana del pago, armada dentro de una transacción para no depender de relaciones perezosas.
     */
    private record Registro(Long id, UUID codigo, UUID escuelaCodigo, UUID estudianteCodigo, UUID planCodigo,
            EstadoPago estado, String planNombre, short horas, BigDecimal monto, BigDecimal montoBs,
            Instant expiraEn, String urlPago, boolean conSesion) {

        PagoIniciadoResponse aRespuesta() {
            String url = estado == EstadoPago.PENDIENTE ? urlPago : null;
            return new PagoIniciadoResponse(codigo, estado, url, expiraEn, planNombre, horas, monto, montoBs);
        }
    }

    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResultadoIniciar iniciar(UUID escuelaCodigo, UUID estudianteCodigo, UUID planCodigo,
            String idempotencyKey) {
        String clave = validarClave(idempotencyKey);

        Optional<Registro> previo = buscarPorClave(clave);
        if (previo.isPresent()) {
            return repetir(previo.get(), escuelaCodigo, estudianteCodigo, planCodigo, clave);
        }

        PlanPago plan = lectura.execute(estado -> planRepository.findByCodigoAndEscuelaCodigo(planCodigo,
            escuelaCodigo).filter(PlanPago::isActivo).orElse(null));
        if (plan == null) {
            throw new NotFoundException(PagosErrorCodes.PLAN_NO_DISPONIBLE, "El plan ya no está disponible");
        }
        IdentidadClient.PerfilEstudiante perfil = identidad.perfilPropio(bearer.cabecera());
        BigDecimal montoUsd = conversion.aDolares(plan.getCostoBs());
        // * Un minuto de holgura: la pasarela exige al menos 30 minutos completos de vigencia, y entre que se
        // * calcula la hora y la pasarela recibe el pedido pasan unos segundos
        Instant expira = reloj.instant().plus(Duration.ofMinutes(propiedades.pasarela().vigenciaSesionMinutos()))
            .plus(HOLGURA_DE_VIGENCIA);
        Pago nuevo = new Pago(escuelaCodigo, estudianteCodigo, plan, montoUsd, propiedades.tipoCambioBsUsd(),
            perfil.nombreCompleto(), perfil.escuelaNombre(), clave, expira);

        Registro guardado;
        try {
            guardado = escritura.execute(estado -> registro(pagoRepository.saveAndFlush(nuevo)));
        } catch (DataIntegrityViolationException e) {
            // * Dos intentos con la misma clave llegaron a la vez: el que perdió sigue como un reintento
            Optional<Registro> ganador = buscarPorClave(clave);
            if (ganador.isPresent()) {
                return repetir(ganador.get(), escuelaCodigo, estudianteCodigo, planCodigo, clave);
            }
            throw e;
        }
        LOG.info("Pago iniciado", kv("module", MODULO), kv("pagoCodigo", guardado.codigo()),
            kv("escuelaCodigo", escuelaCodigo));
        return new ResultadoIniciar(crearSesion(guardado, clave).aRespuesta(), true);
    }

    @PreAuthorize("hasRole('ESTUDIANTE')")
    @Transactional(readOnly = true)
    public EstadoPagoResponse estado(UUID escuelaCodigo, UUID estudianteCodigo, UUID codigo) {
        Pago pago = pagoRepository.findByCodigoAndEstudianteCodigoAndEscuelaCodigo(codigo, estudianteCodigo,
            escuelaCodigo).orElseThrow(() -> new NotFoundException(PagosErrorCodes.PAGO_NO_ENCONTRADO,
            "El pago no existe"));
        return new EstadoPagoResponse(pago.getCodigo(), pago.getEstado());
    }

    // ------------------------------------------------------------------ vencimiento

    /**
     * Pasa a `FALLIDO` los pagos pendientes cuya página de pago venció hace más que el margen (CU26). Lo corre
     * un proceso periódico, sin sesión de nadie: por eso no lleva `@PreAuthorize`. Cada pago se cambia en su
     * propia transacción, y uno que falle no frena a los demás. Un pago que dejó de estar pendiente entre la
     * lectura y el cambio (llegó el aviso de la pasarela) no se toca.
     *
     * @return cuántos pagos cambió
     */
    public int vencerPendientes(Instant ahora) {
        Instant limite = ahora.minus(Duration.ofMinutes(propiedades.vencimiento().margenMinutos()));
        Set<Long> intentados = new HashSet<>();
        int vencidos = 0;
        while (true) {
            List<Long> lote = lectura.execute(estado -> pagoRepository.idsVencidos(EstadoPago.PENDIENTE, limite,
                PageRequest.of(0, TAMANO_LOTE_VENCIMIENTO + intentados.size())));
            List<Long> nuevos = lote.stream().filter(id -> !intentados.contains(id)).toList();
            if (nuevos.isEmpty()) {
                return vencidos;
            }
            for (Long id : nuevos) {
                intentados.add(id);
                try {
                    if (Boolean.TRUE.equals(escritura.execute(estado -> vencer(id)))) {
                        vencidos++;
                    }
                } catch (RuntimeException e) {
                    LOG.error("No se pudo vencer un pago pendiente", kv("module", MODULO), kv("pagoId", id), e);
                }
            }
        }
    }

    private boolean vencer(Long pagoId) {
        return pagoRepository.findById(pagoId).filter(pago -> pago.getEstado() == EstadoPago.PENDIENTE)
            .map(pago -> {
                pago.setEstado(EstadoPago.FALLIDO);
                pagoRepository.saveAndFlush(pago);
                return true;
            }).orElse(false);
    }

    // ------------------------------------------------------------------ reintentos

    /**
     * La clave ya se usó: es un reintento de la misma compra (mismo estudiante, escuela y plan) o es un error
     * del cliente. Un reintento devuelve el mismo pago; si la sesión de cobro nunca llegó a crearse, se retoma.
     */
    private ResultadoIniciar repetir(Registro previo, UUID escuelaCodigo, UUID estudianteCodigo, UUID planCodigo,
            String clave) {
        boolean esLaMismaCompra = previo.estudianteCodigo().equals(estudianteCodigo)
            && previo.escuelaCodigo().equals(escuelaCodigo) && previo.planCodigo().equals(planCodigo);
        if (!esLaMismaCompra) {
            throw new ConflictException(PagosErrorCodes.CLAVE_DE_COMPRA_REUTILIZADA,
                "Esa clave de compra ya se usó para otra compra");
        }
        if (previo.conSesion()) {
            return new ResultadoIniciar(previo.aRespuesta(), false);
        }
        if (!previo.expiraEn().isAfter(reloj.instant())) {
            // * La página de pago ya habría vencido: no se retoma, y se muestra como fallida
            Registro vencido = marcarFallido(previo.id(), previo);
            return new ResultadoIniciar(vencido.aRespuesta(), false);
        }
        Registro retomado = previo.estado() == EstadoPago.FALLIDO ? reabrir(previo) : previo;
        return new ResultadoIniciar(crearSesion(retomado, clave).aRespuesta(), false);
    }

    /** Un pago que falló al crear la sesión (la pasarela no respondía) vuelve a pendiente para reintentarlo. */
    private Registro reabrir(Registro registro) {
        return escritura.execute(estado -> pagoRepository.findById(registro.id()).map(pago -> {
            pago.setEstado(EstadoPago.PENDIENTE);
            return registro(pagoRepository.saveAndFlush(pago));
        }).orElse(registro));
    }

    // ------------------------------------------------------------------ pasarela

    /**
     * Pide la página de pago y guarda la sesión. Si la pasarela falla, el pago queda `FALLIDO` y se relanza el
     * 503; la misma clave puede reintentarse más tarde (ver {@link #repetir}).
     */
    private Registro crearSesion(Registro registro, String clave) {
        String urlRetorno = propiedades.pasarela().urlRetorno();
        SolicitudCobro solicitud = new SolicitudCobro(registro.codigo(), clave, registro.planNombre(),
            enCentavos(registro.monto()), registro.expiraEn(), urlRetorno, urlRetorno);
        SesionCobro sesion;
        try {
            sesion = pasarela.crearSesionCobro(solicitud);
        } catch (ServicioNoDisponibleException e) {
            marcarFallido(registro.id(), registro);
            LOG.warn("La pasarela no pudo crear la sesión de cobro", kv("module", MODULO),
                kv("pagoCodigo", registro.codigo()), kv("errorCode", e.getErrorCode()));
            throw e;
        }
        try {
            escritura.executeWithoutResult(estado -> {
                Pago pago = pagoRepository.getReferenceById(registro.id());
                TransaccionPasarela transaccion = new TransaccionPasarela(pago, PROVEEDOR, SESION_CREADA);
                transaccion.setReferenciaExterna(sesion.referenciaExterna());
                transaccion.setUrlPago(sesion.urlPago());
                transaccionRepository.saveAndFlush(transaccion);
            });
        } catch (DataIntegrityViolationException e) {
            // * Otro intento de la misma compra ya guardó esta misma sesión (la clave de idempotencia de la
            // * pasarela devuelve la misma): se usa la que quedó guardada
            Optional<Registro> guardado = lectura.execute(estado -> pagoRepository.findById(registro.id())
                .map(this::registro));
            if (guardado.isPresent() && guardado.get().conSesion()) {
                return guardado.get();
            }
            throw new ConflictException(PagosErrorCodes.COMPRA_EN_CURSO,
                "Tu compra ya se está procesando, espera unos segundos");
        }
        return new Registro(registro.id(), registro.codigo(), registro.escuelaCodigo(), registro.estudianteCodigo(),
            registro.planCodigo(), registro.estado(), registro.planNombre(), registro.horas(), registro.monto(),
            registro.montoBs(), registro.expiraEn(), sesion.urlPago(), true);
    }

    /** Pasa a `FALLIDO` un pago que sigue pendiente; devuelve cómo quedó. */
    private Registro marcarFallido(Long pagoId, Registro porDefecto) {
        return escritura.execute(estado -> pagoRepository.findById(pagoId).map(pago -> {
            if (pago.getEstado() == EstadoPago.PENDIENTE) {
                pago.setEstado(EstadoPago.FALLIDO);
                pagoRepository.saveAndFlush(pago);
            }
            return registro(pago);
        }).orElse(porDefecto));
    }

    // ------------------------------------------------------------------ utilidades

    private Optional<Registro> buscarPorClave(String clave) {
        return lectura.execute(estado -> pagoRepository.findByIdempotencyKey(clave).map(this::registro));
    }

    private Registro registro(Pago pago) {
        Optional<TransaccionPasarela> transaccion = transaccionRepository.findFirstByPagoIdOrderByIdDesc(
            pago.getId());
        String url = transaccion.map(TransaccionPasarela::getUrlPago).orElse(null);
        return new Registro(pago.getId(), pago.getCodigo(), pago.getEscuelaCodigo(), pago.getEstudianteCodigo(),
            pago.getPlanPago().getCodigo(), pago.getEstado(), pago.getPlanNombre(), pago.getHoras(), pago.getMonto(),
            pago.getMontoBs(), pago.getExpiraEn(), url, url != null);
    }

    private static String validarClave(String clave) {
        if (clave == null || clave.isBlank() || clave.length() > CLAVE_MAXIMA) {
            throw new ValidationException(PagosErrorCodes.VALIDACION,
                "La clave de la compra es obligatoria (de 1 a " + CLAVE_MAXIMA + " caracteres)");
        }
        return clave;
    }

    /** Dólares a centavos enteros: la pasarela cobra en la unidad menor. */
    private static long enCentavos(BigDecimal montoUsd) {
        return montoUsd.movePointRight(2).longValueExact();
    }
}
