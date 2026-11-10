package com.drivehub.pagos.features.pagos.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.pagos.common.exception.PagosErrorCodes;
import com.drivehub.pagos.common.exception.ValidationException;
import com.drivehub.pagos.features.pagos.dto.ReportePagoResponse;
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
 * Las compras de horas de toda la escuela, para el reporte del Administrador (CU52). Solo lectura.
 *
 * * Las fechas del filtro son días de Bolivia: `hasta` incluye todo ese día.
 * * Páginas más grandes que las de una pantalla ({@value #TAMANO_MAXIMO}): el reporte las recorre todas.
 */
@Service
@RequiredArgsConstructor
public class ReportePagosService {

    static final int TAMANO_POR_DEFECTO = 100;
    static final int TAMANO_MAXIMO = 500;
    static final ZoneId ZONA = ZoneId.of("America/La_Paz");
    private static final String MODULO = "pagos";
    private static final Sort MAS_RECIENTE_PRIMERO = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
    private static final Logger LOG = LoggerFactory.getLogger(ReportePagosService.class);

    private final PagoRepository pagoRepository;
    private final ReversionPagoRepository reversionRepository;

    @PreAuthorize("hasRole('ADMIN_ESCUELA')")
    @Transactional(readOnly = true)
    public Page<ReportePagoResponse> listar(UUID escuelaCodigo, LocalDate desde, LocalDate hasta,
            EstadoPagoVisible estado, Integer page, Integer pageSize) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            LOG.warn("Reporte de pagos rechazado: rango de fechas invertido", kv("module", MODULO),
                kv("errorCode", PagosErrorCodes.RANGO_DE_FECHAS_INVALIDO));
            throw new ValidationException(PagosErrorCodes.RANGO_DE_FECHAS_INVALIDO,
                "La fecha inicial no puede ser posterior a la final");
        }
        int pagina = page == null || page < 1 ? 1 : page;
        int tamano = pageSize == null ? TAMANO_POR_DEFECTO : Math.min(Math.max(pageSize, 1), TAMANO_MAXIMO);
        Instant inicio = desde == null ? null : desde.atStartOfDay(ZONA).toInstant();
        Instant fin = hasta == null ? null : hasta.plusDays(1).atStartOfDay(ZONA).toInstant();

        Specification<Pago> filtro = Specification.allOf(PagoSpecification.deEscuela(escuelaCodigo),
            PagoSpecification.deConcepto(ConceptoPago.HORAS), PagoSpecification.creadosEntre(inicio, fin),
            PagoSpecification.conEstadoVisible(estado));
        Page<Pago> encontrados = pagoRepository.findAll(filtro,
            PageRequest.of(pagina - 1, tamano, MAS_RECIENTE_PRIMERO));

        Set<Long> conReembolso = conReversionSolicitada(encontrados.getContent());
        return encontrados.map(p -> aRespuesta(p,
            EstadoPagoVisible.de(p.getEstado(), conReembolso.contains(p.getId()))));
    }

    private Set<Long> conReversionSolicitada(List<Pago> pagos) {
        List<Long> confirmados = pagos.stream().filter(p -> p.getEstado() == EstadoPago.CONFIRMADO)
            .map(Pago::getId).toList();
        if (confirmados.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(reversionRepository.pagoIdsConReversion(confirmados, EstadoReversion.SOLICITADA));
    }

    private static ReportePagoResponse aRespuesta(Pago p, EstadoPagoVisible estado) {
        return new ReportePagoResponse(p.getCodigo(), p.getCreatedAt(), p.getConfirmadoEn(), p.getEstudianteCodigo(),
            p.getEstudianteNombre(), p.getPlanNombre(), p.getHoras(), p.getHorasRestantes(), p.getMonto(),
            p.getMontoBs(), estado);
    }
}
