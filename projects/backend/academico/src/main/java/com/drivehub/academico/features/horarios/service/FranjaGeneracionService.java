package com.drivehub.academico.features.horarios.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.drivehub.academico.config.HorariosProperties;
import com.drivehub.academico.features.horarios.model.Franja;
import com.drivehub.academico.features.horarios.model.PlantillaHorario;
import com.drivehub.academico.features.horarios.repository.FranjaRepository;
import com.drivehub.academico.features.horarios.repository.PlantillaHorarioRepository;

import lombok.RequiredArgsConstructor;

/**
 * Genera las franjas de un horario y mantiene al día las de todos los horarios activos (CU12).
 *
 * * Es idempotente: una franja que ya existe (mismo instructor, fecha y hora) no se duplica ni falla.
 * * Por eso crear un horario no necesita una transacción y repetir la generación es seguro.
 * * Sin @PreAuthorize: también lo ejecuta el sistema (ExtensionFranjasJob), no solo una persona.
 */
@Service
@RequiredArgsConstructor
public class FranjaGeneracionService {

    private static final Logger LOG = LoggerFactory.getLogger(FranjaGeneracionService.class);
    private static final String MODULO = "horarios";
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final int LOTE_HORARIOS = 100;

    private final GeneradorFranjas generador;
    private final FranjaRepository franjaRepository;
    private final PlantillaHorarioRepository plantillaRepository;
    private final Clock clock;
    private final HorariosProperties propiedades;

    /**
     * Genera las franjas que le corresponden al horario y devuelve cuántas realmente se insertaron
     * (las que ya existían no cuentan).
     */
    public int generar(PlantillaHorario horario) {
        LocalDateTime ahora = LocalDateTime.now(clock);
        List<FranjaCalculada> calculadas = generador.calcular(horario.getDiaSemana(),
            LocalTime.parse(horario.getHoraInicio()), LocalTime.parse(horario.getHoraFin()),
            horario.getDuracionClaseMin(), horario.getVigenteDesde(), horario.getVigenteHasta(), ahora,
            propiedades.horizonteDias());

        int insertadas = 0;
        for (FranjaCalculada calculada : calculadas) {
            Franja franja = new Franja(horario.getEscuelaCodigo(), horario.getId(), horario.getInstructorCodigo(),
                calculada.fecha(), calculada.horaInicio().format(HORA), calculada.horaFin().format(HORA));
            if (franjaRepository.insertarSiNoExiste(franja)) {
                insertadas++;
            }
        }
        return insertadas;
    }

    /**
     * Completa las franjas que faltan de todos los horarios activos de todos los instructores.
     * Un horario que falla se registra y no detiene a los demás.
     */
    public ResumenExtension extenderHorariosActivos() {
        LocalDate hoy = LocalDate.now(clock);
        int procesados = 0;
        int creadas = 0;
        int conError = 0;

        // * Ordenado por _id para que las páginas sean estables mientras se recorre
        Page<PlantillaHorario> pagina = plantillaRepository.findByActivaTrueAndDeletedAtIsNull(
            Paginacion.de(1, LOTE_HORARIOS, Sort.by("id")));
        int numero = 1;
        while (true) {
            for (PlantillaHorario horario : pagina.getContent()) {
                if (horario.getVigenteHasta() != null && horario.getVigenteHasta().isBefore(hoy)) {
                    continue;
                }
                procesados++;
                try {
                    creadas += generar(horario);
                } catch (RuntimeException e) {
                    conError++;
                    LOG.error("No se pudieron generar las franjas de un horario", kv("module", MODULO),
                        kv("plantillaCodigo", horario.getCodigoPlantillaHorario()), e);
                }
            }
            if (!pagina.hasNext()) {
                break;
            }
            numero++;
            pagina = plantillaRepository.findByActivaTrueAndDeletedAtIsNull(
                Paginacion.de(numero, LOTE_HORARIOS, Sort.by("id")));
        }

        ResumenExtension resumen = new ResumenExtension(procesados, creadas, conError);
        LOG.info("Generación diaria de franjas terminada", kv("module", MODULO),
            kv("horariosProcesados", procesados), kv("franjasCreadas", creadas), kv("horariosConError", conError));
        return resumen;
    }
}
