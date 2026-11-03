package com.drivehub.academico.features.reservas.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.drivehub.academico.common.notificaciones.NotificacionesClient;
import com.drivehub.academico.features.reservas.model.Reserva;

import lombok.RequiredArgsConstructor;

/**
 * Avisa a quien corresponda cuando una clase se confirma, se cancela o se reprograma (CU36).
 *
 * * Se llama DESPUÉS de que el cambio de estado se aplicó (el repositorio devuelve la reserva ya cambiada), y solo
 * * cuando el cambio realmente ocurrió: una operación repetida no vuelve a avisar.
 * * Nunca propaga un error: el aviso es de mejor esfuerzo y no puede deshacer ni frenar la saga de la clase.
 * * Cada aviso usa un `eventoCodigo` determinista por (evento, reserva, destinatario), así repetirlo no lo duplica.
 * * Una cancelación de una reserva que nunca llegó a confirmarse (`PENDIENTE_PAGO`) no avisa al instructor, que no
 * * llegó a saber de ella.
 * ! Los códigos de la clase viajan en `datos`; nunca ids internos ni datos de pago.
 */
@Component
@RequiredArgsConstructor
public class AvisosReserva {

    private static final Logger LOG = LoggerFactory.getLogger(AvisosReserva.class);
    private static final String TIPO = "RESERVA";
    private static final String TIPO_RECORDATORIO = "RECORDATORIO";

    private final NotificacionesClient notificaciones;

    /** La clase quedó confirmada: avisa al estudiante y al instructor de la franja. */
    public void claseConfirmada(Reserva reserva) {
        String cuando = cuando(reserva);
        avisar("clase.confirmada", reserva, reserva.getEstudianteCodigo(), "Clase reservada",
            "Tu clase del " + cuando + " quedó confirmada.");
        avisar("clase.confirmada", reserva, reserva.getInstructorCodigo(), "Nueva clase reservada",
            "Un estudiante reservó una clase contigo el " + cuando + ".");
    }

    /**
     * La reserva quedó cancelada.
     *
     * @param conoceElInstructor falso si la reserva nunca se confirmó, así que el instructor no se entera
     */
    public void claseCancelada(Reserva reserva, boolean conoceElInstructor) {
        String cuando = cuando(reserva);
        if (reserva.isCanceladaPorInstructor()) {
            avisar("clase.cancelada", reserva, reserva.getEstudianteCodigo(), "Tu instructor canceló la clase",
                "Tu clase del " + cuando + " fue cancelada por tu instructor y no se te descontó la hora.");
            return;
        }
        if (!conoceElInstructor) {
            avisar("clase.no-completada", reserva, reserva.getEstudianteCodigo(), "No pudimos reservar tu clase",
                "Tu reserva del " + cuando + " no se pudo completar. Tu hora fue devuelta.");
            return;
        }
        avisar("clase.cancelada", reserva, reserva.getEstudianteCodigo(), "Clase cancelada",
            "Tu clase del " + cuando + " fue cancelada.");
        avisar("clase.cancelada", reserva, reserva.getInstructorCodigo(), "Clase cancelada",
            "El estudiante canceló la clase del " + cuando + ".");
    }

    /** La clase se movió a otra franja: avisa al estudiante y al instructor de la franja nueva. */
    public void claseReprogramada(Reserva reserva) {
        String cuando = cuando(reserva);
        avisar("clase.reprogramada", reserva, reserva.getEstudianteCodigo(), "Clase reprogramada",
            "Tu clase se movió al " + cuando + ".");
        avisar("clase.reprogramada", reserva, reserva.getInstructorCodigo(), "Clase reprogramada",
            "Tienes una clase reprogramada para el " + cuando + ".");
    }

    /** Falta poco para la clase: recuerda al estudiante y al instructor. */
    public void recordatorio(Reserva reserva) {
        String cuando = cuando(reserva);
        avisar(TIPO_RECORDATORIO, "clase.recordatorio", reserva, reserva.getEstudianteCodigo(), "Recuerda tu clase",
            "Tienes una clase el " + cuando + ".");
        avisar(TIPO_RECORDATORIO, "clase.recordatorio", reserva, reserva.getInstructorCodigo(), "Recuerda tu clase",
            "Tienes una clase el " + cuando + ".");
    }

    private static String cuando(Reserva reserva) {
        return reserva.getFecha() + " a las " + reserva.getHoraInicio();
    }

    private void avisar(String evento, Reserva reserva, UUID destinatario, String titulo, String mensaje) {
        avisar(TIPO, evento, reserva, destinatario, titulo, mensaje);
    }

    private void avisar(String tipo, String evento, Reserva reserva, UUID destinatario, String titulo,
            String mensaje) {
        if (destinatario == null) {
            return;
        }
        try {
            // * La reprogramación puede repetirse sobre la misma reserva: la fecha nueva distingue un aviso de otro
            String clave = reserva.getCodigoReserva() + ":" + destinatario + ":" + reserva.getFecha() + ":"
                + reserva.getHoraInicio();
            notificaciones.avisar(NotificacionesClient.eventoCodigo(evento, UUID.nameUUIDFromBytes(clave.getBytes())),
                destinatario, tipo, titulo, mensaje,
                Map.of("reservaCodigo", reserva.getCodigoReserva().toString(), "evento", evento));
        } catch (RuntimeException e) {
            LOG.warn("No se pudo armar un aviso de clase (tipo {})", e.getClass().getSimpleName(),
                kv("module", "reservas"), kv("reservaCodigo", reserva.getCodigoReserva()), kv("evento", evento));
        }
    }
}
