package com.drivehub.academico.features.resultados.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.drivehub.academico.common.exception.AcademicoErrorCodes;
import com.drivehub.academico.common.exception.ForbiddenException;
import com.drivehub.academico.common.response.ApiResponse;
import com.drivehub.academico.common.response.PageMeta;
import com.drivehub.academico.config.BearerActual;
import com.drivehub.academico.config.CurrentEscuelaId;
import com.drivehub.academico.config.CurrentUserId;
import com.drivehub.academico.features.resultados.dto.ResultadoResponse;
import com.drivehub.academico.features.resultados.enums.OrigenResultado;
import com.drivehub.academico.features.resultados.repository.FiltroResultados;
import com.drivehub.academico.features.resultados.service.ResultadoService;

import lombok.RequiredArgsConstructor;

/**
 * Consulta de resultados y notas (CU23). La decisión por rol vive en el service (@PreAuthorize).
 *
 * * `/mios` es del estudiante (sus notas); `/` es del Administrador de escuela (las de su escuela). La persona y la
 * * escuela salen del token, nunca de la URL.
 */
@RestController
@RequestMapping("/api/v1/resultados")
@RequiredArgsConstructor
public class ResultadoController {

    private final ResultadoService resultadoService;
    private final BearerActual bearer;

    @GetMapping("/mios")
    public ResponseEntity<ApiResponse<List<ResultadoResponse>>> mios(
            @CurrentUserId UUID estudianteCodigo,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(name = "filter[origen]", required = false) OrigenResultado origen) {
        return respuesta(resultadoService.listarMios(estudianteCodigo, new FiltroResultados(origen, null), page, pageSize));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ResultadoResponse>>> deLaEscuela(
            @CurrentEscuelaId UUID escuelaCodigo,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(name = "filter[origen]", required = false) OrigenResultado origen,
            @RequestParam(name = "filter[estudianteCodigo]", required = false) UUID estudianteCodigo) {
        if (escuelaCodigo == null) {
            throw new ForbiddenException(AcademicoErrorCodes.TOKEN_SIN_ESCUELA, "Tu cuenta no pertenece a ninguna escuela");
        }
        return respuesta(resultadoService.listarDeEscuela(escuelaCodigo, bearer.cabecera(),
            new FiltroResultados(origen, estudianteCodigo), page, pageSize));
    }

    private static ResponseEntity<ApiResponse<List<ResultadoResponse>>> respuesta(Page<ResultadoResponse> pagina) {
        return ResponseEntity.ok(ApiResponse.successList(pagina.getContent(), "Resultados obtenidos correctamente",
            new PageMeta(pagina.getNumber() + 1, pagina.getSize(), pagina.getTotalElements(),
                pagina.getTotalPages())));
    }
}
