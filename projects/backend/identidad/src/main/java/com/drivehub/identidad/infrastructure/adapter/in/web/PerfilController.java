package com.drivehub.identidad.infrastructure.adapter.in.web;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.drivehub.identidad.common.response.ApiResponse;
import com.drivehub.identidad.domain.port.in.ActualizarFotoPerfilUseCase;
import com.drivehub.identidad.domain.port.in.CambiarContrasenaComando;
import com.drivehub.identidad.domain.port.in.CambiarContrasenaUseCase;
import com.drivehub.identidad.domain.port.in.EditarPerfilComando;
import com.drivehub.identidad.domain.port.in.EditarPerfilUseCase;
import com.drivehub.identidad.domain.port.in.ObtenerPerfilUseCase;
import com.drivehub.identidad.domain.port.in.QuitarFotoPerfilUseCase;
import com.drivehub.identidad.domain.port.in.SolicitarCambioCorreoComando;
import com.drivehub.identidad.domain.port.in.SolicitarCambioCorreoUseCase;
import com.drivehub.identidad.infrastructure.config.CurrentUserId;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Perfil de la cuenta propia (CU06): datos personales, foto, contraseña y pedir el cambio de correo.
 *
 * * La persona sale SIEMPRE de la sesión (`@CurrentUserId`), nunca de la URL ni del cuerpo.
 * * Confirmar el cambio de correo es público y vive en CambioCorreoController.
 */
@RestController
@RequestMapping("/api/v1/usuarios/me")
@RequiredArgsConstructor
public class PerfilController {

    private final ObtenerPerfilUseCase obtenerPerfil;
    private final EditarPerfilUseCase editarPerfil;
    private final ActualizarFotoPerfilUseCase actualizarFoto;
    private final QuitarFotoPerfilUseCase quitarFoto;
    private final CambiarContrasenaUseCase cambiarContrasena;
    private final SolicitarCambioCorreoUseCase solicitarCambioCorreo;

    @GetMapping
    public ResponseEntity<ApiResponse<PerfilResponse>> obtener(@CurrentUserId UUID usuarioCodigo) {
        return ResponseEntity.ok(ApiResponse.success(
            PerfilResponse.de(obtenerPerfil.obtener(usuarioCodigo)), "Perfil obtenido correctamente"));
    }

    @PatchMapping
    public ResponseEntity<ApiResponse<PerfilResponse>> editar(
            @CurrentUserId UUID usuarioCodigo, @Valid @RequestBody EditarPerfilRequest request) {
        EditarPerfilComando comando = new EditarPerfilComando(usuarioCodigo,
            Optional.ofNullable(request.getNombres()), Optional.ofNullable(request.getApellidos()),
            request.telefonoParaCambiar(), Optional.ofNullable(request.getFechaNacimiento()),
            Optional.ofNullable(request.getCi()), Optional.ofNullable(request.getSexo()),
            Optional.ofNullable(request.getLatitud()), Optional.ofNullable(request.getLongitud()));
        return ResponseEntity.ok(ApiResponse.success(
            PerfilResponse.de(editarPerfil.editar(comando)), "Perfil actualizado correctamente"));
    }

    @PutMapping("/foto")
    public ResponseEntity<ApiResponse<PerfilResponse>> subirFoto(
            @CurrentUserId UUID usuarioCodigo, @RequestPart("archivo") MultipartFile archivo) throws IOException {
        return ResponseEntity.ok(ApiResponse.success(
            PerfilResponse.de(actualizarFoto.actualizar(usuarioCodigo, archivo.getBytes())),
            "Foto actualizada correctamente"));
    }

    @DeleteMapping("/foto")
    public ResponseEntity<ApiResponse<PerfilResponse>> borrarFoto(@CurrentUserId UUID usuarioCodigo) {
        return ResponseEntity.ok(ApiResponse.success(
            PerfilResponse.de(quitarFoto.quitar(usuarioCodigo)), "Foto eliminada correctamente"));
    }

    @PostMapping("/cambiar-contrasena")
    public ResponseEntity<ApiResponse<Void>> cambiarContrasena(
            @CurrentUserId UUID usuarioCodigo, @Valid @RequestBody CambiarContrasenaRequest request) {
        cambiarContrasena.cambiar(new CambiarContrasenaComando(
            usuarioCodigo, request.passwordActual(), request.passwordNueva(), request.dispositivo()));
        return ResponseEntity.ok(ApiResponse.success(null, "Contraseña cambiada correctamente"));
    }

    @PostMapping("/cambiar-correo")
    public ResponseEntity<ApiResponse<Void>> cambiarCorreo(
            @CurrentUserId UUID usuarioCodigo, @Valid @RequestBody CambiarCorreoRequest request) {
        solicitarCambioCorreo.solicitar(new SolicitarCambioCorreoComando(
            usuarioCodigo, request.passwordActual(), request.correoNuevo()));
        return ResponseEntity.ok(ApiResponse.success(null, "Te enviamos un correo a la dirección nueva"));
    }
}
