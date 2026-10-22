package com.drivehub.identidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.DockerClientFactory;

import com.drivehub.identidad.domain.model.EstadoUsuario;
import com.drivehub.identidad.domain.model.RolNombres;
import com.drivehub.identidad.domain.model.TipoTokenCorreo;
import com.drivehub.identidad.domain.model.TokenRecuperacion;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.out.EnviadorCorreoPort;
import com.drivehub.identidad.domain.port.out.GeneradorTokenOpacoPort;
import com.drivehub.identidad.domain.port.out.PasswordHasherPort;
import com.drivehub.identidad.domain.port.out.RolRepositoryPort;
import com.drivehub.identidad.domain.port.out.TokenRecuperacionRepositoryPort;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;
import com.drivehub.identidad.infrastructure.config.AlmacenamientoConfig;
import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.http.Cookie;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;

/**
 * CU06 de punta a punta con Postgres y MinIO reales: ver y editar el perfil, subir y quitar la foto,
 * cambiar la contraseña y cambiar el correo. El envío de correo se reemplaza por un doble que guarda
 * el enlace. La cuenta es Super admin + Estudiante sin escuela, así entra por la web y por la app y no
 * hace falta una Escuela falsa. Cada método nombra el criterio (CAxx) del CU que cubre.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@ExtendWith(OutputCaptureExtension.class)
class PerfilIntegracionTest extends PostgresIntegrationTestBase {

    private static final String PASSWORD = "clave-de-prueba-1";
    private static final String RUTA = "/api/v1/usuarios/me";

    private static S3Client s3;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UsuarioRepositoryPort usuarios;
    @Autowired
    private RolRepositoryPort roles;
    @Autowired
    private PasswordHasherPort hasher;
    @Autowired
    private TokenRecuperacionRepositoryPort tokens;
    @Autowired
    private GeneradorTokenOpacoPort generadorToken;

    @MockitoBean
    private EnviadorCorreoPort correo;

    @BeforeAll
    static void levantarMinioSiHayDocker() {
        boolean dockerDisponible;
        try {
            dockerDisponible = DockerClientFactory.instance().isDockerAvailable();
        } catch (Exception e) {
            dockerDisponible = false;
        }
        assumeTrue(dockerDisponible, "Docker no está disponible: se salta el test con MinIO real");
        var minio = MinioTestContainer.iniciar();
        s3 = new AlmacenamientoConfig().s3Client(minio.getS3URL(), MinioTestContainer.USUARIO, MinioTestContainer.CLAVE);
        try {
            s3.createBucket(CreateBucketRequest.builder().bucket(MinioTestContainer.BUCKET).build());
        } catch (RuntimeException e) {
            // * El bucket ya existe: otra clase de test lo creó en el mismo container
        }
    }

    @AfterAll
    static void cerrarCliente() {
        if (s3 != null) {
            s3.close();
        }
    }

    @DynamicPropertySource
    static void propiedadesDeMinio(DynamicPropertyRegistry registry) {
        registry.add("app.minio.url", () -> MinioTestContainer.iniciar().getS3URL());
        registry.add("app.minio.public-url", () -> MinioTestContainer.iniciar().getS3URL());
        registry.add("app.minio.access-key", () -> MinioTestContainer.USUARIO);
        registry.add("app.minio.secret-key", () -> MinioTestContainer.CLAVE);
        registry.add("app.minio.bucket-name", () -> MinioTestContainer.BUCKET);
    }

    // ---------- ayudantes ----------

    private String crearCuenta() {
        String email = "perfil-" + UUID.randomUUID().toString().substring(0, 8) + "@gmail.com";
        usuarios.guardar(Usuario.builder().email(email).passwordHash(hasher.hashear(PASSWORD)).nombres("Ana")
            .apellidos("Pérez").telefono("0991234567").fechaNacimiento(LocalDate.of(1995, 5, 5))
            .estado(EstadoUsuario.ACTIVO).intentosFallidos(0)
            .roles(Set.of(roles.buscarPorNombre(RolNombres.SUPER_ADMIN).orElseThrow(),
                roles.buscarPorNombre(RolNombres.ESTUDIANTE).orElseThrow()))
            .build());
        return email;
    }

    private ResultActions login(String email, String password, String dispositivo) throws Exception {
        String json = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\",\"dispositivo\":\"" + dispositivo + "\"}";
        return mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String leer(ResultActions resultado, String ruta) throws Exception {
        return JsonPath.read(resultado.andReturn().getResponse().getContentAsString(), ruta);
    }

    private String accessToken(String email) throws Exception {
        return leer(login(email, PASSWORD, "WEB").andExpect(status().isOk()), "$.data.accessToken");
    }

    private static String bearer(String access) {
        return "Bearer " + access;
    }

    private static byte[] png() throws Exception {
        BufferedImage imagen = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(imagen, "png", salida);
        return salida.toByteArray();
    }

    private ResultActions subirFoto(String access, byte[] contenido) throws Exception {
        return mockMvc.perform(multipart(HttpMethod.PUT, RUTA + "/foto")
            .file(new MockMultipartFile("archivo", "foto.png", "image/png", contenido))
            .header(HttpHeaders.AUTHORIZATION, bearer(access)));
    }

    private ResultActions perfil(String access) throws Exception {
        return mockMvc.perform(get(RUTA).header(HttpHeaders.AUTHORIZATION, bearer(access)));
    }

    private List<String> objetosDe(String codigoUsuario) {
        return s3.listObjectsV2(ListObjectsV2Request.builder().bucket(MinioTestContainer.BUCKET)
                .prefix("usuarios/" + codigoUsuario + "/").build())
            .contents().stream().map(o -> o.key()).toList();
    }

    private static byte[] descargar(String url) throws Exception {
        HttpURLConnection conexion = (HttpURLConnection) URI.create(url).toURL().openConnection();
        assertThat(conexion.getResponseCode()).isEqualTo(200);
        return conexion.getInputStream().readAllBytes();
    }

    private ResultActions patchPerfil(String access, String cuerpo) throws Exception {
        return mockMvc.perform(patch(RUTA).header(HttpHeaders.AUTHORIZATION, bearer(access))
            .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private ResultActions cambiarContrasena(String access, String actual, String nueva, String dispositivo) throws Exception {
        return mockMvc.perform(post(RUTA + "/cambiar-contrasena").header(HttpHeaders.AUTHORIZATION, bearer(access))
            .contentType(MediaType.APPLICATION_JSON).content("{\"passwordActual\":\"" + actual + "\",\"passwordNueva\":\""
                + nueva + "\",\"dispositivo\":\"" + dispositivo + "\"}"));
    }

    private ResultActions pedirCambioCorreo(String access, String password, String correoNuevo) throws Exception {
        return mockMvc.perform(post(RUTA + "/cambiar-correo").header(HttpHeaders.AUTHORIZATION, bearer(access))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"passwordActual\":\"" + password + "\",\"correoNuevo\":\"" + correoNuevo + "\"}"));
    }

    private ResultActions confirmarCambioCorreo(String token) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/cambiar-correo/confirmar").contentType(MediaType.APPLICATION_JSON)
            .content("{\"token\":\"" + token + "\"}"));
    }

    /** Los enlaces de cambio de correo enviados al `destinatario`, en orden; el token va después de `token=`. */
    private List<String> enlacesDeCambio(String destinatario) {
        ArgumentCaptor<String> enlace = ArgumentCaptor.forClass(String.class);
        verify(correo, atLeastOnce()).enviarCambioCorreo(eq(destinatario), any(), enlace.capture());
        return enlace.getAllValues();
    }

    private static String tokenDe(String enlace) {
        return enlace.substring(enlace.indexOf("token=") + "token=".length());
    }

    // ---------- criterios ----------

    @Test
    void ca01Ca02Ca03_verYEditarElPerfil() throws Exception {
        String email = crearCuenta();
        String access = accessToken(email);

        perfil(access).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.email").value(email))
            .andExpect(jsonPath("$.data.nombres").value("Ana"))
            .andExpect(jsonPath("$.data.apellidos").value("Pérez"))
            .andExpect(jsonPath("$.data.telefono").value("0991234567"))
            .andExpect(jsonPath("$.data.fechaNacimiento").value("1995-05-05"))
            .andExpect(jsonPath("$.data.correoVerificado").value(false))
            .andExpect(jsonPath("$.data.roles[0]").value("ESTUDIANTE"))
            .andExpect(jsonPath("$.data.escuelaCodigo").doesNotExist())
            .andExpect(jsonPath("$.data.id").doesNotExist())
            .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

        patchPerfil(access, "{\"nombres\":\"María José\",\"telefono\":\"0987654321\"}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.nombres").value("María José"))
            .andExpect(jsonPath("$.data.apellidos").value("Pérez"))
            .andExpect(jsonPath("$.data.telefono").value("0987654321"));
        perfil(access).andExpect(jsonPath("$.data.nombres").value("María José"));

        patchPerfil(access, "{\"telefono\":null}").andExpect(status().isOk())
            .andExpect(jsonPath("$.data.telefono").doesNotExist());

        patchPerfil(access, "{\"fechaNacimiento\":\"" + LocalDate.now().minusYears(15) + "\"}")
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_IDE_02"));
        patchPerfil(access, "{\"nombres\":\"" + "a".repeat(81) + "\"}")
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        perfil(access).andExpect(jsonPath("$.data.fechaNacimiento").value("1995-05-05"));
    }

    @Test
    void ca04_ca05_ca06_ca07_ca08_laFotoSeGuardaSeReemplazaSeRechazaYSeQuita(CapturedOutput salida) throws Exception {
        String email = crearCuenta();
        String access = accessToken(email);
        String codigo = leer(perfil(access), "$.data.codigo");
        byte[] primera = png();

        ResultActions subida = subirFoto(access, primera).andExpect(status().isOk());
        String urlPrimera = leer(subida, "$.data.fotoUrl");
        List<String> objetos = objetosDe(codigo);
        assertThat(objetos).hasSize(1);
        assertThat(objetos.get(0)).matches("usuarios/" + codigo + "/[0-9a-f-]{36}\\.png");
        assertThat(descargar(urlPrimera)).isEqualTo(primera);

        // * Iniciar sesión y renovarla traen la URL de la foto
        login(email, PASSWORD, "WEB").andExpect(jsonPath("$.data.usuario.fotoUrl").isNotEmpty());

        // * Una segunda foto reemplaza y borra la primera
        subirFoto(access, png()).andExpect(status().isOk());
        List<String> trasReemplazar = objetosDe(codigo);
        assertThat(trasReemplazar).hasSize(1).doesNotContainAnyElementsOf(objetos);

        // * Rechazos: nada nuevo queda en el almacenamiento
        byte[] ejecutable = new byte[200];
        Arrays.fill(ejecutable, (byte) 7);
        ejecutable[0] = 'M';
        ejecutable[1] = 'Z';
        subirFoto(access, ejecutable).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_IDE_16"));
        byte[] conExtra = Arrays.copyOf(png(), png().length + 3);
        conExtra[conExtra.length - 1] = 'Z';
        subirFoto(access, conExtra).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_IDE_16"));
        subirFoto(access, new byte[5 * 1024 * 1024 + 1]).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_IDE_17"));
        assertThat(objetosDe(codigo)).isEqualTo(trasReemplazar);

        // * Quitar la foto la borra y repetirlo no falla
        mockMvc.perform(delete(RUTA + "/foto").header(HttpHeaders.AUTHORIZATION, bearer(access)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.fotoUrl").doesNotExist());
        assertThat(objetosDe(codigo)).isEmpty();
        mockMvc.perform(delete(RUTA + "/foto").header(HttpHeaders.AUTHORIZATION, bearer(access)))
            .andExpect(status().isOk());

        assertThat(salida.getOut()).doesNotContain("X-Amz-Signature").doesNotContain(urlPrimera);
    }

    @Test
    void ca09_ca10_ca11_cambiarLaContrasena(CapturedOutput salida) throws Exception {
        String email = crearCuenta();
        String refreshMovil = leer(login(email, PASSWORD, "MOVIL").andExpect(status().isOk()), "$.data.refreshToken");
        String access = accessToken(email);

        // * Actual incorrecta varias veces: ERR_IDE_14 y la cuenta no se bloquea
        for (int i = 0; i < 7; i++) {
            cambiarContrasena(access, "incorrecta-" + i, "nueva-clave-segura-1", "WEB")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_IDE_14"));
        }
        // * Igual a la actual
        cambiarContrasena(access, PASSWORD, PASSWORD, "WEB")
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_IDE_15"));
        // * Contraseña nueva corta
        cambiarContrasena(access, PASSWORD, "corta", "WEB")
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        login(email, PASSWORD, "WEB").andExpect(status().isOk());

        access = accessToken(email);
        cambiarContrasena(access, PASSWORD, "nueva-clave-segura-1", "WEB").andExpect(status().isOk())
            .andExpect(jsonPath("$.data").doesNotExist());

        login(email, "nueva-clave-segura-1", "WEB").andExpect(status().isOk());
        login(email, PASSWORD, "WEB").andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").value("ERR_IDE_04"));

        // * La sesión del otro dispositivo (móvil) deja de renovarse
        mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshMovil + "\",\"dispositivo\":\"MOVIL\"}"))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").value("ERR_IDE_09"));

        assertThat(salida.getOut()).doesNotContain("nueva-clave-segura-1").doesNotContain(PASSWORD)
            .doesNotContain(refreshMovil);
    }

    @Test
    void ca09_laSesionDelDispositivoActualSigueActivaTrasCambiarLaContrasena() throws Exception {
        String email = crearCuenta();
        ResultActions loginWeb = login(email, PASSWORD, "WEB").andExpect(status().isOk());
        String access = leer(loginWeb, "$.data.accessToken");
        String cabecera = loginWeb.andReturn().getResponse().getHeader(HttpHeaders.SET_COOKIE);
        String refreshWeb = cabecera.substring("refreshToken=".length(), cabecera.indexOf(';'));

        cambiarContrasena(access, PASSWORD, "nueva-clave-segura-1", "WEB").andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content("{\"dispositivo\":\"WEB\"}").cookie(new Cookie("refreshToken", refreshWeb)))
            .andExpect(status().isOk());
    }

    @Test
    void ca12_ca13_ca14_ca15_ca17_cambiarElCorreo(CapturedOutput salida) throws Exception {
        String viejo = crearCuenta();
        String otraCuenta = crearCuenta();
        String nuevo = "nuevo-" + UUID.randomUUID().toString().substring(0, 8) + "@gmail.com";
        String access = accessToken(viejo);

        // * Rechazos: contraseña incorrecta, correo en uso
        pedirCambioCorreo(access, "incorrecta", nuevo).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_IDE_14"));
        pedirCambioCorreo(access, PASSWORD, otraCuenta).andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("ERR_IDE_01"));
        pedirCambioCorreo(access, PASSWORD, viejo.toUpperCase()).andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("ERR_IDE_18"));

        // * El enlace va al correo NUEVO y el de la cuenta no cambia todavía
        pedirCambioCorreo(access, PASSWORD, nuevo).andExpect(status().isOk());
        String primero = tokenDe(enlacesDeCambio(nuevo).get(0));
        assertThat(enlacesDeCambio(nuevo).get(0)).contains("/account/change-email?token=");
        perfil(access).andExpect(jsonPath("$.data.email").value(viejo));

        // * Pedir otro invalida el primero
        pedirCambioCorreo(access, PASSWORD, nuevo).andExpect(status().isOk());
        List<String> enlaces = enlacesDeCambio(nuevo);
        String segundo = tokenDe(enlaces.get(enlaces.size() - 1));
        confirmarCambioCorreo(primero).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_IDE_11"));

        // * Un enlace de verificación o de recuperación no sirve para confirmar el cambio
        UUID usuarioCodigo = usuarios.buscarPorEmail(viejo).orElseThrow().getCodigo();
        for (TipoTokenCorreo tipo : new TipoTokenCorreo[] {TipoTokenCorreo.VERIFICACION, TipoTokenCorreo.RECUPERACION}) {
            String ajeno = "ajeno-" + tipo + "-" + UUID.randomUUID();
            tokens.guardar(TokenRecuperacion.builder().usuarioCodigo(usuarioCodigo).tipo(tipo)
                .tokenHash(generadorToken.hashear(ajeno)).expiraEn(Instant.now().plusSeconds(3600)).build());
            confirmarCambioCorreo(ajeno).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_IDE_11"));
        }

        // * Confirmar cambia el correo, lo deja verificado y avisa al anterior; el enlace no sirve otra vez
        confirmarCambioCorreo(segundo).andExpect(status().isOk());
        verify(correo).enviarAvisoCambioCorreo(eq(viejo), eq("Ana"), eq(nuevo));
        login(nuevo, PASSWORD, "WEB").andExpect(status().isOk()).andExpect(jsonPath("$.data.usuario.correoVerificado").value(true));
        login(viejo, PASSWORD, "WEB").andExpect(status().isUnauthorized());
        confirmarCambioCorreo(segundo).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_IDE_11"));

        assertThat(salida.getOut()).doesNotContain(primero).doesNotContain(segundo).doesNotContain(PASSWORD);
    }
}
