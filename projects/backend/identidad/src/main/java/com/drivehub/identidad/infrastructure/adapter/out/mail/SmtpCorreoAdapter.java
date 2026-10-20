package com.drivehub.identidad.infrastructure.adapter.out.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import com.drivehub.identidad.domain.port.out.EnviadorCorreoPort;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Envía los correos por SMTP: Mailpit en desarrollo, el SMTP del proveedor en
 * producción (solo cambian las variables `SMTP_*`).
 *
 * * Texto plano con una versión HTML simple; el enlace es lo único accionable.
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
        String html = "<p>Hola " + HtmlUtils.htmlEscape(nombres) + ",</p>"
            + "<p>Para verificar tu correo en DriveHub abre este enlace (sirve una sola vez y vence en 24 horas):</p>"
            + "<p><a href=\"" + HtmlUtils.htmlEscape(enlace) + "\">Verificar mi correo</a></p>"
            + "<p>Si no lo pediste, puedes ignorar este mensaje.</p>";
        enviar(destinatario, "Verifica tu correo en DriveHub", texto, html);
    }

    @Override
    public void enviarRecuperacionContrasena(String destinatario, String nombres, String enlace) {
        String texto = "Hola " + nombres + ",\n\n"
            + "Para elegir una contraseña nueva en DriveHub abre este enlace (sirve una sola vez y vence en 30 minutos):\n\n"
            + enlace + "\n\n"
            + "Si no lo pediste, puedes ignorar este mensaje: tu contraseña no cambia.\n";
        String html = "<p>Hola " + HtmlUtils.htmlEscape(nombres) + ",</p>"
            + "<p>Para elegir una contraseña nueva en DriveHub abre este enlace (sirve una sola vez y vence en 30 minutos):</p>"
            + "<p><a href=\"" + HtmlUtils.htmlEscape(enlace) + "\">Cambiar mi contraseña</a></p>"
            + "<p>Si no lo pediste, puedes ignorar este mensaje: tu contraseña no cambia.</p>";
        enviar(destinatario, "Recupera tu contraseña de DriveHub", texto, html);
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
