package com.drivehub.academico.features.reportes.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.drivehub.academico.common.exception.AcademicoErrorCodes;
import com.drivehub.academico.common.exception.ForbiddenException;
import com.drivehub.academico.common.response.ApiResponse;
import com.drivehub.academico.common.response.PageMeta;
import com.drivehub.academico.config.BearerActual;
import com.drivehub.academico.features.reportes.dto.ReporteClaseResponse;
import com.drivehub.academico.features.reportes.repository.FiltroReporteClases;
import com.drivehub.academico.features.reportes.service.ReporteClasesService;
import com.drivehub.academico.features.reservas.enums.EstadoReserva;

import lombok.RequiredArgsConstructor;

/**
 * Las clases de la escuela, para los reportes del Administrador (CU52). Solo lectura. La decisión por rol vive en el
 * service (@PreAuthorize) y la escuela sale del claim `escuela` del token.
 */
@RestController
@RequestMapping("/api/v1/reportes-clases")
@RequiredArgsConstructor
public class ReporteClasesController {

    private final ReporteClasesService service;
    private final BearerActual bearer;

    /** `filter[desde]` y `filter[hasta]` en `yyyy-MM-dd`, inclusivos. Un estado o una fecha mal escritos son 400. */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ReporteClaseResponse>>> listar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(name = "filter[desde]", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(name = "filter[hasta]", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(name = "filter[estado]", required = false) EstadoReserva estado,
            @RequestParam(name = "filter[instructorCodigo]", required = false) UUID instructorCodigo) {
        Page<ReporteClaseResponse> pagina = service.listar(escuelaDe(jwt), bearer.cabecera(),
            new FiltroReporteClases(desde, hasta, estado, instructorCodigo), page, pageSize);
        return ResponseEntity.ok(ApiResponse.successList(pagina.getContent(), "Clases obtenidas correctamente",
            new PageMeta(pagina.getNumber() + 1, pagina.getSize(), pagina.getTotalElements(),
                pagina.getTotalPages())));
    }

    private static UUID escuelaDe(Jwt jwt) {
        String escuela = jwt == null ? null : jwt.getClaimAsString("escuela");
        try {
            return UUID.fromString(escuela);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ForbiddenException(AcademicoErrorCodes.TOKEN_SIN_ESCUELA, "Tu cuenta no pertenece a ninguna escuela");
        }
    }
}
