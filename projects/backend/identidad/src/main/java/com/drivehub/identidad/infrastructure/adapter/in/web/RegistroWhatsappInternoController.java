package com.drivehub.identidad.infrastructure.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.drivehub.identidad.common.response.ApiResponse;
import com.drivehub.identidad.domain.port.in.CrearSolicitudRegistroWhatsappComando;
import com.drivehub.identidad.domain.port.in.CrearSolicitudRegistroWhatsappUseCase;

import jakarta.validation.Valid;

/**
 * El registro por WhatsApp (CU48), la parte que llama n8n: validar un dato y guardar la solicitud. Rutas internas,
 * con la credencial de servicio y fuera del gateway. La contraseña NO pasa por acá.
 */
@RestController
@RequestMapping("/internal/v1/whatsapp/registros")
public class RegistroWhatsappInternoController {

    private final ValidadorDatoRegistro validador;
    private final CrearSolicitudRegistroWhatsappUseCase crearSolicitud;

    public RegistroWhatsappInternoController(
            ValidadorDatoRegistro validador, CrearSolicitudRegistroWhatsappUseCase crearSolicitud) {
        this.validador = validador;
        this.crearSolicitud = crearSolicitud;
    }

    @PostMapping("/validaciones")
    public ResponseEntity<ApiResponse<Void>> validar(@Valid @RequestBody ValidarDatoRegistroRequest request) {
        validador.validar(request.campo(), request.valor());
        return ResponseEntity.ok(ApiResponse.success(null, "El dato es válido"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EnlaceRegistroResponse>> crear(
            @Valid @RequestBody CrearRegistroWhatsappRequest request) {
        var enlace = crearSolicitud.crear(new CrearSolicitudRegistroWhatsappComando(request.telefono(),
            request.nombres(), request.apellidos(), request.fechaNacimiento(), request.sexo(), request.ci(),
            request.correo(), request.latitud(), request.longitud()));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(EnlaceRegistroResponse.de(enlace), "Registro guardado"));
    }
}
