package com.drivehub.identidad.infrastructure.adapter.in.web;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.drivehub.identidad.common.response.ApiResponse;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.in.CrearAdministradorEscuelaComando;
import com.drivehub.identidad.domain.port.in.CrearAdministradorEscuelaUseCase;
import com.drivehub.identidad.domain.port.in.RegistrarEstudianteComando;
import com.drivehub.identidad.domain.port.in.RegistrarEstudianteUseCase;
import com.drivehub.identidad.domain.port.in.RevertirAdministradorEscuelaUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Recurso `usuarios`: el registro del estudiante (CU01), que es público, y los endpoints
 * internos de los Administradores de escuela (CU05), que exigen un Super admin.
 */
@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final RegistrarEstudianteUseCase registrarEstudiante;
    private final CrearAdministradorEscuelaUseCase crearAdministradorEscuela;
    private final RevertirAdministradorEscuelaUseCase revertirAdministradorEscuela;

    @PostMapping
    public ResponseEntity<ApiResponse<UsuarioRegistradoResponse>> registrar(
            @Valid @RequestBody RegistrarEstudianteRequest request) {
        Usuario usuario = registrarEstudiante.registrar(new RegistrarEstudianteComando(
            request.nombres(), request.apellidos(), request.fechaNacimiento(), request.email(),
            request.password(), request.telefono(), request.escuelaSlug(), request.ci(), request.sexo(),
            request.latitud(), request.longitud()));
        UsuarioRegistradoResponse response = new UsuarioRegistradoResponse(
            usuario.getCodigo(), usuario.getEmail(), usuario.getNombres(), usuario.getApellidos());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, "Usuario registrado correctamente"));
    }

    /** Lo llama Escuela por la red interna al crear una escuela (CU05). */
    @PostMapping("/administradores-escuela")
    public ResponseEntity<ApiResponse<AdministradorEscuelaResponse>> crearAdministradorEscuela(
            @Valid @RequestBody CrearAdministradorEscuelaRequest request) {
        Usuario usuario = crearAdministradorEscuela.crear(new CrearAdministradorEscuelaComando(
            request.nombres(), request.apellidos(), request.fechaNacimiento(), request.email(),
            request.password(), request.telefono(), request.escuelaCodigo(), request.ci(), request.sexo(),
            request.latitud(), request.longitud()));
        AdministradorEscuelaResponse response = new AdministradorEscuelaResponse(
            usuario.getCodigo(), usuario.getEmail(), usuario.getNombres(), usuario.getApellidos(),
            usuario.getEscuelaCodigo());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, "Administrador creado correctamente"));
    }

    /** Compensación de Escuela si la creación de la escuela falló a medias (CU05). Idempotente. */
    @DeleteMapping("/administradores-escuela")
    public ResponseEntity<ApiResponse<Void>> revertirAdministradorEscuela(@RequestParam UUID escuelaCodigo) {
        revertirAdministradorEscuela.revertir(escuelaCodigo);
        return ResponseEntity.ok(ApiResponse.success(null, "Administrador revertido correctamente"));
    }
}
