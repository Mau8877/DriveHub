package com.drivehub.identidad.infrastructure.adapter.in.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.drivehub.identidad.common.response.ApiResponse;
import com.drivehub.identidad.domain.port.in.ConsultarEstudiantesUseCase;
import com.drivehub.identidad.infrastructure.config.CurrentUserId;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * El nombre de un conjunto de estudiantes (CU29). Lo llama Pagos con el token del instructor.
 *
 * * Quien consulta sale SIEMPRE de la sesión (`@CurrentUserId`); de su cuenta se toma la escuela.
 * * La decisión por rol vive en el caso de uso (@PreAuthorize), no acá.
 */
@RestController
@RequestMapping("/api/v1/estudiantes/consulta")
@RequiredArgsConstructor
public class EstudianteConsultaController {

    private final ConsultarEstudiantesUseCase consultarEstudiantes;

    @PostMapping
    public ResponseEntity<ApiResponse<List<PersonaEstudianteResponse>>> consultar(
            @CurrentUserId UUID solicitanteCodigo, @Valid @RequestBody ConsultarEstudiantesRequest request) {
        List<PersonaEstudianteResponse> personas = consultarEstudiantes
            .consultar(solicitanteCodigo, request.codigos()).stream().map(PersonaEstudianteResponse::de).toList();
        return ResponseEntity.ok(ApiResponse.success(personas, "Estudiantes obtenidos correctamente"));
    }
}
