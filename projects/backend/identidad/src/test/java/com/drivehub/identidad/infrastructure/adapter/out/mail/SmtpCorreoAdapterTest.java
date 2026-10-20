package com.drivehub.identidad.infrastructure.adapter.out.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

@ExtendWith(MockitoExtension.class)
class SmtpCorreoAdapterTest {

    private static final String ENLACE = "http://localhost:4200/cuenta/verificar-correo?token=abc123";

    @Mock
    private JavaMailSender sender;

    private SmtpCorreoAdapter adaptador;

    @BeforeEach
    void crearAdaptador() {
        when(sender.createMimeMessage()).thenAnswer(i -> new MimeMessage(Session.getInstance(new Properties())));
        adaptador = new SmtpCorreoAdapter(sender, "no-reply@drivehub.local");
    }

    /** Junta el texto decodificado de todas las partes del mensaje. */
    private static String cuerpo(Part parte) throws Exception {
        Object contenido = parte.getContent();
        if (contenido instanceof Multipart multipart) {
            StringBuilder todo = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++) {
                todo.append(cuerpo(multipart.getBodyPart(i)));
            }
            return todo.toString();
        }
        return contenido.toString();
    }

    @Test
    void enviarVerificacionCorreo_armaElMensajeConDestinatarioRemitenteAsuntoYEnlace() throws Exception {
        adaptador.enviarVerificacionCorreo("ana@gmail.com", "Ana", ENLACE);

        ArgumentCaptor<MimeMessage> enviado = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(enviado.capture());
        MimeMessage mensaje = enviado.getValue();
        assertThat(((InternetAddress) mensaje.getRecipients(Message.RecipientType.TO)[0]).getAddress())
            .isEqualTo("ana@gmail.com");
        assertThat(((InternetAddress) mensaje.getFrom()[0]).getAddress()).isEqualTo("no-reply@drivehub.local");
        assertThat(mensaje.getSubject()).isEqualTo("Verifica tu correo en DriveHub");
        assertThat(cuerpo(mensaje)).contains("Hola Ana").contains(ENLACE);
    }

    @Test
    void enviarVerificacionCorreo_escapaElNombreEnLaVersionHtml() throws Exception {
        adaptador.enviarVerificacionCorreo("ana@gmail.com", "<b>Ana</b>", ENLACE);

        ArgumentCaptor<MimeMessage> enviado = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(enviado.capture());
        assertThat(cuerpo(enviado.getValue())).contains("&lt;b&gt;Ana&lt;/b&gt;");
    }

    @Test
    void enviarVerificacionCorreo_siFallaElEnvio_laExcepcionSePropaga() {
        doThrow(new MailSendException("SMTP caido")).when(sender).send(org.mockito.ArgumentMatchers.any(MimeMessage.class));

        assertThatThrownBy(() -> adaptador.enviarVerificacionCorreo("ana@gmail.com", "Ana", ENLACE))
            .isInstanceOf(MailSendException.class);
    }
}
