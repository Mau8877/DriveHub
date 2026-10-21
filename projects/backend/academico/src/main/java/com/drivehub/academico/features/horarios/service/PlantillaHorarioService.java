package com.drivehub.academico.features.horarios.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
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
import com.drivehub.academico.features.horarios.model.PlantillaHorario;
import com.drivehub.academico.features.horarios.repository.FranjaRepository;
import com.drivehub.academico.features.horarios.repository.PlantillaHorarioRepository;

import lombok.RequiredArgsConstructor;

/**
 * Horarios semanales del instructor (CU12): crear, listar y desactivar.
 *
 * * El instructor siempre es el de la sesión: un horario de otro responde "no encontrado", no "prohibido",
 * * para no revelar que existe.
 * * Sin transacciones (Académico no tiene MongoTransactionManager): crear es repetible porque la generación de
 * * franjas es idempotente; desactivar borra primero las franjas y marca el horario inactivo al final, así
 * * que si falla a medias el horario sigue activo y se puede repetir.
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

        validarVentana(inicio, fin, duracion);
        validarVigencia(desde, hasta, hoy);
        validarQueNoSeCruce(instructorCodigo, request.diaSemana(), inicio, fin, desde, hasta);

        PlantillaHorario horario = plantillaRepository.save(new PlantillaHorario(escuelaCodigo, instructorCodigo,
            request.diaSemana(), request.horaInicio(), request.horaFin(), duracion, desde, hasta));
        int franjasGeneradas = generacion.generar(horario);

        LOG.info("Horario creado", kv("module", MODULO), kv("instructorCodigo", instructorCodigo),
            kv("plantillaCodigo", horario.getCodigoPlantillaHorario()), kv("franjasGeneradas", franjasGeneradas));
        return new PlantillaHorarioCreadaResponse(horario.getCodigoPlantillaHorario(), horario.getDiaSemana(),
            horario.getHoraInicio(), horario.getHoraFin(), horario.getDuracionClaseMin(), horario.getVigenteDesde(),
            horario.getVigenteHasta(), horario.isActiva(), franjasGeneradas);
    }

    @PreAuthorize("hasRole('INSTRUCTOR')")
    public Page<PlantillaHorarioResponse> listar(UUID instructorCodigo, Boolean activa, int page, int pageSize) {
        var pagina = Paginacion.de(page, pageSize, Sort.by("diaSemana", "horaInicio"));
        Page<PlantillaHorario> horarios = activa == null
            ? plantillaRepository.findByInstructorCodigoAndDeletedAtIsNull(instructorCodigo, pagina)
            : plantillaRepository.findByInstructorCodigoAndActivaAndDeletedAtIsNull(instructorCodigo, activa, pagina);
        return horarios.map(PlantillaHorarioService::aRespuesta);
    }

    @PreAuthorize("hasRole('INSTRUCTOR')")
    public void desactivar(UUID instructorCodigo, UUID plantillaCodigo) {
        PlantillaHorario horario = plantillaRepository
            .findByCodigoPlantillaHorarioAndInstructorCodigoAndDeletedAtIsNull(plantillaCodigo, instructorCodigo)
            .orElseThrow(() -> new NotFoundException(AcademicoErrorCodes.HORARIO_NO_ENCONTRADO,
                "El horario no existe"));
        if (!horario.isActiva()) {
            throw new ConflictException(AcademicoErrorCodes.HORARIO_YA_INACTIVO, "El horario ya está desactivado");
        }

        LocalDateTime ahora = LocalDateTime.now(clock);
        long eliminadas = franjaRepository.eliminarFuturasNoReservadas(horario.getId(), ahora.toLocalDate(),
            ahora.toLocalTime().format(HORA));
        // * Al final: si el borrado falla, el horario sigue activo y la operación se puede repetir
        horario.setActiva(false);
        plantillaRepository.save(horario);

        LOG.info("Horario desactivado", kv("module", MODULO), kv("instructorCodigo", instructorCodigo),
            kv("plantillaCodigo", plantillaCodigo), kv("franjasEliminadas", eliminadas));
    }

    private static PlantillaHorarioResponse aRespuesta(PlantillaHorario h) {
        return new PlantillaHorarioResponse(h.getCodigoPlantillaHorario(), h.getDiaSemana(), h.getHoraInicio(),
            h.getHoraFin(), h.getDuracionClaseMin(), h.getVigenteDesde(), h.getVigenteHasta(), h.isActiva());
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
     * Cruce: mismo día, ventanas de hora que se solapan y vigencias que se solapan. Que una ventana termine
     * a la hora en que empieza la otra no es solaparse.
     */
    private void validarQueNoSeCruce(UUID instructorCodigo, int diaSemana, LocalTime inicio, LocalTime fin,
            LocalDate desde, LocalDate hasta) {
        LocalDate hastaNuevo = hasta != null ? hasta : LocalDate.MAX;
        for (PlantillaHorario otro : plantillaRepository
                .findByInstructorCodigoAndDiaSemanaAndActivaTrueAndDeletedAtIsNull(instructorCodigo, diaSemana)) {
            LocalDate hastaOtro = otro.getVigenteHasta() != null ? otro.getVigenteHasta() : LocalDate.MAX;
            boolean seSolapanHoras = inicio.isBefore(LocalTime.parse(otro.getHoraFin()))
                && LocalTime.parse(otro.getHoraInicio()).isBefore(fin);
            boolean seSolapanVigencias = !desde.isAfter(hastaOtro) && !otro.getVigenteDesde().isAfter(hastaNuevo);
            if (seSolapanHoras && seSolapanVigencias) {
                throw new ConflictException(AcademicoErrorCodes.HORARIO_SE_CRUZA,
                    "El horario se cruza con otro horario activo tuyo");
            }
        }
    }
}
