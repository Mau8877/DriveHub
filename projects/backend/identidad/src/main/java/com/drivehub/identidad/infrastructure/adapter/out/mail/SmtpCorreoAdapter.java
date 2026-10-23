package com.drivehub.identidad.infrastructure.adapter.out.mail;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import com.drivehub.identidad.domain.port.out.EnviadorCorreoPort;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Envía los correos por SMTP: Mailpit en desarrollo, el SMTP del proveedor en
 * producción (solo cambian las variables `SMTP_*`).
 *
 * * Texto plano más una versión HTML con la marca (ver {@link PlantillaCorreo}); el enlace es
 * * lo único accionable.
 * ! Nunca se loguea el enlace: es el secreto de un solo uso.
 */
@Component
public class SmtpCorreoAdapter implements EnviadorCorreoPort {

    private final JavaMailSender sender;
    private final String remitente;

    public SmtpCorreoAdapter(JavaMailSender sender, @Value("${app.correo.remitente}") String remitente) {
        this.sender = sender;
        this.remitente = remitente;
    }

    @Override
    public void enviarVerificacionCorreo(String destinatario, String nombres, String enlace) {
        String texto = "Hola " + nombres + ",\n\n"
            + "Para verificar tu correo en DriveHub abre este enlace (sirve una sola vez y vence en 24 horas):\n\n"
            + enlace + "\n\n"
            + "Si no lo pediste, puedes ignorar este mensaje.\n";
        String html = PlantillaCorreo.armar("Verifica tu correo", nombres,
            List.of("Para verificar tu correo en DriveHub toca el botón. El enlace sirve una sola vez y vence en 24 horas."),
            "Verificar mi correo", enlace, "Si no lo pediste, puedes ignorar este mensaje.");
        enviar(destinatario, "Verifica tu correo en DriveHub", texto, html);
    }

    @Override
    public void enviarRecuperacionContrasena(String destinatario, String nombres, String enlace) {
        String texto = "Hola " + nombres + ",\n\n"
            + "Para elegir una contraseña nueva en DriveHub abre este enlace (sirve una sola vez y vence en 30 minutos):\n\n"
            + enlace + "\n\n"
            + "Si no lo pediste, puedes ignorar este mensaje: tu contraseña no cambia.\n";
        String html = PlantillaCorreo.armar("Recupera tu contraseña", nombres,
            List.of("Para elegir una contraseña nueva en DriveHub toca el botón. El enlace sirve una sola vez y vence en 30 minutos."),
            "Cambiar mi contraseña", enlace, "Si no lo pediste, puedes ignorar este mensaje: tu contraseña no cambia.");
        enviar(destinatario, "Recupera tu contraseña de DriveHub", texto, html);
    }

    @Override
    public void enviarCambioCorreo(String correoNuevo, String nombres, String enlace) {
        String texto = "Hola " + nombres + ",\n\n"
            + "Alguien pidió cambiar el correo de tu cuenta de DriveHub a esta dirección. "
            + "Para confirmarlo abre este enlace (sirve una sola vez y vence en 24 horas):\n\n"
            + enlace + "\n\n"
            + "Si no lo pediste, puedes ignorar este mensaje: el correo de tu cuenta no cambia.\n";
        String html = PlantillaCorreo.armar("Confirma tu nuevo correo", nombres,
            List.of("Alguien pidió cambiar el correo de tu cuenta de DriveHub a esta dirección.",
                "Para confirmarlo toca el botón. El enlace sirve una sola vez y vence en 24 horas."),
            "Confirmar mi nuevo correo", enlace,
            "Si no lo pediste, puedes ignorar este mensaje: el correo de tu cuenta no cambia.");
        enviar(correoNuevo, "Confirma tu nuevo correo en DriveHub", texto, html);
    }

    @Override
    public void enviarAvisoCambioCorreo(String correoAnterior, String nombres, String correoNuevo) {
        String texto = "Hola " + nombres + ",\n\n"
            + "El correo de tu cuenta de DriveHub cambió a " + correoNuevo + ".\n\n"
            + "Si no fuiste tú, contacta a tu escuela.\n";
        String html = PlantillaCorreo.armar("El correo de tu cuenta cambió", nombres,
            List.of("El correo de tu cuenta de DriveHub cambió a " + correoNuevo + "."),
            null, null, "Si no fuiste tú, contacta a tu escuela.");
        enviar(correoAnterior, "El correo de tu cuenta de DriveHub cambió", texto, html);
    }

    private void enviar(String destinatario, String asunto, String texto, String html) {
        try {
            MimeMessage mensaje = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
            helper.setFrom(remitente);
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(texto, html);
            sender.send(mensaje);
        } catch (MessagingException e) {
            throw new IllegalStateException("No se pudo armar el correo", e);
        }
    }
}
