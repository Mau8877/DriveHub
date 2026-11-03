package com.drivehub.escuela.application.usecase;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.drivehub.escuela.application.dto.AutoDeInstructorOutput;
import com.drivehub.escuela.application.dto.ConsultarDisponibilidadInput;
import com.drivehub.escuela.application.dto.DisponibilidadInstructorOutput;
import com.drivehub.escuela.application.dto.PaginaOutput;
import com.drivehub.escuela.common.exception.EscuelaErrorCodes;
import com.drivehub.escuela.common.exception.ValidationException;
import com.drivehub.escuela.domain.entity.Disponibilidad;
import com.drivehub.escuela.domain.entity.EstadoAutos;
import com.drivehub.escuela.domain.entity.EstadoVehiculo;
import com.drivehub.escuela.domain.entity.Instructor;
import com.drivehub.escuela.domain.gateway.HorariosGateway;
import com.drivehub.escuela.domain.gateway.InstructorCuentaGateway;
import com.drivehub.escuela.domain.gateway.PersonaInstructor;
import com.drivehub.escuela.domain.gateway.ResumenFranjas;
import com.drivehub.escuela.domain.repository.AsignacionDeAuto;
import com.drivehub.escuela.domain.repository.AsignacionVehiculoRepository;
import com.drivehub.escuela.domain.repository.InstructorRepository;
import com.drivehub.escuela.domain.repository.Pagina;

import lombok.RequiredArgsConstructor;

/**
 * CU11: el Administrador de escuela consulta qué instructores de SU escuela pueden dar clase un día (y un momento
 * del día) y con qué autos cuentan.
 *
 * * La disponibilidad se calcula con las franjas de Académico, así que no se puede filtrar ni ordenar en la
 * * base: se reúnen las fichas de la escuela (hasta {@value #TOPE_INSTRUCTORES}), sus autos vigentes y el
 * * resumen de franjas, y recién con eso se filtra, se ordena y se corta la página.
 * * Los nombres se piden a Identidad solo para las filas de la página; la búsqueda por nombre usa
 * * `buscarCodigos`, igual que CU09. Una persona que Identidad no devuelve deja el nombre en `null` (WARN).
 * * Es de solo lectura y no abre una transacción propia: no se retiene una conexión mientras se espera a
 * * Académico o a Identidad. Sin log por lectura.
 */
@Service
@RequiredArgsConstructor
public class ConsultarDisponibilidadUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(ConsultarDisponibilidadUseCase.class);
    static final int TAMANO_POR_DEFECTO = 20;
    static final int TAMANO_MAXIMO = 100;
    static final int TOPE_INSTRUCTORES = 500;

    private final InstructorRepository instructorRepository;
    private final AsignacionVehiculoRepository asignacionRepository;
    private final HorariosGateway horariosGateway;
    private final InstructorCuentaGateway cuentaGateway;
    private final Clock reloj;

    // * Un instructor con todo lo que hace falta para filtrar, ordenar y mostrar (sin el nombre todavía)
    private record Fila(Instructor ficha, List<AsignacionDeAuto> autos, ResumenFranjas franjas,
            Disponibilidad disponibilidad, EstadoAutos estadoAutos) {
    }

    @PreAuthorize("hasRole('ADMIN_ESCUELA')")
    public PaginaOutput<DisponibilidadInstructorOutput> ejecutar(ConsultarDisponibilidadInput input) {
        LocalDate hoy = LocalDate.now(reloj);
        LocalDate fecha = input.fecha() == null ? hoy : input.fecha();
        if (fecha.isBefore(hoy)) {
            throw new ValidationException(EscuelaErrorCodes.VALIDACION, "La fecha no puede ser anterior a hoy");
        }
        int pagina = input.pagina() == null || input.pagina() < 1 ? 1 : input.pagina();
        int tamano = input.tamano() == null || input.tamano() < 1
            ? TAMANO_POR_DEFECTO : Math.min(input.tamano(), TAMANO_MAXIMO);
        String busqueda = input.busqueda() == null || input.busqueda().isBlank() ? null : input.busqueda().trim();

        Pagina<Instructor> fichas = instructorRepository.listar(input.escuelaCodigo(), null, null, 1,
            TOPE_INSTRUCTORES);
        if (fichas.total() > TOPE_INSTRUCTORES) {
            LOG.warn("La escuela tiene más instructores de los que se consultan",
                kv("module", "disponibilidad"), kv("instructores", fichas.total()), kv("tope", TOPE_INSTRUCTORES));
        }
        if (fichas.elementos().isEmpty()) {
            return new PaginaOutput<>(List.of(), pagina, tamano, 0, 0);
        }

        List<UUID> usuarios = fichas.elementos().stream().map(Instructor::getUsuarioCodigo).toList();
        Map<UUID, List<AsignacionDeAuto>> autosPorInstructor = asignacionRepository
            .vigentesDeInstructores(input.escuelaCodigo(), usuarios).stream()
            .collect(Collectors.groupingBy(AsignacionDeAuto::instructorUsuarioCodigo));
        Map<UUID, ResumenFranjas> resumen = horariosGateway.resumenDelDia(fecha, input.momento());
        Set<UUID> coincidenPorNombre = busqueda == null ? Set.of() : Set.copyOf(cuentaGateway.buscarCodigos(busqueda));
        String textoNormalizado = busqueda == null ? null : normalizar(busqueda);

        List<Fila> filas = fichas.elementos().stream()
            .map(ficha -> armarFila(ficha, autosPorInstructor.getOrDefault(ficha.getUsuarioCodigo(), List.of()),
                resumen.getOrDefault(ficha.getUsuarioCodigo(), ResumenFranjas.VACIO)))
            .filter(fila -> input.disponibilidad() == null || fila.disponibilidad() == input.disponibilidad())
            .filter(fila -> input.estadoAutos() == null || fila.estadoAutos() == input.estadoAutos())
            .filter(fila -> textoNormalizado == null || coincide(fila, coincidenPorNombre, textoNormalizado))
            // * La ordenación es estable: a igualdad queda el orden del repositorio (alta más reciente primero)
            .sorted(Comparator.comparingInt((Fila f) -> f.disponibilidad().orden())
                .thenComparing(Comparator.comparingInt((Fila f) -> f.franjas().libres()).reversed()))
            .toList();

        long total = filas.size();
        int desde = (int) Math.min((long) (pagina - 1) * tamano, total);
        List<Fila> deLaPagina = filas.subList(desde, (int) Math.min(desde + (long) tamano, total));

        Map<UUID, PersonaInstructor> personas = deLaPagina.isEmpty() ? Map.of()
            : cuentaGateway.consultar(deLaPagina.stream().map(f -> f.ficha().getUsuarioCodigo()).toList())
                .stream().collect(Collectors.toMap(PersonaInstructor::codigo, Function.identity(), (a, b) -> a));

        List<DisponibilidadInstructorOutput> elementos = deLaPagina.stream().map(fila -> {
            PersonaInstructor persona = personas.get(fila.ficha().getUsuarioCodigo());
            if (persona == null) {
                LOG.warn("Identidad no devolvió a la persona de un instructor",
                    kv("module", "disponibilidad"), kv("instructorCodigo", fila.ficha().getCodigo()));
            }
            return aSalida(fila, persona);
        }).toList();

        int totalPaginas = (int) Math.ceil((double) total / tamano);
        return new PaginaOutput<>(elementos, pagina, tamano, total, totalPaginas);
    }

    private static Fila armarFila(Instructor ficha, List<AsignacionDeAuto> autos, ResumenFranjas franjas) {
        List<EstadoVehiculo> estados = autos.stream().map(a -> a.vehiculo().getEstado()).toList();
        return new Fila(ficha, autos, franjas,
            Disponibilidad.de(ficha.getEstado(), franjas.total(), franjas.libres()), EstadoAutos.de(estados));
    }

    // * Coincide por el nombre (lo resolvió Identidad) o por la placa, marca o modelo de alguno de sus autos
    private static boolean coincide(Fila fila, Collection<UUID> coincidenPorNombre, String texto) {
        if (coincidenPorNombre.contains(fila.ficha().getUsuarioCodigo())) {
            return true;
        }
        return fila.autos().stream().anyMatch(a -> normalizar(a.vehiculo().getPlaca()).contains(texto)
            || normalizar(a.vehiculo().getMarca()).contains(texto)
            || normalizar(a.vehiculo().getModelo()).contains(texto));
    }

    // * Sin mayúsculas ni tildes, para que «José» y «jose» sean lo mismo
    private static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    private static DisponibilidadInstructorOutput aSalida(Fila fila, PersonaInstructor persona) {
        List<AutoDeInstructorOutput> autos = fila.autos().stream()
            .map(a -> new AutoDeInstructorOutput(a.vehiculo().getCodigo(), a.vehiculo().getPlaca(),
                a.vehiculo().getMarca(), a.vehiculo().getModelo(), a.vehiculo().getEstado()))
            .toList();
        return new DisponibilidadInstructorOutput(fila.ficha().getCodigo(),
            persona == null ? null : persona.nombreCompleto(), fila.ficha().getEstado(), fila.disponibilidad(),
            fila.estadoAutos(), autos, fila.franjas().libres(), fila.franjas().reservadas(),
            fila.franjas().bloqueadas());
    }
}
