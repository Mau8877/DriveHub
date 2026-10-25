package com.drivehub.pagos.features.comprobantes.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.util.NoSuchElementException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.pagos.common.exception.ConflictException;
import com.drivehub.pagos.common.exception.NotFoundException;
import com.drivehub.pagos.common.exception.PagosErrorCodes;
import com.drivehub.pagos.common.exception.ServicioNoDisponibleException;
import com.drivehub.pagos.config.BearerActual;
import com.drivehub.pagos.features.comprobantes.almacen.AlmacenComprobantes;
import com.drivehub.pagos.features.comprobantes.model.Comprobante;
import com.drivehub.pagos.features.comprobantes.repository.ComprobanteRepository;
import com.drivehub.pagos.features.pagos.client.EscuelaClient;
import com.drivehub.pagos.features.pagos.model.Pago;

import lombok.RequiredArgsConstructor;

/**
 * Entrega el PDF de un comprobante (CU30) a quien tiene derecho a verlo.
 *
 * * Lo ve el estudiante dueño del pago y el instructor al que ese estudiante está asignado (se pregunta a Escuela
 * * con el token del instructor); es el mismo documento para ambos. Nadie más.
 * * No se distingue "no existe" de "no es tuyo" (de otra persona, de otra escuela, inexistente): `ERR_PAG_13`.
 * * Un comprobante creado cuyo PDF todavía se prepara responde `ERR_PAG_14`.
 * * Sin log por lectura. El PDF nunca sale por una dirección pública: solo por este camino.
 */
@Service
@RequiredArgsConstructor
public class ComprobanteDescargaService {

    private static final Logger LOG = LoggerFactory.getLogger(ComprobanteDescargaService.class);

    private final ComprobanteRepository comprobanteRepository;
    private final AlmacenComprobantes almacen;
    private final EscuelaClient escuelaClient;
    private final BearerActual bearerActual;

    /** El PDF y el número del comprobante (para el nombre del archivo). */
    public record Pdf(String numero, byte[] bytes) {
    }

    /**
     * @param personaCodigo quien pide, del token
     * @param esInstructor si quien pide tiene el rol de instructor
     */
    @PreAuthorize("hasAnyRole('ESTUDIANTE', 'INSTRUCTOR')")
    @Transactional(readOnly = true)
    public Pdf obtener(UUID escuelaCodigo, UUID personaCodigo, boolean esInstructor, UUID comprobanteCodigo) {
        Comprobante comprobante = comprobanteRepository.findByCodigo(comprobanteCodigo)
            .orElseThrow(ComprobanteDescargaService::noEncontrado);
        Pago pago = comprobante.getPago();
        if (!pago.getEscuelaCodigo().equals(escuelaCodigo) || !puedeVerlo(pago, personaCodigo, esInstructor)) {
            throw noEncontrado();
        }
        if (!comprobante.tienePdf()) {
            throw new ConflictException(PagosErrorCodes.COMPROBANTE_EN_PREPARACION,
                "Tu comprobante todavía se está preparando. Inténtalo en unos minutos.");
        }
        try {
            return new Pdf(comprobante.getNroComprobante(), almacen.leer(comprobante.getUrlPdf()));
        } catch (NoSuchElementException e) {
            // * El comprobante dice tener PDF pero el almacén no lo encuentra: es un problema del almacén, no del cliente
            LOG.error("El PDF de un comprobante no está en el almacén", kv("module", "comprobantes"),
                kv("comprobanteCodigo", comprobanteCodigo));
            throw new ServicioNoDisponibleException(PagosErrorCodes.SERVICIO_DEPENDIENTE_NO_DISPONIBLE,
                "No pudimos leer el comprobante. Inténtalo de nuevo más tarde.");
        }
    }

    private boolean puedeVerlo(Pago pago, UUID personaCodigo, boolean esInstructor) {
        if (pago.getEstudianteCodigo().equals(personaCodigo)) {
            return true;
        }
        return esInstructor && escuelaClient.esMiAlumno(pago.getEstudianteCodigo(), bearerActual.cabecera());
    }

    private static NotFoundException noEncontrado() {
        return new NotFoundException(PagosErrorCodes.COMPROBANTE_NO_ENCONTRADO, "El comprobante no existe");
    }
}
