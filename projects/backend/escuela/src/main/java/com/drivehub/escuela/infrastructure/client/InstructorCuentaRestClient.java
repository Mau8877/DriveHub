package com.drivehub.escuela.infrastructure.client;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.drivehub.escuela.domain.gateway.DatosCuentaInstructor;
import com.drivehub.escuela.domain.gateway.IdentidadNoDisponibleException;
import com.drivehub.escuela.domain.gateway.InstructorCuentaGateway;
import com.drivehub.escuela.domain.gateway.PersonaInstructor;

/**
 * Llama a Identidad por REST interno, sin pasar por el gateway:
 * `{app.identidad.url}/api/v1/usuarios/instructores`.
 *
 * * Reenvía el Bearer de la persona que hizo la petición (el Administrador): Identidad vuelve a
 * * validar el token y el rol, y toma la escuela de su cuenta.
 * * Los errores se traducen como en CU05 (ver {@link TraductorDeErroresDeIdentidad}).
 * ! Nunca se escriben la contraseña, el CI, el correo ni el cuerpo de la petición en los registros.
 */
@Component
public class InstructorCuentaRestClient implements InstructorCuentaGateway {

    private static final Logger LOG = LoggerFactory.getLogger(InstructorCuentaRestClient.class);
    private static final String RUTA = "/api/v1/usuarios/instructores";
    static final int TAMANO_LOTE = 100;
    static final int MAXIMO_PAGINAS_DE_BUSQUEDA = 5;

    private final RestClient restClient;

    public InstructorCuentaRestClient(
            @Value("${app.identidad.url}") String baseUrl,
            @Value("${app.identidad.timeout-segundos:5}") long timeoutSegundos) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(httpClient);
        fabrica.setReadTimeout(Duration.ofSeconds(timeoutSegundos));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(fabrica).build();
    }

    @Override
    public PersonaInstructor crearCuenta(DatosCuentaInstructor datos) {
        PeticionCrear cuerpo = new PeticionCrear(datos.usuarioCodigo(), datos.nombres(), datos.apellidos(),
            datos.fechaNacimiento().toString(), datos.sexo(), datos.ci(), datos.telefono(), datos.email(),
            datos.password(), datos.latitud(), datos.longitud());
        SobrePersona sobre = TraductorDeErroresDeIdentidad.ejecutar(() -> restClient.post()
            .uri(RUTA)
            .header(HttpHeaders.AUTHORIZATION, TraductorDeErroresDeIdentidad.bearerDeLaSesion())
            .contentType(MediaType.APPLICATION_JSON)
            .body(cuerpo)
            .retrieve()
            .body(SobrePersona.class));
        if (sobre == null || sobre.data() == null) {
            throw new IdentidadNoDisponibleException("Identidad respondió sin los datos del instructor");
        }
        return sobre.data().aDominio();
    }

    @Override
    public void revertirCuenta(UUID usuarioCodigo) {
        TraductorDeErroresDeIdentidad.ejecutar(() -> restClient.delete()
            .uri(RUTA + "/{codigo}", usuarioCodigo)
            .header(HttpHeaders.AUTHORIZATION, TraductorDeErroresDeIdentidad.bearerDeLaSesion())
            .retrieve()
            .toBodilessEntity());
    }

    @Override
    public List<PersonaInstructor> consultar(Collection<UUID> usuarioCodigos) {
        List<UUID> codigos = List.copyOf(usuarioCodigos);
        List<PersonaInstructor> personas = new ArrayList<>();
        for (int desde = 0; desde < codigos.size(); desde += TAMANO_LOTE) {
            List<UUID> lote = codigos.subList(desde, Math.min(desde + TAMANO_LOTE, codigos.size()));
            SobreListaPersonas sobre = TraductorDeErroresDeIdentidad.ejecutar(() -> restClient.post()
                .uri(RUTA + "/consulta")
                .header(HttpHeaders.AUTHORIZATION, TraductorDeErroresDeIdentidad.bearerDeLaSesion())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new PeticionConsulta(lote))
                .retrieve()
                .body(SobreListaPersonas.class));
            if (sobre == null || sobre.data() == null) {
                throw new IdentidadNoDisponibleException("Identidad respondió sin los datos de los instructores");
            }
            sobre.data().forEach(d -> personas.add(d.aDominio()));
        }
        return personas;
    }

    @Override
    public List<UUID> buscarCodigos(String busqueda) {
        List<UUID> codigos = new ArrayList<>();
        int pagina = 1;
        while (true) {
            int paginaActual = pagina;
            SobreListaPersonas sobre = TraductorDeErroresDeIdentidad.ejecutar(() -> restClient.get()
                .uri(uri -> uri.path(RUTA).queryParam("search", busqueda).queryParam("page", paginaActual)
                    .queryParam("pageSize", TAMANO_LOTE).build())
                .header(HttpHeaders.AUTHORIZATION, TraductorDeErroresDeIdentidad.bearerDeLaSesion())
                .retrieve()
                .body(SobreListaPersonas.class));
            if (sobre == null || sobre.data() == null) {
                throw new IdentidadNoDisponibleException("Identidad respondió sin los datos de la búsqueda");
            }
            sobre.data().forEach(d -> codigos.add(d.codigo()));
            int totalPaginas = sobre.meta() == null ? paginaActual : sobre.meta().totalPages();
            if (paginaActual >= totalPaginas) {
                return codigos;
            }
            if (paginaActual >= MAXIMO_PAGINAS_DE_BUSQUEDA) {
                LOG.warn("La búsqueda de instructores se cortó en el tope de coincidencias",
                    kv("module", "instructores"), kv("coincidenciasDevueltas", codigos.size()));
                return codigos;
            }
            pagina++;
        }
    }

    @Override
    public PersonaInstructor cambiarUbicacion(UUID usuarioCodigo, BigDecimal latitud, BigDecimal longitud) {
        SobrePersona sobre = TraductorDeErroresDeIdentidad.ejecutar(() -> restClient.patch()
            .uri(RUTA + "/{codigo}/ubicacion", usuarioCodigo)
            .header(HttpHeaders.AUTHORIZATION, TraductorDeErroresDeIdentidad.bearerDeLaSesion())
            .contentType(MediaType.APPLICATION_JSON)
            .body(new PeticionUbicacion(latitud, longitud))
            .retrieve()
            .body(SobrePersona.class));
        if (sobre == null || sobre.data() == null) {
            throw new IdentidadNoDisponibleException("Identidad respondió sin los datos del instructor");
        }
        return sobre.data().aDominio();
    }

    // * Cuerpos de las peticiones a Identidad. La fecha viaja como texto ISO (yyyy-MM-dd).
    private record PeticionCrear(UUID codigo, String nombres, String apellidos, String fechaNacimiento, String sexo,
            String ci, String telefono, String email, String password, BigDecimal latitud, BigDecimal longitud) {

        @Override
        public String toString() {
            return "PeticionCrear[codigo=" + codigo + "]";
        }
    }

    private record PeticionConsulta(List<UUID> codigos) {
    }

    private record PeticionUbicacion(BigDecimal latitud, BigDecimal longitud) {
    }

    // * Sobre estándar de Identidad ({status, data, meta, ...}); solo se leen estos campos
    private record SobrePersona(DatosPersona data) {
    }

    private record SobreListaPersonas(List<DatosPersona> data, Meta meta) {
    }

    private record Meta(int totalPages) {
    }

    // * Las fechas llegan como texto ISO y se convierten acá, sin depender de cómo configure Jackson java.time
    private record DatosPersona(UUID codigo, String nombres, String apellidos, String nombreCompleto, String fotoUrl,
            int edad, String fechaNacimiento, String sexo, String ci, String email, String telefono,
            String direccion, BigDecimal latitud, BigDecimal longitud, String fechaAlta) {

        PersonaInstructor aDominio() {
            return new PersonaInstructor(codigo, nombres, apellidos, nombreCompleto, fotoUrl, edad,
                fecha(fechaNacimiento), sexo, ci, email, telefono, direccion, latitud, longitud, fecha(fechaAlta));
        }

        private static LocalDate fecha(String texto) {
            return texto == null ? null : LocalDate.parse(texto);
        }
    }
}
