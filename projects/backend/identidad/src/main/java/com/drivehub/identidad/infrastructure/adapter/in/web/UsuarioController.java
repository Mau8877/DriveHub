package com.drivehub.identidad.infrastructure.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.drivehub.identidad.common.response.ApiResponse;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.in.RegistrarEstudianteComando;
import com.drivehub.identidad.domain.port.in.RegistrarEstudianteUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Recurso `usuarios`. Por ahora solo el registro del estudiante (CU01), que
 * es público.
 */
@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final RegistrarEstudianteUseCase registrarEstudiante;

    @PostMapping
    public ResponseEntity<ApiResponse<UsuarioRegistradoResponse>> registrar(
            @Valid @RequestBody RegistrarEstudianteRequest request) {
        Usuario usuario = registrarEstudiante.registrar(new RegistrarEstudianteComando(
            request.nombres(), request.apellidos(), request.fechaNacimiento(), request.email(),
            request.password(), request.telefono(), request.escuelaSlug()));

        UsuarioRegistradoResponse response = new UsuarioRegistradoResponse(
            usuario.getCodigo(), usuario.getEmail(), usuario.getNombres(), usuario.getApellidos());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, "Usuario registrado correctamente"));
    }
}
