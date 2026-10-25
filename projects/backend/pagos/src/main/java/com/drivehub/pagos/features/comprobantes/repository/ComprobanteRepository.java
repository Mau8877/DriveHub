package com.drivehub.pagos.features.comprobantes.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.drivehub.pagos.features.comprobantes.enums.TipoComprobante;
import com.drivehub.pagos.features.comprobantes.model.Comprobante;

/**
 * Acceso a datos de los comprobantes (CU30), sin lógica. El id interno Long nunca sale del servicio.
 */
public interface ComprobanteRepository extends JpaRepository<Comprobante, Long> {

    Optional<Comprobante> findByPagoIdAndTipo(Long pagoId, TipoComprobante tipo);

    Optional<Comprobante> findByCodigo(UUID codigo);

    /** Todos los comprobantes de un pago (el de compra y, si lo hay, el de reembolso), el más antiguo primero. */
    List<Comprobante> findByPagoIdOrderByIdAsc(Long pagoId);

    /**
     * El código de los comprobantes de ese tipo de varios pagos que ya tienen su PDF, en una sola consulta. Cada fila
     * es `[pagoId (Long), codigo (UUID)]`.
     */
    @Query("select c.pago.id, c.codigo from Comprobante c where c.pago.id in :pagoIds and c.tipo = :tipo"
        + " and c.urlPdf is not null")
    List<Object[]> codigosConPdfDe(@Param("pagoIds") Collection<Long> pagoIds, @Param("tipo") TipoComprobante tipo);

    /** Los comprobantes sin PDF creados antes de `limite`, los más antiguos primero. */
    @Query("select c from Comprobante c where c.urlPdf is null and c.createdAt < :limite order by c.createdAt, c.id")
    List<Comprobante> pendientesAntesDe(@Param("limite") Instant limite,
            Pageable pagina);

    /** El siguiente valor de la secuencia de números de comprobante. */
    @Query(value = "select nextval('comprobante_nro_seq')", nativeQuery = true)
    long siguienteNumero();
}
