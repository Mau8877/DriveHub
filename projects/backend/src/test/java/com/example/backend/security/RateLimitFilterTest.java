package com.example.backend.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitFilterTest {

    @Test
    void doFilterInternal_dentroDelLimite_dejaPasarTodasLasRequests() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(3, 60);
        AtomicInteger vecesQueLlegoAlChain = new AtomicInteger();

        for (int i = 0; i < 3; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilterInternal(
                    apiRequest("203.0.113.10"),
                    response,
                    (req, res) -> vecesQueLlegoAlChain.incrementAndGet());

            assertThat(response.getStatus()).isEqualTo(200);
        }

        assertThat(vecesQueLlegoAlChain.get()).isEqualTo(3);
    }

    @Test
    void doFilterInternal_superandoElLimite_devuelve429ConElSobreDeError() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(2, 60);
        AtomicInteger vecesQueLlegoAlChain = new AtomicInteger();

        // Agota el cupo (2 requests permitidas)
        for (int i = 0; i < 2; i++) {
            filter.doFilterInternal(
                    apiRequest("203.0.113.20"),
                    new MockHttpServletResponse(),
                    (req, res) -> vecesQueLlegoAlChain.incrementAndGet());
        }

        // La tercera ya no debería llegar al resto de la cadena
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(
                apiRequest("203.0.113.20"),
                response,
                (req, res) -> vecesQueLlegoAlChain.incrementAndGet());

        assertThat(vecesQueLlegoAlChain.get()).isEqualTo(2);
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentAsString())
                .contains("\"status\":\"failed\"")
                .contains("\"error\":\"ERR_SYS_02\"");
    }

    @Test
    void doFilterInternal_distintasIps_tienenCuposIndependientes() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(1, 60);
        AtomicInteger vecesQueLlegoAlChain = new AtomicInteger();

        filter.doFilterInternal(
                apiRequest("203.0.113.30"),
                new MockHttpServletResponse(),
                (req, res) -> vecesQueLlegoAlChain.incrementAndGet());

        // Otra IP: no debería verse afectada por el cupo agotado de la anterior
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(
                apiRequest("203.0.113.31"),
                response,
                (req, res) -> vecesQueLlegoAlChain.incrementAndGet());

        assertThat(vecesQueLlegoAlChain.get()).isEqualTo(2);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void shouldNotFilter_rutasFueraDeApiV1_quedanExcluidas() {
        RateLimitFilter filter = new RateLimitFilter(100, 100);

        assertThat(filter.shouldNotFilter(requestTo("/health"))).isTrue();
        assertThat(filter.shouldNotFilter(requestTo("/prometheus"))).isTrue();
        assertThat(filter.shouldNotFilter(requestTo("/swagger-ui/index.html"))).isTrue();
        assertThat(filter.shouldNotFilter(requestTo("/api/v1/usuarios"))).isFalse();
    }

    private static MockHttpServletRequest apiRequest(String remoteAddr) {
        MockHttpServletRequest request = requestTo("/api/v1/ping");
        request.setRemoteAddr(remoteAddr);
        return request;
    }

    private static MockHttpServletRequest requestTo(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(uri);
        return request;
    }
}
