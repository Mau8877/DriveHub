package com.drivehub.academico.features.horarios.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.drivehub.academico.common.exception.AcademicoErrorCodes;
import com.drivehub.academico.common.exception.ConflictException;
import com.drivehub.academico.common.exception.NotFoundException;
import com.drivehub.academico.common.exception.ValidationException;
import com.drivehub.academico.features.horarios.dto.CrearPlantillaHorarioRequest;
import com.drivehub.academico.features.horarios.dto.PlantillaHorarioCreadaResponse;
import com.drivehub.academico.features.horarios.dto.PlantillaHorarioResponse;
import com.drivehub.academico.features.horarios.enums.EstadoPlantilla;
import com.drivehub.academico.features.horarios.model.PlantillaHorario;
import com.drivehub.academico.features.horarios.repository.FranjaRepository;
import com.drivehub.academico.features.horarios.repository.PlantillaHorarioRepository;

import lombok.RequiredArgsConstructor;

/**
 * Horarios semanales del instructor (CU12): crear, listar, pausar, reanudar y desactivar.
 *
 * * El instructor siempre es el de la sesión: un horario de otro responde "no encontrado", no "prohibido",
 * * para no revelar que existe.
 * * Sin transacciones (Académico no tiene MongoTransactionManager): crear y reanudar son repetibles porque la
 * * generación de franjas es idempotente; pausar y desactivar borran primero las franjas y cambian el estado
 * * al final, así que si falla a medias el horario conserva su estado y se puede repetir.
 * ! No se loguean datos sensibles; solo códigos.
 */
@Service
@RequiredArgsConstructor
public class PlantillaHorarioService {

    static final int DURACION_MINIMA = 15;
    static final int DURACION_MAXIMA = 240;

    private static final Logger LOG = LoggerFactory.getLogger(PlantillaHorarioService.class);
    private static final String MODULO = "horarios";
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final PlantillaHorarioRepository plantillaRepository;
    private final FranjaRepository franjaRepository;
    private final FranjaGeneracionService generacion;
    private final Clock clock;

    @PreAuthorize("hasRole('INSTRUCTOR')")
    public PlantillaHorarioCreadaResponse crear(UUID instructorCodigo, UUID escuelaCodigo,
            CrearPlantillaHorarioRequest request) {
        LocalTime inicio = LocalTime.parse(request.horaInicio());
        LocalTime fin = LocalTime.parse(request.horaFin());
        int duracion = request.duracionClaseMin();
        LocalDate hoy = LocalDate.now(clock);
        LocalDate desde = request.vigenteDesde() != null ? request.vigenteDesde() : hoy;
        LocalDate hasta = request.vigenteHasta();
        List<Integer> dias = request.diasSemana().stream().distinct().sorted().toList();

        validarVentana(inicio, fin, duracion);
        validarVigencia(desde, hasta, hoy);
        validarQueNoSeCruce(instructorCodigo, dias, inicio, fin, desde, hasta);

        PlantillaHorario horario = plantillaRepository.save(new PlantillaHorario(escuelaCodigo, instructorCodigo,
            request.nombre().trim(), dias, request.horaInicio(), request.horaFin(), duracion, desde, hasta));
        int franjasGeneradas = generacion.generar(horario);

        LOG.info("Horario creado", kv("module", MODULO), kv("instructorCodigo", instructorCodigo),
            kv("plantillaCodigo", horario.getCodigoPlantillaHorario()), kv("franjasGeneradas", franjasGeneradas));
        return new PlantillaHorarioCreadaResponse(horario.getCodigoPlantillaHorario(), horario.getNombre(),
            horario.getDiasSemana(), horario.getHoraInicio(), horario.getHoraFin(), horario.getDuracionClaseMin(),
            horario.getVigenteDesde(), horario.getVigenteHasta(), horario.getEstado(), franjasGeneradas);
    }

    /**
     * Los horarios del instructor en los estados pedidos (sin estados: activos y pausados), ordenados por
     * estado (activos, pausados, inactivos), primer día de la semana y hora de inicio.
     *
     * * Se ordena y se pagina en memoria: un instructor tiene pocos horarios y el orden por estado no sale
     * * del orden alfabético que daría la base.
     */
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public Page<PlantillaHorarioResponse> listar(UUID instructorCodigo, Set<EstadoPlantilla> estados, int page,
            int pageSize) {
        Set<EstadoPlantilla> pedidos = estados == null || estados.isEmpty()
            ? EnumSet.of(EstadoPlantilla.ACTIVA, EstadoPlantilla.PAUSADA) : estados;
        List<PlantillaHorario> todos = new ArrayList<>(plantillaRepository
            .findByInstructorCodigoAndEstadoInAndDeletedAtIsNull(instructorCodigo, pedidos));
        todos.sort(Comparator.comparing(PlantillaHorario::getEstado)
            .thenComparingInt(h -> h.getDiasSemana().get(0))
            .thenComparing(PlantillaHorario::getHoraInicio));

        var pagina = Paginacion.de(page, pageSize, Sort.unsorted());
        int desde = (int) Math.min(pagina.getOffset(), todos.size());
        int hasta = Math.min(desde + pagina.getPageSize(), todos.size());
        List<PlantillaHorarioResponse> contenido = todos.subList(desde, hasta).stream()
            .map(PlantillaHorarioService::aRespuesta).toList();
        return new PageImpl<>(contenido, pagina, todos.size());
    }

    @PreAuthorize("hasRole('INSTRUCTOR')")
    public void pausar(UUID instructorCodigo, UUID plantillaCodigo) {
        PlantillaHorario horario = buscar(instructorCodigo, plantillaCodigo);
        if (horario.getEstado() != EstadoPlantilla.ACTIVA) {
            throw new ConflictException(AcademicoErrorCodes.HORARIO_NO_ESTA_ACTIVO,
                "Solo se puede pausar un horario activo");
        }

        long eliminadas = eliminarFranjasFuturas(horario);
        // * Al final: si el borrado falla, el horario sigue activo y la operación se puede repetir
        horario.setEstado(EstadoPlantilla.PAUSADA);
        plantillaRepository.save(horario);

        LOG.info("Horario pausado", kv("module", MODULO), kv("instructorCodigo", instructorCodigo),
            kv("plantillaCodigo", plantillaCodigo), kv("franjasEliminadas", eliminadas));
    }

    /**
     * Vuelve a activar un horario pausado. En la pausa pudo crearse otro horario activo que choque, así que
     * se vuelve a comprobar el cruce. El estado cambia antes de generar: si la generación falla, el horario
     * ya quedó activo y la tarea diaria completa las franjas que falten.
     */
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public void reanudar(UUID instructorCodigo, UUID plantillaCodigo) {
        PlantillaHorario horario = buscar(instructorCodigo, plantillaCodigo);
        if (horario.getEstado() != EstadoPlantilla.PAUSADA) {
            throw new ConflictException(AcademicoErrorCodes.HORARIO_NO_ESTA_PAUSADO,
                "Solo se puede reanudar un horario pausado");
        }
        validarQueNoSeCruce(instructorCodigo, horario.getDiasSemana(), LocalTime.parse(horario.getHoraInicio()),
            LocalTime.parse(horario.getHoraFin()), horario.getVigenteDesde(), horario.getVigenteHasta());

        horario.setEstado(EstadoPlantilla.ACTIVA);
        plantillaRepository.save(horario);
        int franjasGeneradas = generacion.generar(horario);

        LOG.info("Horario reanudado", kv("module", MODULO), kv("instructorCodigo", instructorCodigo),
            kv("plantillaCodigo", plantillaCodigo), kv("franjasGeneradas", franjasGeneradas));
    }

    @PreAuthorize("hasRole('INSTRUCTOR')")
    public void desactivar(UUID instructorCodigo, UUID plantillaCodigo) {
        PlantillaHorario horario = buscar(instructorCodigo, plantillaCodigo);
        if (horario.getEstado() == EstadoPlantilla.INACTIVA) {
            throw new ConflictException(AcademicoErrorCodes.HORARIO_YA_INACTIVO, "El horario ya está desactivado");
        }

        long eliminadas = eliminarFranjasFuturas(horario);
        // * Al final: si el borrado falla, el horario conserva su estado y la operación se puede repetir
        horario.setEstado(EstadoPlantilla.INACTIVA);
        plantillaRepository.save(horario);

        LOG.info("Horario desactivado", kv("module", MODULO), kv("instructorCodigo", instructorCodigo),
            kv("plantillaCodigo", plantillaCodigo), kv("franjasEliminadas", eliminadas));
    }

    private PlantillaHorario buscar(UUID instructorCodigo, UUID plantillaCodigo) {
        return plantillaRepository
            .findByCodigoPlantillaHorarioAndInstructorCodigoAndDeletedAtIsNull(plantillaCodigo, instructorCodigo)
            .orElseThrow(() -> new NotFoundException(AcademicoErrorCodes.HORARIO_NO_ENCONTRADO,
                "El horario no existe"));
    }

    private long eliminarFranjasFuturas(PlantillaHorario horario) {
        LocalDateTime ahora = LocalDateTime.now(clock);
        return franjaRepository.eliminarFuturasNoReservadas(horario.getId(), ahora.toLocalDate(),
            ahora.toLocalTime().format(HORA));
    }

    private static PlantillaHorarioResponse aRespuesta(PlantillaHorario h) {
        return new PlantillaHorarioResponse(h.getCodigoPlantillaHorario(), h.getNombre(), h.getDiasSemana(),
            h.getHoraInicio(), h.getHoraFin(), h.getDuracionClaseMin(), h.getVigenteDesde(), h.getVigenteHasta(),
            h.getEstado());
    }

    private static void validarVentana(LocalTime inicio, LocalTime fin, int duracion) {
        if (!fin.isAfter(inicio)) {
            throw new ValidationException(AcademicoErrorCodes.HORA_FIN_NO_POSTERIOR,
                "La hora de fin debe ser posterior a la de inicio");
        }
        if (duracion < DURACION_MINIMA || duracion > DURACION_MAXIMA) {
            throw new ValidationException(AcademicoErrorCodes.DURACION_INVALIDA,
                "La duración de la clase debe estar entre " + DURACION_MINIMA + " y " + DURACION_MAXIMA + " minutos");
        }
        int tramo = (fin.getHour() * 60 + fin.getMinute()) - (inicio.getHour() * 60 + inicio.getMinute());
        if (tramo % duracion != 0) {
            throw new ValidationException(AcademicoErrorCodes.DURACION_INVALIDA,
                "El tramo entre la hora de inicio y la de fin debe ser un múltiplo de la duración de la clase");
        }
    }

    private static void validarVigencia(LocalDate desde, LocalDate hasta, LocalDate hoy) {
        if (desde.isBefore(hoy)) {
            throw new ValidationException(AcademicoErrorCodes.VIGENCIA_INVALIDA,
                "El horario no puede empezar a regir antes de hoy");
        }
        if (hasta != null && hasta.isBefore(desde)) {
            throw new ValidationException(AcademicoErrorCodes.VIGENCIA_INVALIDA,
                "La fecha en que termina el horario no puede ser anterior a la de inicio");
        }
    }

    /**
     * Cruce con los horarios activos del instructor: algún día en común, ventanas de hora que se solapan y
     * vigencias que se solapan. Que una ventana termine a la hora en que empieza la otra no es solaparse.
     * Los pausados y los inactivos no cuentan.
     */
    private void validarQueNoSeCruce(UUID instructorCodigo, Collection<Integer> dias, LocalTime inicio,
            LocalTime fin, LocalDate desde, LocalDate hasta) {
        LocalDate hastaNuevo = hasta != null ? hasta : LocalDate.MAX;
        for (PlantillaHorario otro : plantillaRepository.findByInstructorCodigoAndEstadoAndDeletedAtIsNull(
                instructorCodigo, EstadoPlantilla.ACTIVA)) {
            boolean compartenDia = otro.getDiasSemana().stream().anyMatch(dias::contains);
            LocalDate hastaOtro = otro.getVigenteHasta() != null ? otro.getVigenteHasta() : LocalDate.MAX;
            boolean seSolapanHoras = inicio.isBefore(LocalTime.parse(otro.getHoraFin()))
                && LocalTime.parse(otro.getHoraInicio()).isBefore(fin);
            boolean seSolapanVigencias = !desde.isAfter(hastaOtro) && !otro.getVigenteDesde().isAfter(hastaNuevo);
            if (compartenDia && seSolapanHoras && seSolapanVigencias) {
                throw new ConflictException(AcademicoErrorCodes.HORARIO_SE_CRUZA,
                    "El horario se cruza con otro horario activo tuyo");
            }
        }
    }
}
