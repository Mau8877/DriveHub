package com.drivehub.escuela.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.drivehub.escuela.common.exception.BusinessException;
import com.drivehub.escuela.common.exception.ServicioNoDisponibleException;
import com.drivehub.escuela.domain.entity.MomentoDelDia;
import com.drivehub.escuela.domain.gateway.ResumenFranjas;
import com.sun.net.httpserver.HttpServer;

/**
 * CU11: el cliente de Académico contra un Académico falso (HttpServer del JDK).
 */
class AcademicoRestClientTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 11, 2);

    private HttpServer academicoFalso;
    private AcademicoRestClient cliente;

    private record Peticion(String metodo, String rutaYConsulta, String autorizacion) {
    }

    private final List<Peticion> peticiones = new CopyOnWriteArrayList<>();
    private volatile int estado = 200;
    private volatile String json = "{}";
    private volatile long demoraMs = 0;

    @BeforeEach
    void levantarAcademicoFalso() throws IOException {
        academicoFalso = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        academicoFalso.createContext("/", exchange -> {
            peticiones.add(new Peticion(exchange.getRequestMethod(), exchange.getRequestURI().toString(),
                exchange.getRequestHeaders().getFirst("Authorization")));
            try {
                Thread.sleep(demoraMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(estado, bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                try (OutputStream salida = exchange.getResponseBody()) {
                    salida.write(bytes);
                }
            }
            exchange.close();
        });
        academicoFalso.start();
        cliente = new AcademicoRestClient("http://localhost:" + academicoFalso.getAddress().getPort(), 1);
        Jwt jwt = Jwt.withTokenValue("token-del-admin").header("alg", "none")
            .claim("sub", UUID.randomUUID().toString()).issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(900)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @AfterEach
    void apagar() {
        SecurityContextHolder.clearContext();
        academicoFalso.stop(0);
    }

    private static String sobre(UUID instructor, int total, int libres, int reservadas, int bloqueadas) {
        return "{\"status\":\"success\",\"data\":[{\"instructorCodigo\":\"" + instructor + "\",\"total\":" + total
            + ",\"libres\":" + libres + ",\"reservadas\":" + reservadas + ",\"bloqueadas\":" + bloqueadas
            + "}],\"meta\":{\"page\":1,\"pageSize\":1,\"totalItems\":1,\"totalPages\":1}}";
    }

    @Test
    void resumenDelDia_leeElResumenEnUnMapaPorInstructor() {
        UUID instructor = UUID.randomUUID();
        json = sobre(instructor, 6, 3, 2, 1);

        Map<UUID, ResumenFranjas> resumen = cliente.resumenDelDia(FECHA, null);

        assertThat(resumen).containsOnlyKeys(instructor);
        assertThat(resumen.get(instructor)).isEqualTo(new ResumenFranjas(6, 3, 2, 1));
    }

    @Test
    void resumenDelDia_sinFranjas_devuelveUnMapaVacio() {
        json = "{\"status\":\"success\",\"data\":[],\"meta\":{\"page\":1,\"pageSize\":1,\"totalItems\":0,"
            + "\"totalPages\":0}}";

        assertThat(cliente.resumenDelDia(FECHA, null)).isEmpty();
    }

    @Test
    void resumenDelDia_armaLaRutaConLosFiltrosCodificadosYElBearerDelAdministrador() {
        json = "{\"status\":\"success\",\"data\":[]}";

        cliente.resumenDelDia(FECHA, MomentoDelDia.TARDE);

        assertThat(peticiones).hasSize(1);
        Peticion peticion = peticiones.get(0);
        assertThat(peticion.metodo()).isEqualTo("GET");
        assertThat(peticion.autorizacion()).isEqualTo("Bearer token-del-admin");
        // * Los corchetes de filter[campo] viajan codificados: Tomcat rechaza con 400 los que no lo están
        assertThat(peticion.rutaYConsulta()).startsWith("/api/v1/franjas/resumen?")
            .contains("filter%5Bfecha%5D=2026-11-02")
            .contains("filter%5BhoraDesde%5D=12:00")
            .contains("filter%5BhoraHasta%5D=18:00")
            .doesNotContain("[").doesNotContain("]");
    }

    @Test
    void resumenDelDia_conUnTramoSinLimites_soloMandaLaHoraQueTiene() {
        json = "{\"status\":\"success\",\"data\":[]}";

        cliente.resumenDelDia(FECHA, MomentoDelDia.MANANA);
        cliente.resumenDelDia(FECHA, MomentoDelDia.NOCHE);
        cliente.resumenDelDia(FECHA, null);

        assertThat(peticiones.get(0).rutaYConsulta()).contains("filter%5BhoraHasta%5D=12:00")
            .doesNotContain("horaDesde");
        assertThat(peticiones.get(1).rutaYConsulta()).contains("filter%5BhoraDesde%5D=18:00")
            .doesNotContain("horaHasta");
        assertThat(peticiones.get(2).rutaYConsulta()).doesNotContain("hora");
    }

    @Test
    void resumenDelDia_unErrorDeServidorDeAcademico_esErrEsc20() {
        estado = 500;
        json = "{\"status\":\"failed\",\"error\":\"ERR_SYS_00\"}";

        assertErrEsc20();
    }

    @Test
    void resumenDelDia_unRechazoDeAcademico_esErrEsc20() {
        estado = 403;
        json = "{\"status\":\"failed\",\"error\":\"ERR_SYS_05\"}";

        assertErrEsc20();
    }

    @Test
    void resumenDelDia_unCuerpoSinDatos_esErrEsc20() {
        json = "{\"status\":\"success\"}";

        assertErrEsc20();
    }

    @Test
    void resumenDelDia_unCuerpoVacio_esErrEsc20() {
        json = "";

        assertErrEsc20();
    }

    @Test
    void resumenDelDia_siAcademicoTardaMasQueElTiempoDeEspera_esErrEsc20() {
        json = "{\"status\":\"success\",\"data\":[]}";
        demoraMs = 2500;

        assertErrEsc20();
    }

    @Test
    void resumenDelDia_siAcademicoNoEstaArriba_esErrEsc20() {
        academicoFalso.stop(0);

        assertErrEsc20();
    }

    private void assertErrEsc20() {
        assertThatThrownBy(() -> cliente.resumenDelDia(FECHA, null))
            .isInstanceOf(ServicioNoDisponibleException.class)
            .isInstanceOfSatisfying(BusinessException.class, e -> {
                assertThat(e.getErrorCode()).isEqualTo("ERR_ESC_20");
                assertThat(e.getHttpStatus().value()).isEqualTo(503);
            });
    }
}
