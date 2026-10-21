package com.drivehub.identidad.infrastructure.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.drivehub.identidad.common.response.ApiResponse;
import com.drivehub.identidad.domain.port.in.RestablecerContrasenaUseCase;
import com.drivehub.identidad.domain.port.in.SolicitarRecuperacionUseCase;

import jakarta.validation.Valid;

/**
 * Recuperación de contraseña (CU04). Ambos endpoints son públicos.
 *
 * * `/recuperar` responde siempre lo mismo: no revela qué correos existen ni cuáles
 * * están verificados.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class RecuperacionContrasenaController {

    private final SolicitarRecuperacionUseCase solicitarRecuperacion;
    private final RestablecerContrasenaUseCase restablecerContrasena;

    public RecuperacionContrasenaController(
            SolicitarRecuperacionUseCase solicitarRecuperacion,
            RestablecerContrasenaUseCase restablecerContrasena) {
        this.solicitarRecuperacion = solicitarRecuperacion;
        this.restablecerContrasena = restablecerContrasena;
    }

    @PostMapping("/recuperar")
    public ResponseEntity<ApiResponse<Void>> recuperar(@Valid @RequestBody RecuperarRequest request) {
        solicitarRecuperacion.solicitar(request.email());
        return ResponseEntity.ok(ApiResponse.success(null,
            "Si el correo está registrado y verificado, te enviamos un enlace"));
    }

    @PostMapping("/restablecer")
    public ResponseEntity<ApiResponse<Void>> restablecer(@Valid @RequestBody RestablecerRequest request) {
        restablecerContrasena.restablecer(request.token(), request.password());
        return ResponseEntity.ok(ApiResponse.success(null, "Contraseña actualizada correctamente"));
    }
}
