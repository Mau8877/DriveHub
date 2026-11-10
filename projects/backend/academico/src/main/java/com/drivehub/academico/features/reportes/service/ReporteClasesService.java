package com.drivehub.academico.features.reportes.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.drivehub.academico.common.exception.AcademicoErrorCodes;
import com.drivehub.academico.common.exception.ServicioNoDisponibleException;
import com.drivehub.academico.common.exception.ValidationException;
import com.drivehub.academico.features.reportes.dto.ReporteClaseResponse;
import com.drivehub.academico.features.reportes.repository.FiltroReporteClases;
import com.drivehub.academico.features.reportes.repository.ReporteClasesRepository;
import com.drivehub.academico.features.reservas.client.IdentidadClient;
import com.drivehub.academico.features.reservas.model.Reserva;

import lombok.RequiredArgsConstructor;

/**
 * Las clases de la escuela para el reporte del Administrador (CU52). Solo lectura: no cambia ninguna reserva.
 *
 * * Las páginas son más grandes que las de una pantalla ({@value #TAMANO_MAXIMO}): el reporte las recorre todas.
 * * Los nombres son un agregado: si Identidad no responde, el reporte sale igual con los nombres vacíos.
 */
@Service
@RequiredArgsConstructor
public class ReporteClasesService {

    static final int TAMANO_POR_DEFECTO = 100;
    static final int TAMANO_MAXIMO = 500;
    private static final String MODULO = "reportes";
    private static final Logger LOG = LoggerFactory.getLogger(ReporteClasesService.class);

    private final ReporteClasesRepository repository;
    private final IdentidadClient identidadClient;

    @PreAuthorize("hasRole('ADMIN_ESCUELA')")
    public Page<ReporteClaseResponse> listar(UUID escuelaCodigo, String bearer, FiltroReporteClases filtro,
            Integer pagina, Integer tamano) {
        if (filtro.desde() != null && filtro.hasta() != null && filtro.desde().isAfter(filtro.hasta())) {
            LOG.warn("Reporte de clases rechazado: rango de fechas invertido", kv("module", MODULO),
                kv("errorCode", AcademicoErrorCodes.RANGO_DE_FECHAS_INVALIDO));
            throw new ValidationException(AcademicoErrorCodes.RANGO_DE_FECHAS_INVALIDO,
                "La fecha inicial no puede ser posterior a la final");
        }
        int numero = pagina == null || pagina < 1 ? 1 : pagina;
        int cantidad = tamano == null || tamano < 1 ? TAMANO_POR_DEFECTO : Math.min(tamano, TAMANO_MAXIMO);
        Page<Reserva> clases = repository.listar(escuelaCodigo, filtro, PageRequest.of(numero - 1, cantidad));
        if (clases.isEmpty()) {
            return clases.map(r -> aRespuesta(r, Map.of(), Map.of()));
        }

        List<UUID> estudiantes = clases.getContent().stream().map(Reserva::getEstudianteCodigo).toList();
        List<UUID> instructores = clases.getContent().stream().map(Reserva::getInstructorCodigo).toList();
        Map<UUID, String> nombresEstudiantes = nombres(identidadClient::nombresDeEstudiantes, estudiantes, bearer);
        Map<UUID, String> nombresInstructores = nombres(identidadClient::nombresDeInstructores, instructores, bearer);
        return clases.map(r -> aRespuesta(r, nombresEstudiantes, nombresInstructores));
    }

    private Map<UUID, String> nombres(BiFunction<List<UUID>, String, Map<UUID, String>> consulta, List<UUID> codigos,
            String bearer) {
        try {
            return consulta.apply(codigos, bearer);
        } catch (ServicioNoDisponibleException e) {
            LOG.warn("Reporte de clases sin nombres: Identidad no respondió", kv("module", MODULO),
                kv("errorCode", e.getErrorCode()));
            return Map.of();
        }
    }

    private static ReporteClaseResponse aRespuesta(Reserva r, Map<UUID, String> estudiantes,
            Map<UUID, String> instructores) {
        return new ReporteClaseResponse(r.getCodigoReserva(), r.getFecha(), r.getHoraInicio(), r.getHoraFin(),
            r.getEstado(), r.getEstudianteCodigo(), estudiantes.get(r.getEstudianteCodigo()), r.getInstructorCodigo(),
            instructores.get(r.getInstructorCodigo()), r.getHorasCobradas(), r.isCanceladaPorInstructor());
    }
}
