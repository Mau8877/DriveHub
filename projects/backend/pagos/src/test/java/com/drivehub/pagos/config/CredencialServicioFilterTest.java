package com.drivehub.pagos.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.ServletException;

/**
 * La puerta de las rutas internas (CU44): solo pasa quien manda la credencial exacta, y nunca se loguea su valor.
 */
class CredencialServicioFilterTest {

    private static final String CREDENCIAL = "credencial-secreta-de-prueba";

    private ListAppender<ILoggingEvent> logs;

    @BeforeEach
    void escucharLogs() {
        logs = new ListAppender<>();
        logs.start();
        ((Logger) LoggerFactory.getLogger(CredencialServicioFilter.class)).addAppender(logs);
    }

    @AfterEach
    void dejarDeEscuchar() {
        ((Logger) LoggerFactory.getLogger(CredencialServicioFilter.class)).detachAppender(logs);
    }

    private MockHttpServletResponse pasar(String credencialConfigurada, String ruta, String cabecera)
            throws ServletException, IOException {
        MockHttpServletRequest peticion = new MockHttpServletRequest("POST", ruta);
        peticion.setRequestURI(ruta);
        if (cabecera != null) {
            peticion.addHeader("X-Service-Key", cabecera);
        }
        MockHttpServletResponse respuesta = new MockHttpServletResponse();
        new CredencialServicioFilter(credencialConfigurada).doFilter(peticion, respuesta, new MockFilterChain());
        return respuesta;
    }

    private boolean continuo(MockHttpServletResponse respuesta) {
        return respuesta.getStatus() == 200;
    }

    @Test
    void conLaCredencialCorrecta_laPeticionContinua() throws Exception {
        MockHttpServletResponse respuesta = pasar(CREDENCIAL, "/internal/v1/billeteras-horas/consumos", CREDENCIAL);

        assertThat(continuo(respuesta)).isTrue();
        assertThat(respuesta.getContentAsString()).isEmpty();
    }

    @Test
    void sinElHeader_responde401ConErrPag19EnElSobre() throws Exception {
        MockHttpServletResponse respuesta = pasar(CREDENCIAL, "/internal/v1/billeteras-horas/consumos", null);

        assertThat(respuesta.getStatus()).isEqualTo(401);
        assertThat(respuesta.getContentType()).startsWith("application/json");
        assertThat(respuesta.getContentAsString()).contains("\"status\":\"failed\"", "\"error\":\"ERR_PAG_19\"",
            "Credencial de servicio inválida", "\"timestamp\"");
    }

    @Test
    void conUnaCredencialIncorrecta_responde401() throws Exception {
        MockHttpServletResponse respuesta = pasar(CREDENCIAL, "/internal/v1/billeteras-horas/consumos", "otra");

        assertThat(respuesta.getStatus()).isEqualTo(401);
        assertThat(respuesta.getContentAsString()).contains("ERR_PAG_19");
    }

    @Test
    void conUnaCredencialQueSoloSePareceAlInicio_responde401() throws Exception {
        assertThat(pasar(CREDENCIAL, "/internal/x", CREDENCIAL + "-").getStatus()).isEqualTo(401);
        assertThat(pasar(CREDENCIAL, "/internal/x", CREDENCIAL.substring(1)).getStatus()).isEqualTo(401);
    }

    @Test
    void sinCredencialConfigurada_rechazaTodo_aunqueElHeaderVengaVacioOIgual() throws Exception {
        assertThat(pasar("", "/internal/x", "").getStatus()).isEqualTo(401);
        assertThat(pasar("", "/internal/x", null).getStatus()).isEqualTo(401);
        assertThat(pasar(null, "/internal/x", "algo").getStatus()).isEqualTo(401);
    }

    @Test
    void unaRutaQueNoEsInterna_noLaTocaElFiltro() throws Exception {
        MockHttpServletResponse respuesta = pasar(CREDENCIAL, "/api/v1/pagos", null);

        assertThat(continuo(respuesta)).isTrue();
        assertThat(logs.list).isEmpty();
    }

    @Test
    void elValorDeLaCredencialNuncaApareceEnElLog() throws Exception {
        pasar(CREDENCIAL, "/internal/v1/billeteras-horas/consumos", "intento-con-otro-valor");
        pasar(CREDENCIAL, "/internal/v1/billeteras-horas/consumos", null);

        assertThat(logs.list).isNotEmpty();
        for (ILoggingEvent evento : logs.list) {
            assertThat(evento.getFormattedMessage()).doesNotContain(CREDENCIAL).doesNotContain("intento-con-otro-valor");
            assertThat(String.valueOf(evento.getArgumentArray())).doesNotContain(CREDENCIAL)
                .doesNotContain("intento-con-otro-valor");
        }
        assertThat(logs.list.get(0).getLevel().toString()).isEqualTo("WARN");
    }
}
