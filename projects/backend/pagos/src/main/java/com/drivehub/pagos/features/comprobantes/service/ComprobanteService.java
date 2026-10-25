package com.drivehub.pagos.features.comprobantes.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.drivehub.pagos.features.comprobantes.almacen.AlmacenComprobantes;
import com.drivehub.pagos.features.comprobantes.dto.DatosComprobante;
import com.drivehub.pagos.features.comprobantes.enums.TipoComprobante;
import com.drivehub.pagos.features.comprobantes.model.Comprobante;
import com.drivehub.pagos.features.comprobantes.repository.ComprobanteRepository;
import com.drivehub.pagos.features.pagos.enums.EstadoPago;
import com.drivehub.pagos.features.pagos.model.Pago;
import com.drivehub.pagos.features.pagos.model.ReversionPago;
import com.drivehub.pagos.features.pagos.repository.PagoRepository;
import com.drivehub.pagos.features.pagos.repository.ReversionPagoRepository;

/**
 * Genera el comprobante de una compra o de un reembolso (CU30) y reintenta los que quedaron pendientes.
 *
 * * Lo dispara el sistema (una compra confirmada, un reembolso ejecutado): no lleva `@PreAuthorize` ni sesión.
 * * Idempotente: un comprobante por pago y tipo. Pedirlo otra vez no genera otro; si el existente no tiene PDF,
 * * solo reintenta el PDF.
 * * El comprobante se numera y queda creado ANTES de dibujar el PDF. Si el PDF o el almacén fallan, el
 * * comprobante queda pendiente (`url_pdf` nulo) y un proceso lo reintenta; la falla NUNCA se propaga: un pago
 * * confirmado jamás se revierte por esto.
 * * Los datos salen SOLO de las copias que guarda el pago: no se llama a ningún otro servicio.
 * * No se abre una transacción alrededor del PDF ni del almacén: cada paso de base de datos tiene la suya.
 * ! No se loguean nombres ni montos; solo códigos.
 */
@Service
public class ComprobanteService {

    static final int TAMANO_LOTE = 50;
    static final Duration ESPERA_ANTES_DE_REINTENTAR = Duration.ofMinutes(1);

    private static final Logger LOG = LoggerFactory.getLogger(ComprobanteService.class);
    private static final String MODULO = "comprobantes";
    private static final String METODO_TARJETA = "Tarjeta · procesado por Stripe";

    private final PagoRepository pagoRepository;
    private final ReversionPagoRepository reversionRepository;
    private final ComprobanteRepository comprobanteRepository;
    private final ComprobantePdfGenerador generador;
    private final AlmacenComprobantes almacen;
    private final TextoDeMotivoReversion textoDeMotivo;
    private final Clock reloj;
    private final TransactionTemplate escritura;
    private final TransactionTemplate lectura;

    public ComprobanteService(PagoRepository pagoRepository, ReversionPagoRepository reversionRepository,
            ComprobanteRepository comprobanteRepository, ComprobantePdfGenerador generador,
            AlmacenComprobantes almacen, TextoDeMotivoReversion textoDeMotivo, Clock reloj,
            PlatformTransactionManager transactionManager) {
        this.pagoRepository = pagoRepository;
        this.reversionRepository = reversionRepository;
        this.comprobanteRepository = comprobanteRepository;
        this.generador = generador;
        this.almacen = almacen;
        this.textoDeMotivo = textoDeMotivo;
        this.reloj = reloj;
        // * REQUIRES_NEW: el comprobante se pide justo después del commit del pago (AFTER_COMMIT), cuando esa
        // * transacción ya terminó pero sigue "atada" al hilo: una transacción común se uniría a ella y NO se
        // * guardaría nada.
        this.escritura = new TransactionTemplate(transactionManager);
        this.escritura.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.lectura = new TransactionTemplate(transactionManager);
        this.lectura.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.lectura.setReadOnly(true);
    }

    /**
     * El comprobante de la compra de un pago confirmado. Un pago que no está confirmado no tiene comprobante (se
     * registra un `WARN`), salvo que ya lo tuviera y le falte el PDF.
     */
    public void generarDePago(UUID pagoCodigo) {
        try {
            Long pendiente = crearOReintentar(pagoCodigo, TipoComprobante.PAGO, null, null);
            if (pendiente != null) {
                completarPdf(pendiente);
            }
        } catch (RuntimeException e) {
            LOG.warn("No se pudo generar el comprobante de la compra (tipo {})", e.getClass().getSimpleName(),
                kv("module", MODULO), kv("pagoCodigo", pagoCodigo));
        }
    }

    /**
     * El comprobante del reembolso ejecutado de una compra, ligado a su reversión. Lo llama CU31.
     *
     * @param montoDevuelto lo que recibe el estudiante
     * @param montoRetenido lo que se retiene (cero en una compensación automática)
     */
    public void generarDeReembolso(UUID pagoCodigo, UUID reversionCodigo, BigDecimal montoDevuelto,
            BigDecimal montoRetenido) {
        try {
            Long pendiente = crearOReintentar(pagoCodigo, TipoComprobante.REEMBOLSO, reversionCodigo, montoDevuelto);
            if (pendiente != null) {
                completarPdf(pendiente, montoRetenido);
            }
        } catch (RuntimeException e) {
            LOG.warn("No se pudo generar el comprobante del reembolso (tipo {})", e.getClass().getSimpleName(),
                kv("module", MODULO), kv("pagoCodigo", pagoCodigo), kv("reversionCodigo", reversionCodigo));
        }
    }

    /**
     * Reintenta los comprobantes sin PDF creados hace más de un minuto (el reintento corre en un proceso aparte).
     *
     * @return cuántos quedaron completos
     */
    public int reintentarPendientes() {
        Instant limite = reloj.instant().minus(ESPERA_ANTES_DE_REINTENTAR);
        Set<Long> intentados = new HashSet<>();
        int completos = 0;
        while (true) {
            List<Comprobante> lote = lectura.execute(estado -> comprobanteRepository.pendientesAntesDe(limite,
                PageRequest.of(0, TAMANO_LOTE + intentados.size())));
            List<Comprobante> nuevos = lote.stream().filter(c -> !intentados.contains(c.getId())).toList();
            if (nuevos.isEmpty()) {
                return completos;
            }
            for (Comprobante pendiente : nuevos) {
                intentados.add(pendiente.getId());
                try {
                    if (completarPdf(pendiente.getId(), null)) {
                        completos++;
                    }
                } catch (RuntimeException e) {
                    LOG.warn("No se pudo completar un comprobante pendiente (tipo {})", e.getClass().getSimpleName(),
                        kv("module", MODULO), kv("comprobanteCodigo", pendiente.getCodigo()));
                }
            }
        }
    }

    // ------------------------------------------------------------------ pasos

    /**
     * Deja el comprobante creado y numerado, en su propia transacción.
     *
     * @return el id del comprobante al que le falta el PDF, o `null` si no hay nada que dibujar
     */
    private Long crearOReintentar(UUID pagoCodigo, TipoComprobante tipo, UUID reversionCodigo, BigDecimal monto) {
        try {
            return escritura.execute(estado -> preparar(pagoCodigo, tipo, reversionCodigo, monto));
        } catch (DataIntegrityViolationException e) {
            // * Otro hilo lo creó a la vez (índice único pago + tipo): se lee el que ganó
            return escritura.execute(estado -> preparar(pagoCodigo, tipo, reversionCodigo, monto));
        }
    }

    private Long preparar(UUID pagoCodigo, TipoComprobante tipo, UUID reversionCodigo, BigDecimal monto) {
        Optional<Pago> encontrado = pagoRepository.findByCodigo(pagoCodigo);
        if (encontrado.isEmpty()) {
            LOG.warn("Se pidió el comprobante de un pago inexistente", kv("module", MODULO),
                kv("pagoCodigo", pagoCodigo));
            return null;
        }
        Pago pago = encontrado.get();
        Optional<Comprobante> existente = comprobanteRepository.findByPagoIdAndTipo(pago.getId(), tipo);
        if (existente.isPresent()) {
            return existente.get().tienePdf() ? null : existente.get().getId();
        }
        if (tipo == TipoComprobante.PAGO && pago.getEstado() != EstadoPago.CONFIRMADO) {
            LOG.warn("Se pidió el comprobante de un pago que no está confirmado", kv("module", MODULO),
                kv("pagoCodigo", pagoCodigo));
            return null;
        }
        ReversionPago reversion = null;
        if (tipo == TipoComprobante.REEMBOLSO) {
            reversion = reversionRepository.findByCodigo(reversionCodigo).orElse(null);
            if (reversion == null || !reversion.getPago().getId().equals(pago.getId())) {
                LOG.warn("Se pidió el comprobante de un reembolso inexistente", kv("module", MODULO),
                    kv("reversionCodigo", reversionCodigo));
                return null;
            }
        }
        Instant ahora = reloj.instant();
        String numero = numerar(tipo, ahora);
        BigDecimal montoComprobante = tipo == TipoComprobante.PAGO ? pago.getMonto() : monto;
        Comprobante creado = comprobanteRepository.saveAndFlush(
            new Comprobante(pago, tipo, reversion, numero, montoComprobante, ahora));
        LOG.info("Comprobante creado", kv("module", MODULO), kv("comprobanteCodigo", creado.getCodigo()),
            kv("tipo", tipo));
        return creado.getId();
    }

    private boolean completarPdf(Long comprobanteId) {
        return completarPdf(comprobanteId, null);
    }

    /**
     * Dibuja el PDF y lo guarda. Si falla, el comprobante sigue pendiente.
     *
     * @param retenidoDelReembolso lo retenido, si se conoce (al reintentar, sale de la reversión)
     * @return si quedó completo
     */
    private boolean completarPdf(Long comprobanteId, BigDecimal retenidoDelReembolso) {
        try {
            DatosYClave datos = lectura.execute(estado -> datosDe(comprobanteId, retenidoDelReembolso));
            if (datos == null) {
                return false;
            }
            byte[] pdf = generador.generar(datos.datos());
            almacen.guardar(datos.clave(), pdf);
            escritura.executeWithoutResult(estado -> comprobanteRepository.findById(comprobanteId)
                .ifPresent(c -> c.setUrlPdf(datos.clave())));
            return true;
        } catch (RuntimeException e) {
            LOG.warn("El comprobante queda pendiente de PDF (tipo {})", e.getClass().getSimpleName(),
                kv("module", MODULO), kv("comprobanteId", comprobanteId));
            return false;
        }
    }

    private DatosYClave datosDe(Long comprobanteId, BigDecimal retenidoDelReembolso) {
        Comprobante comprobante = comprobanteRepository.findById(comprobanteId).orElse(null);
        if (comprobante == null || comprobante.tienePdf()) {
            return null;
        }
        Pago pago = comprobante.getPago();
        String referencia = "PAG-" + codigoCorto(pago.getCodigo());
        int horas = pago.getHoras() == null ? 0 : pago.getHoras();
        DatosComprobante datos;
        if (comprobante.getTipo() == TipoComprobante.PAGO) {
            datos = DatosComprobante.deCompra(comprobante.getNroComprobante(), pago.getEscuelaNombre(),
                pago.getEstudianteNombre(), pago.getPlanNombre(), horas, pago.getMonto(), pago.getMontoBs(),
                pago.getTipoCambio(), comprobante.getEmitidoEn(), referencia, METODO_TARJETA);
        } else {
            ReversionPago reversion = comprobante.getReversionPago();
            BigDecimal devuelto = comprobante.getMonto();
            BigDecimal retenido = retenidoDelReembolso != null ? retenidoDelReembolso
                : pago.getMonto().subtract(devuelto);
            Optional<Comprobante> original = comprobanteRepository.findByPagoIdAndTipo(pago.getId(),
                TipoComprobante.PAGO);
            datos = DatosComprobante.deReembolso(comprobante.getNroComprobante(), pago.getEscuelaNombre(),
                pago.getEstudianteNombre(), pago.getPlanNombre(), horas, pago.getMontoBs(), pago.getTipoCambio(),
                comprobante.getEmitidoEn(), referencia, METODO_TARJETA,
                original.map(Comprobante::getNroComprobante).orElse(null),
                pago.getConfirmadoEn() != null ? pago.getConfirmadoEn() : pago.getCreatedAt(), pago.getMonto(),
                porcentaje(retenido, pago.getMonto()), retenido, devuelto,
                devuelto.multiply(pago.getTipoCambio()).setScale(2, RoundingMode.HALF_UP),
                textoDeMotivo.de(reversion));
        }
        int anio = comprobante.getEmitidoEn().atZone(ZoneOffset.UTC).getYear();
        String clave = "comprobantes/" + pago.getEscuelaCodigo() + "/" + anio + "/" + comprobante.getNroComprobante()
            + ".pdf";
        return new DatosYClave(datos, clave);
    }

    private String numerar(TipoComprobante tipo, Instant ahora) {
        String prefijo = tipo == TipoComprobante.PAGO ? "CMP" : "RMB";
        int anio = ahora.atZone(ZoneOffset.UTC).getYear();
        return String.format("%s-%d-%06d", prefijo, anio, comprobanteRepository.siguienteNumero());
    }

    /** Los últimos 8 caracteres del código: la parte aleatoria del UUID v7 (el principio es la hora y se repite). */
    private static String codigoCorto(UUID codigo) {
        String texto = codigo.toString().replace("-", "").toUpperCase();
        return texto.substring(texto.length() - 8);
    }

    private static int porcentaje(BigDecimal retenido, BigDecimal original) {
        if (original == null || original.signum() == 0 || retenido == null) {
            return 0;
        }
        return retenido.multiply(BigDecimal.valueOf(100)).divide(original, 0, RoundingMode.HALF_UP).intValue();
    }

    private record DatosYClave(DatosComprobante datos, String clave) {
    }
}
