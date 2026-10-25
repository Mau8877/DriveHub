package com.drivehub.pagos.features.billetera.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.text.Normalizer;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.drivehub.pagos.common.exception.NotFoundException;
import com.drivehub.pagos.common.exception.PagosErrorCodes;
import com.drivehub.pagos.config.BearerActual;
import com.drivehub.pagos.features.billetera.dto.ConsumoClaseResponse;
import com.drivehub.pagos.features.billetera.dto.HoraAlumnoResponse;
import com.drivehub.pagos.features.billetera.dto.HorasAlumnoDetalleResponse;
import com.drivehub.pagos.features.billetera.dto.PagoAlumnoResponse;
import com.drivehub.pagos.features.billetera.repository.BilleteraRepository;
import com.drivehub.pagos.features.billetera.repository.MovimientoHorasRepository;
import com.drivehub.pagos.features.comprobantes.enums.TipoComprobante;
import com.drivehub.pagos.features.comprobantes.repository.ComprobanteRepository;
import com.drivehub.pagos.features.pagos.client.EscuelaClient;
import com.drivehub.pagos.features.pagos.client.IdentidadClient;
import com.drivehub.pagos.features.pagos.client.IdentidadClient.NombreEstudiante;
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
 * Las horas de los alumnos de un instructor (CU29): listado y detalle.
 *
 * * Tres servicios, una pantalla: Escuela dice quiénes son sus alumnos, Identidad cómo se llaman y Pagos
 * * cuántas horas tienen. Pagos pide a los otros dos con el Bearer del instructor y NO guarda ni nombres ni la
 * * lista de alumnos.
 * * Para buscar por nombre hace falta tener a todos los alumnos con su nombre (hasta el tope de 500 de
 * * `EscuelaClient`): se ordena, se busca y se pagina en memoria, y los tres totales se calculan solo para los
 * * alumnos de la página.
 * * Las horas gastadas son SOLO las del instructor del token; las compradas y las restantes son del alumno.
 * * No se abre una transacción alrededor de todo el método: se esperaría a otros servicios con una conexión de
 * * la base ocupada. Cada consulta abre la suya.
 * * Son lecturas: no se loguean (salvo el aviso de un alumno que Identidad no devuelve).
 */
@Service
@RequiredArgsConstructor
public class HorasAlumnosService {

    static final int PAGE_SIZE_POR_DEFECTO = 20;
    static final int PAGE_SIZE_MAXIMO = 100;
    static final int PAGOS_EN_EL_DETALLE = 20;

    private static final Logger LOG = LoggerFactory.getLogger(HorasAlumnosService.class);
    private static final Sort MAS_RECIENTE_PRIMERO = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final EscuelaClient escuelaClient;
    private final IdentidadClient identidadClient;
    private final BearerActual bearerActual;
    private final PagoRepository pagoRepository;
    private final BilleteraRepository billeteraRepository;
    private final MovimientoHorasRepository movimientoRepository;
    private final ReversionPagoRepository reversionRepository;
    private final ComprobanteRepository comprobanteRepository;

    /**
     * Sin `page`, la 1; sin `pageSize`, 20 y nunca más de 100. Ordenado por apellidos y nombres. `search` (en
     * blanco no filtra) busca el texto dentro de `nombres apellidos`, sin distinguir mayúsculas ni tildes.
     */
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public Page<HoraAlumnoResponse> listar(UUID escuelaCodigo, UUID instructorCodigo, String search, Integer page,
            Integer pageSize) {
        int pagina = page == null || page < 1 ? 1 : page;
        int tamano = pageSize == null ? PAGE_SIZE_POR_DEFECTO : Math.min(Math.max(pageSize, 1), PAGE_SIZE_MAXIMO);

        String bearer = bearerActual.cabecera();
        List<UUID> alumnos = escuelaClient.alumnosDelInstructor(bearer);
        Map<UUID, NombreEstudiante> nombres = alumnos.isEmpty() ? Map.of()
            : identidadClient.nombresDe(alumnos, bearer);
        if (nombres.size() < alumnos.size()) {
            LOG.warn("Identidad no devolvió a todos los alumnos de un instructor",
                kv("module", "horas-alumnos"), kv("instructorCodigo", instructorCodigo));
        }

        String buscado = search == null || search.isBlank() ? null : normalizar(search.trim());
        List<UUID> ordenados = nombres.entrySet().stream()
            .filter(e -> buscado == null || normalizar(e.getValue().completo()).contains(buscado))
            .sorted(Comparator
                .comparing((Map.Entry<UUID, NombreEstudiante> e) -> normalizar(e.getValue().apellidos()))
                .thenComparing(e -> normalizar(e.getValue().nombres()))
                .thenComparing(Map.Entry::getKey))
            .map(Map.Entry::getKey).toList();

        int desde = (int) Math.min((long) (pagina - 1) * tamano, ordenados.size());
        List<UUID> deLaPagina = ordenados.subList(desde, Math.min(desde + tamano, ordenados.size()));

        Map<UUID, NombreEstudiante> nombresDeLaPagina = deLaPagina.stream()
            .collect(Collectors.toMap(Function.identity(), nombres::get));
        List<HoraAlumnoResponse> filas = resumenes(escuelaCodigo, instructorCodigo, deLaPagina, nombresDeLaPagina);
        return new PageImpl<>(filas, PageRequest.of(pagina - 1, tamano), ordenados.size());
    }

    @PreAuthorize("hasRole('INSTRUCTOR')")
    public HorasAlumnoDetalleResponse detalle(UUID escuelaCodigo, UUID instructorCodigo, UUID estudianteCodigo) {
        String bearer = bearerActual.cabecera();
        if (!escuelaClient.esMiAlumno(estudianteCodigo, bearer)) {
            throw alumnoNoAsignado();
        }
        NombreEstudiante nombre = identidadClient.nombresDe(List.of(estudianteCodigo), bearer).get(estudianteCodigo);
        if (nombre == null) {
            throw alumnoNoAsignado();
        }

        HoraAlumnoResponse resumen = resumenes(escuelaCodigo, instructorCodigo, List.of(estudianteCodigo),
            Map.of(estudianteCodigo, nombre)).get(0);
        List<ConsumoClaseResponse> consumos = movimientoRepository
            .consumosPorClase(escuelaCodigo, instructorCodigo, estudianteCodigo).stream()
            .map(fila -> new ConsumoClaseResponse((UUID) fila[0], (Instant) fila[1], ((Number) fila[2]).intValue()))
            .toList();
        return new HorasAlumnoDetalleResponse(resumen, consumos, pagosDe(escuelaCodigo, estudianteCodigo));
    }

    /**
     * Arma, en el orden dado, el resumen de cada alumno con TRES consultas para todos (compradas, gastadas con
     * este instructor y disponibles). Un alumno sin compras ni billetera queda con ceros.
     */
    private List<HoraAlumnoResponse> resumenes(UUID escuelaCodigo, UUID instructorCodigo, List<UUID> alumnos,
            Map<UUID, NombreEstudiante> nombres) {
        if (alumnos.isEmpty()) {
            return List.of();
        }
        Map<UUID, Integer> compradas = comoMapa(pagoRepository.horasCompradasPor(escuelaCodigo, alumnos));
        Map<UUID, Integer> gastadas = comoMapa(
            movimientoRepository.horasGastadasConInstructor(escuelaCodigo, instructorCodigo, alumnos));
        Map<UUID, Integer> restantes = comoMapa(billeteraRepository.horasDisponiblesDe(escuelaCodigo, alumnos));

        return alumnos.stream().map(codigo -> new HoraAlumnoResponse(codigo, nombres.get(codigo).nombres(),
            nombres.get(codigo).apellidos(), compradas.getOrDefault(codigo, 0),
            Math.max(0, gastadas.getOrDefault(codigo, 0)), restantes.getOrDefault(codigo, 0))).toList();
    }

    private List<PagoAlumnoResponse> pagosDe(UUID escuelaCodigo, UUID estudianteCodigo) {
        Specification<Pago> filtro = Specification.allOf(PagoSpecification.deEstudiante(escuelaCodigo,
            estudianteCodigo), PagoSpecification.deConcepto(ConceptoPago.HORAS));
        List<Pago> pagos = pagoRepository
            .findAll(filtro, PageRequest.of(0, PAGOS_EN_EL_DETALLE, MAS_RECIENTE_PRIMERO)).getContent();

        List<Long> confirmados = pagos.stream().filter(p -> p.getEstado() == EstadoPago.CONFIRMADO)
            .map(Pago::getId).toList();
        Set<Long> conReembolsoSolicitado = confirmados.isEmpty() ? Set.of()
            : Set.copyOf(reversionRepository.pagoIdsConReversion(confirmados, EstadoReversion.SOLICITADA));
        // * El comprobante de la COMPRA de cada pago, solo si su PDF ya está listo (el instructor no ve el de reembolso)
        Map<Long, UUID> comprobantes = pagos.isEmpty() ? Map.of()
            : comprobanteRepository.codigosConPdfDe(pagos.stream().map(Pago::getId).toList(),
                TipoComprobante.PAGO).stream()
                .collect(Collectors.toMap(fila -> (Long) fila[0], fila -> (UUID) fila[1], (a, b) -> a));
        return pagos.stream().map(p -> new PagoAlumnoResponse(p.getCodigo(), p.getPlanNombre(), p.getHoras(),
            EstadoPagoVisible.de(p.getEstado(), conReembolsoSolicitado.contains(p.getId())), p.getCreatedAt(),
            comprobantes.get(p.getId()))).toList();
    }

    /** Filas `[UUID, Number]` a un mapa código → entero. */
    private static Map<UUID, Integer> comoMapa(List<Object[]> filas) {
        Map<UUID, Integer> mapa = new HashMap<>();
        for (Object[] fila : filas) {
            mapa.put((UUID) fila[0], ((Number) fila[1]).intValue());
        }
        return mapa;
    }

    /** Minúsculas y sin tildes: "José" y "jose" son lo mismo para ordenar y buscar. */
    private static String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT);
    }

    private static NotFoundException alumnoNoAsignado() {
        return new NotFoundException(PagosErrorCodes.ALUMNO_NO_ASIGNADO, "El alumno no existe o no es tuyo");
    }
}
