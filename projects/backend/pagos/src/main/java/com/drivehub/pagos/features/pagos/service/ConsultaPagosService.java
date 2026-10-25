package com.drivehub.pagos.features.pagos.service;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.pagos.common.exception.NotFoundException;
import com.drivehub.pagos.common.exception.PagosErrorCodes;
import com.drivehub.pagos.features.pagos.dto.PagoDetalleResponse;
import com.drivehub.pagos.features.pagos.dto.PagoResumenResponse;
import com.drivehub.pagos.features.pagos.enums.ConceptoPago;
import com.drivehub.pagos.features.pagos.enums.EstadoPago;
import com.drivehub.pagos.features.pagos.enums.EstadoPagoVisible;
import com.drivehub.pagos.features.pagos.enums.EstadoReversion;
import com.drivehub.pagos.features.pagos.model.Pago;
import com.drivehub.pagos.features.pagos.repository.PagoRepository;
import com.drivehub.pagos.features.pagos.repository.PagoSpecification;
import com.drivehub.pagos.features.pagos.repository.ReversionPagoRepository;

import lombok.RequiredArgsConstructor;

/**
 * El historial de compras del estudiante (CU28): listado y detalle, con el reembolso previsto.
 *
 * * El estudiante y su escuela salen del token (los pasa el controller): un pago de otra persona responde "no
 * * existe", no "prohibido", para no revelar que existe.
 * * Solo se listan pagos del concepto `HORAS`; los de reserva de clase (CU44) tendrán su propia vista.
 * * `reembolsable` lo decide el servidor: pago confirmado, ninguna hora usada y sin reversión vigente. CU31
 * * vuelve a comprobarlo al ejecutar.
 * * Son lecturas: no se loguean.
 */
@Service
@RequiredArgsConstructor
public class ConsultaPagosService {

    static final int PAGE_SIZE_POR_DEFECTO = 20;
    static final int PAGE_SIZE_MAXIMO = 100;

    private static final Sort MAS_RECIENTE_PRIMERO = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final PagoRepository pagoRepository;
    private final ReversionPagoRepository reversionRepository;
    private final ReembolsoCalculadora calculadora;

    /**
     * Sin `page`, la 1; sin `pageSize`, 20 y nunca más de 100. Del más reciente al más antiguo. `estado` nulo
     * trae todos.
     */
    @PreAuthorize("hasRole('ESTUDIANTE')")
    @Transactional(readOnly = true)
    public Page<PagoResumenResponse> listar(UUID escuelaCodigo, UUID estudianteCodigo, Integer page,
            Integer pageSize, EstadoPagoVisible estado) {
        int pagina = page == null || page < 1 ? 1 : page;
        int tamano = pageSize == null ? PAGE_SIZE_POR_DEFECTO : Math.min(Math.max(pageSize, 1), PAGE_SIZE_MAXIMO);
        Specification<Pago> filtro = Specification.allOf(PagoSpecification.deEstudiante(escuelaCodigo,
            estudianteCodigo), PagoSpecification.deConcepto(ConceptoPago.HORAS),
            PagoSpecification.conEstadoVisible(estado));
        Page<Pago> encontrados = pagoRepository.findAll(filtro,
            PageRequest.of(pagina - 1, tamano, MAS_RECIENTE_PRIMERO));

        Set<Long> conReembolsoSolicitado = conReversionSolicitada(encontrados.getContent());
        return encontrados.map(pago -> aResumen(pago, visible(pago, conReembolsoSolicitado.contains(pago.getId()))));
    }

    @PreAuthorize("hasRole('ESTUDIANTE')")
    @Transactional(readOnly = true)
    public PagoDetalleResponse detalle(UUID escuelaCodigo, UUID estudianteCodigo, UUID codigo) {
        Pago pago = pagoRepository.findByCodigoAndEstudianteCodigoAndEscuelaCodigo(codigo, estudianteCodigo,
            escuelaCodigo).orElseThrow(() -> new NotFoundException(PagosErrorCodes.PAGO_NO_ENCONTRADO,
            "El pago no existe"));

        boolean confirmado = pago.getEstado() == EstadoPago.CONFIRMADO;
        boolean solicitada = confirmado
            && reversionRepository.existsByPagoIdAndEstado(pago.getId(), EstadoReversion.SOLICITADA);
        boolean reembolsable = confirmado && !solicitada
            && pago.getHoras() != null && pago.getHoras().equals(pago.getHorasRestantes())
            && !reversionRepository.existsByPagoIdAndEstado(pago.getId(), EstadoReversion.EJECUTADA);
        return new PagoDetalleResponse(pago.getCodigo(), pago.getPlanNombre(), pago.getHoras(), pago.getMonto(),
            pago.getMontoBs(), visible(pago, solicitada), pago.getCreatedAt(), pago.getHorasRestantes(),
            pago.getConfirmadoEn(), reembolsable, reembolsable ? calculadora.calcular(pago.getMonto()) : null);
    }

    /** Los ids de los pagos confirmados de la página que tienen una reversión solicitada (una sola consulta). */
    private Set<Long> conReversionSolicitada(Collection<Pago> pagos) {
        List<Long> confirmados = pagos.stream().filter(p -> p.getEstado() == EstadoPago.CONFIRMADO)
            .map(Pago::getId).toList();
        if (confirmados.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(reversionRepository.pagoIdsConReversion(confirmados, EstadoReversion.SOLICITADA));
    }

    private static EstadoPagoVisible visible(Pago pago, boolean reversionSolicitada) {
        return EstadoPagoVisible.de(pago.getEstado(), reversionSolicitada);
    }

    private static PagoResumenResponse aResumen(Pago pago, EstadoPagoVisible estado) {
        return new PagoResumenResponse(pago.getCodigo(), pago.getPlanNombre(), pago.getHoras(), pago.getMonto(),
            pago.getMontoBs(), estado, pago.getCreatedAt());
    }
}
