package com.drivehub.academico.features.reservas.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.drivehub.academico.common.response.ApiResponse;
import com.drivehub.academico.common.response.PageMeta;
import com.drivehub.academico.config.BearerActual;
import com.drivehub.academico.config.CurrentEscuelaId;
import com.drivehub.academico.config.CurrentUserId;
import com.drivehub.academico.features.reservas.dto.ReservaResponse;
import com.drivehub.academico.features.reservas.dto.ReservarFranjasRequest;
import com.drivehub.academico.features.reservas.dto.ReservasResultadoResponse;
import com.drivehub.academico.features.reservas.service.ReservaService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Las reservas del estudiante (CU14). La decisión por rol vive en el service (@PreAuthorize), no acá.
 *
 * * El estudiante y su escuela salen del token, nunca del cuerpo ni de la URL.
 * * Confirmar es parcial: responde 200 con las franjas reservadas y las que no, cada una con su motivo, aunque
 * * ninguna haya quedado reservada. Los errores de la petición completa (validación, sesión, rol) siguen el
 * * contrato de siempre.
 */
@RestController
@RequestMapping("/api/v1/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaService service;
    private final BearerActual bearer;

    /**
     * `filter[vista]`: `proximas` (por defecto) o `historial`. Cualquier otro valor es `400 ERR_SYS_01`.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ReservaResponse>>> listar(
            @CurrentUserId UUID estudianteCodigo,
            @RequestParam(name = "filter[vista]", required = false) String vista,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        Page<ReservaResponse> pagina = service.listar(estudianteCodigo, bearer.cabecera(), vista, page, pageSize);
        return ResponseEntity.ok(ApiResponse.successList(pagina.getContent(), "Clases obtenidas correctamente",
            new PageMeta(pagina.getNumber() + 1, pagina.getSize(), pagina.getTotalElements(),
                pagina.getTotalPages())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReservasResultadoResponse>> reservar(
            @CurrentUserId UUID estudianteCodigo,
            @CurrentEscuelaId UUID escuelaCodigo,
            @Valid @RequestBody ReservarFranjasRequest request) {
        List<UUID> franjaCodigos = request.franjaCodigos();
        return ResponseEntity.ok(ApiResponse.success(
            service.reservar(estudianteCodigo, escuelaCodigo, bearer.cabecera(), franjaCodigos),
            "Reservas procesadas correctamente"));
    }
}
