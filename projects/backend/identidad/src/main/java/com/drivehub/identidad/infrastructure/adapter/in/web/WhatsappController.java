package com.drivehub.identidad.infrastructure.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.drivehub.identidad.common.response.ApiResponse;
import com.drivehub.identidad.domain.port.in.ConfirmarCodigoWhatsappComando;
import com.drivehub.identidad.domain.port.in.ConfirmarCodigoWhatsappUseCase;
import com.drivehub.identidad.domain.port.in.EmitirSesionWhatsappUseCase;
import com.drivehub.identidad.domain.port.in.SolicitarCodigoWhatsappComando;
import com.drivehub.identidad.domain.port.in.SolicitarCodigoWhatsappUseCase;

import jakarta.validation.Valid;

/**
 * El chat de WhatsApp (CU47). Rutas internas: las llama n8n con la credencial de servicio
 * (`CredencialServicioFilter`), no el navegador ni la app, y por eso no están en el gateway.
 */
@RestController
@RequestMapping("/internal/v1/whatsapp")
public class WhatsappController {

    private static final String MENSAJE_CODIGO = "Si el correo tiene una cuenta, te enviamos un código";

    private final SolicitarCodigoWhatsappUseCase solicitarCodigo;
    private final ConfirmarCodigoWhatsappUseCase confirmarCodigo;
    private final EmitirSesionWhatsappUseCase emitirSesion;

    public WhatsappController(
            SolicitarCodigoWhatsappUseCase solicitarCodigo,
            ConfirmarCodigoWhatsappUseCase confirmarCodigo,
            EmitirSesionWhatsappUseCase emitirSesion) {
        this.solicitarCodigo = solicitarCodigo;
        this.confirmarCodigo = confirmarCodigo;
        this.emitirSesion = emitirSesion;
    }

    @PostMapping("/codigos")
    public ResponseEntity<ApiResponse<Void>> pedirCodigo(@Valid @RequestBody CodigoWhatsappRequest request) {
        solicitarCodigo.solicitar(new SolicitarCodigoWhatsappComando(request.telefono(), request.correo()));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success(null, MENSAJE_CODIGO));
    }

    @PostMapping("/vinculos")
    public ResponseEntity<ApiResponse<VinculoWhatsappResponse>> vincular(
            @Valid @RequestBody VincularWhatsappRequest request) {
        confirmarCodigo.confirmar(
            new ConfirmarCodigoWhatsappComando(request.telefono(), request.correo(), request.codigo()));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(new VinculoWhatsappResponse(true), "Tu WhatsApp quedó vinculado"));
    }

    @PostMapping("/sesiones")
    public ResponseEntity<ApiResponse<SesionWhatsappResponse>> sesion(
            @Valid @RequestBody SesionWhatsappRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
            SesionWhatsappResponse.de(emitirSesion.emitir(request.telefono())), "Sesión emitida"));
    }
}
