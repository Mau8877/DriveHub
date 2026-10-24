package com.drivehub.escuela.infrastructure.client;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.drivehub.escuela.domain.gateway.AdministradorCreado;
import com.drivehub.escuela.domain.gateway.AdministradorEscuelaGateway;
import com.drivehub.escuela.domain.gateway.DatosAdministrador;
import com.drivehub.escuela.domain.gateway.IdentidadNoDisponibleException;

/**
 * Llama a Identidad por REST interno, sin pasar por el gateway:
 * `POST` y `DELETE {app.identidad.url}/api/v1/usuarios/administradores-escuela`.
 *
 * * Reenvía el Bearer de la persona que hizo la petición (el Super admin): Identidad vuelve a
 * * validar el token y el rol, no confía en que la llamada venga de Escuela.
 * * Un 4xx de negocio de Identidad se relanza como la excepción equivalente con su MISMO código
 * * y mensaje (así el formulario muestra el motivo real). Un 5xx, un timeout o un error de red
 * * lanzan IdentidadNoDisponibleException: el resultado allá es incierto y el caso de uso compensa.
 * ! Nunca se escribe la contraseña ni el cuerpo de la petición en los registros.
 */
@Component
public class AdministradorEscuelaRestClient implements AdministradorEscuelaGateway {

    private static final String RUTA = "/api/v1/usuarios/administradores-escuela";

    private final RestClient restClient;

    public AdministradorEscuelaRestClient(
            @Value("${app.identidad.url}") String baseUrl,
            @Value("${app.identidad.timeout-segundos:5}") long timeoutSegundos) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(httpClient);
        fabrica.setReadTimeout(Duration.ofSeconds(timeoutSegundos));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(fabrica).build();
    }

    @Override
    public AdministradorCreado crearAdministrador(UUID escuelaCodigo, DatosAdministrador datos) {
        PeticionCrear cuerpo = new PeticionCrear(datos.nombres(), datos.apellidos(), datos.fechaNacimiento().toString(),
            datos.email(), datos.password(), datos.telefono(), escuelaCodigo, datos.ci(), datos.sexo(),
            datos.latitud(), datos.longitud());
        try {
            SobreAdministrador sobre = restClient.post()
                .uri(RUTA)
                .header(HttpHeaders.AUTHORIZATION, TraductorDeErroresDeIdentidad.bearerDeLaSesion())
                .contentType(MediaType.APPLICATION_JSON)
                .body(cuerpo)
                .retrieve()
                .body(SobreAdministrador.class);
            if (sobre == null || sobre.data() == null) {
                throw new IdentidadNoDisponibleException("Identidad respondió sin los datos del administrador");
            }
            DatosRespuesta d = sobre.data();
            return new AdministradorCreado(d.codigo(), d.email(), d.nombres(), d.apellidos());
        } catch (RestClientResponseException e) {
            throw TraductorDeErroresDeIdentidad.traducir(e);
        } catch (RestClientException e) {
            throw new IdentidadNoDisponibleException("No se pudo hablar con Identidad", e);
        }
    }

    @Override
    public void revertirAdministrador(UUID escuelaCodigo) {
        try {
            restClient.delete()
                .uri(uri -> uri.path(RUTA).queryParam("escuelaCodigo", escuelaCodigo).build())
                .header(HttpHeaders.AUTHORIZATION, TraductorDeErroresDeIdentidad.bearerDeLaSesion())
                .retrieve()
                .toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw TraductorDeErroresDeIdentidad.traducir(e);
        } catch (RestClientException e) {
            throw new IdentidadNoDisponibleException("No se pudo hablar con Identidad", e);
        }
    }

    // * Cuerpo de la petición a Identidad. La fecha viaja como texto ISO (yyyy-MM-dd).
    // * CU08a: suma CI, sexo y la ubicación; Identidad calcula la dirección.
    private record PeticionCrear(String nombres, String apellidos, String fechaNacimiento, String email,
            String password, String telefono, UUID escuelaCodigo, String ci, String sexo, BigDecimal latitud,
            BigDecimal longitud) {

        @Override
        public String toString() {
            return "PeticionCrear[email=" + email + ", password=***]";
        }
    }

    // * Sobre estándar de Identidad ({status, data, ...}); solo se leen estos campos
    private record SobreAdministrador(DatosRespuesta data) {
    }

    private record DatosRespuesta(UUID codigo, String email, String nombres, String apellidos) {
    }
}
