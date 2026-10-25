package com.drivehub.pagos.features.billetera.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.drivehub.pagos.common.response.ApiResponse;
import com.drivehub.pagos.common.response.PageMeta;
import com.drivehub.pagos.common.util.EscuelaDelToken;
import com.drivehub.pagos.config.CurrentEscuelaId;
import com.drivehub.pagos.config.CurrentUserId;
import com.drivehub.pagos.features.billetera.dto.HoraAlumnoResponse;
import com.drivehub.pagos.features.billetera.dto.HorasAlumnoDetalleResponse;
import com.drivehub.pagos.features.billetera.service.HorasAlumnosService;

import lombok.RequiredArgsConstructor;

/**
 * Las horas de los alumnos del instructor (CU29): solo lectura. La decisión por rol vive en el service
 * (@PreAuthorize), no acá.
 *
 * * El instructor y su escuela salen del token, nunca de la URL ni del cuerpo.
 */
@RestController
@RequestMapping("/api/v1/horas-alumnos")
@RequiredArgsConstructor
public class HorasAlumnosController {

    private final HorasAlumnosService horasAlumnosService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<HoraAlumnoResponse>>> listar(
            @CurrentUserId UUID instructorCodigo,
            @CurrentEscuelaId UUID escuelaCodigo,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String search) {
        Page<HoraAlumnoResponse> pagina = horasAlumnosService.listar(EscuelaDelToken.exigir(escuelaCodigo),
            instructorCodigo, search, page, pageSize);
        return ResponseEntity.ok(ApiResponse.successList(pagina.getContent(), "Alumnos obtenidos correctamente",
            new PageMeta(pagina.getNumber() + 1, pagina.getSize(), pagina.getTotalElements(),
                pagina.getTotalPages())));
    }

    @GetMapping("/{estudianteCodigo}")
    public ResponseEntity<ApiResponse<HorasAlumnoDetalleResponse>> detalle(
            @CurrentUserId UUID instructorCodigo,
            @CurrentEscuelaId UUID escuelaCodigo,
            @PathVariable UUID estudianteCodigo) {
        return ResponseEntity.ok(ApiResponse.success(
            horasAlumnosService.detalle(EscuelaDelToken.exigir(escuelaCodigo), instructorCodigo, estudianteCodigo),
            "Alumno obtenido correctamente"));
    }
}
