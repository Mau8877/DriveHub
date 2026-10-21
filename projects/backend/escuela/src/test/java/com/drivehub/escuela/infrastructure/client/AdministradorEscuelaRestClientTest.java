package com.drivehub.escuela.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.drivehub.escuela.common.exception.BusinessException;
import com.drivehub.escuela.common.exception.ConflictException;
import com.drivehub.escuela.common.exception.ForbiddenException;
import com.drivehub.escuela.common.exception.ValidationException;
import com.drivehub.escuela.domain.gateway.AdministradorCreado;
import com.drivehub.escuela.domain.gateway.DatosAdministrador;
import com.drivehub.escuela.domain.gateway.IdentidadNoDisponibleException;
import com.sun.net.httpserver.HttpServer;

/**
 * El cliente de Identidad contra una Identidad falsa (HttpServer del JDK).
 */
class AdministradorEscuelaRestClientTest {

    private static final UUID ESCUELA = UUID.randomUUID();
    private static final String CLAVE = "claveSecreta123";

    private HttpServer identidadFalsa;
    private AdministradorEscuelaRestClient cliente;

    // * Lo último que recibió la Identidad falsa
    private final AtomicReference<String> metodo = new AtomicReference<>();
    private final AtomicReference<String> rutaYConsulta = new AtomicReference<>();
    private final AtomicReference<String> autorizacion = new AtomicReference<>();
    private final AtomicReference<String> cuerpo = new AtomicReference<>();

    // * Lo que responde la Identidad falsa a la siguiente petición
    private volatile int estadoRespuesta = 201;
    private volatile String cuerpoRespuesta = "";
    private volatile long demoraMs = 0;

    @BeforeEach
    void levantarIdentidadFalsa() throws IOException {
        identidadFalsa = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        identidadFalsa.createContext("/", exchange -> {
            metodo.set(exchange.getRequestMethod());
            rutaYConsulta.set(exchange.getRequestURI().toString());
            autorizacion.set(exchange.getRequestHeaders().getFirst("Authorization"));
            cuerpo.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            try {
                Thread.sleep(demoraMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] bytes = cuerpoRespuesta.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(estadoRespuesta, bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                try (OutputStream salida = exchange.getResponseBody()) {
                    salida.write(bytes);
                }
            }
            exchange.close();
        });
        identidadFalsa.start();
        cliente = new AdministradorEscuelaRestClient("http://localhost:" + identidadFalsa.getAddress().getPort(), 1);
        iniciarSesionComo("token-del-super-admin");
    }

    @AfterEach
    void apagar() {
        SecurityContextHolder.clearContext();
        identidadFalsa.stop(0);
    }

    private void iniciarSesionComo(String token) {
        Jwt jwt = Jwt.withTokenValue(token).header("alg", "none").claim("sub", UUID.randomUUID().toString())
            .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(900)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    private DatosAdministrador datos() {
        return new DatosAdministrador("Marta", "Gómez", LocalDate.of(1985, 5, 5), "marta@gmail.com", CLAVE, null);
    }

    private void responde(int estado, String json) {
        estadoRespuesta = estado;
        cuerpoRespuesta = json;
    }

    private static String error(String codigo, String mensaje) {
        return "{\"status\":\"failed\",\"data\":null,\"message\":\"" + mensaje + "\",\"error\":\"" + codigo + "\"}";
    }

    @Test
    void crearAdministrador_enviaElBearerYElCuerpoEsperadoYDevuelveLaCuentaCreada() {
        UUID codigo = UUID.randomUUID();
        responde(201, "{\"status\":\"success\",\"data\":{\"codigo\":\"" + codigo + "\",\"email\":\"marta@gmail.com\","
            + "\"nombres\":\"Marta\",\"apellidos\":\"Gómez\",\"escuelaCodigo\":\"" + ESCUELA + "\"},"
            + "\"message\":\"Administrador creado correctamente\"}");

        AdministradorCreado creado = cliente.crearAdministrador(ESCUELA, datos());

        assertThat(creado).isEqualTo(new AdministradorCreado(codigo, "marta@gmail.com", "Marta", "Gómez"));
        assertThat(metodo.get()).isEqualTo("POST");
        assertThat(rutaYConsulta.get()).isEqualTo("/api/v1/usuarios/administradores-escuela");
        assertThat(autorizacion.get()).isEqualTo("Bearer token-del-super-admin");
        assertThat(cuerpo.get())
            .contains("\"escuelaCodigo\":\"" + ESCUELA + "\"", "\"email\":\"marta@gmail.com\"",
                "\"fechaNacimiento\":\"1985-05-05\"", "\"nombres\":\"Marta\"");
    }

    @Test
    void crearAdministrador_sinSesionConToken_noLlamaAIdentidad() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> cliente.crearAdministrador(ESCUELA, datos()))
            .isInstanceOf(IllegalStateException.class);
        assertThat(metodo.get()).isNull();
    }

    @Test
    void crearAdministrador_conCorreoEnUso_lanzaConflictConElMismoCodigoYMensaje() {
        responde(409, error("ERR_IDE_01", "Ya existe una cuenta con ese correo"));

        assertThatThrownBy(() -> cliente.crearAdministrador(ESCUELA, datos()))
            .isInstanceOfSatisfying(ConflictException.class, ex -> {
                assertThat(ex.getErrorCode()).isEqualTo("ERR_IDE_01");
                assertThat(ex.getMessage()).isEqualTo("Ya existe una cuenta con ese correo");
            });
    }

    @Test
    void crearAdministrador_conMenorDeEdad_lanzaValidationConSuCodigo() {
        responde(400, error("ERR_IDE_02", "Debe tener al menos 18 años"));

        assertThatThrownBy(() -> cliente.crearAdministrador(ESCUELA, datos()))
            .isInstanceOfSatisfying(ValidationException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo("ERR_IDE_02"));
    }

    @Test
    void crearAdministrador_sinPermiso_lanzaForbidden() {
        responde(403, error("ERR_SYS_05", "No tienes permiso"));

        assertThatThrownBy(() -> cliente.crearAdministrador(ESCUELA, datos()))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void crearAdministrador_conErrorDeServidor_lanzaIdentidadNoDisponibleYNoUnaExcepcionDeNegocio() {
        responde(500, error("ERR_SYS_00", "Ocurrió un error inesperado"));

        assertThatThrownBy(() -> cliente.crearAdministrador(ESCUELA, datos()))
            .isInstanceOf(IdentidadNoDisponibleException.class)
            .isNotInstanceOf(BusinessException.class);
    }

    @Test
    void crearAdministrador_siIdentidadTardaMasQueElTiempoDeEspera_lanzaIdentidadNoDisponible() {
        responde(201, "{}");
        demoraMs = 2500;

        assertThatThrownBy(() -> cliente.crearAdministrador(ESCUELA, datos()))
            .isInstanceOf(IdentidadNoDisponibleException.class);
    }

    @Test
    void crearAdministrador_siIdentidadEstaCaida_lanzaIdentidadNoDisponible() {
        identidadFalsa.stop(0);

        assertThatThrownBy(() -> cliente.crearAdministrador(ESCUELA, datos()))
            .isInstanceOf(IdentidadNoDisponibleException.class);
    }

    @Test
    void crearAdministrador_ningunErrorNiMensajeContieneLaContrasena() {
        responde(500, error("ERR_SYS_00", "x"));

        assertThatThrownBy(() -> cliente.crearAdministrador(ESCUELA, datos()))
            .satisfies(ex -> {
                assertThat(String.valueOf(ex.getMessage())).doesNotContain(CLAVE);
                assertThat(String.valueOf(ex.getCause())).doesNotContain(CLAVE);
            });
        assertThat(datos().toString()).doesNotContain(CLAVE);
    }

    @Test
    void revertirAdministrador_llamaADeleteConElCodigoDeLaEscuelaYElBearer() {
        responde(200, "{\"status\":\"success\",\"data\":null,\"message\":\"Administrador revertido correctamente\"}");

        cliente.revertirAdministrador(ESCUELA);

        assertThat(metodo.get()).isEqualTo("DELETE");
        assertThat(rutaYConsulta.get()).isEqualTo("/api/v1/usuarios/administradores-escuela?escuelaCodigo=" + ESCUELA);
        assertThat(autorizacion.get()).isEqualTo("Bearer token-del-super-admin");
    }

    @Test
    void revertirAdministrador_siElAdministradorYaSeUso_lanzaConflictConErrIde13() {
        responde(409, error("ERR_IDE_13", "Ya inició sesión"));

        assertThatThrownBy(() -> cliente.revertirAdministrador(ESCUELA))
            .isInstanceOfSatisfying(ConflictException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo("ERR_IDE_13"));
    }

    @Test
    void revertirAdministrador_conErrorDeServidor_lanzaIdentidadNoDisponible() {
        responde(503, "");

        assertThatThrownBy(() -> cliente.revertirAdministrador(ESCUELA))
            .isInstanceOf(IdentidadNoDisponibleException.class);
        assertThatCode(() -> {
            responde(200, "{}");
            cliente.revertirAdministrador(ESCUELA);
        }).doesNotThrowAnyException();
    }
}
