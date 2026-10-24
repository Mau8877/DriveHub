package com.drivehub.identidad.infrastructure.adapter.out.mail;

import java.util.List;

import org.springframework.web.util.HtmlUtils;

/**
 * Arma la versión HTML de todos los correos con la paleta de marca de DriveHub.
 *
 * * Es HTML de correo, no de web: tablas y estilos en línea (los clientes de correo ignoran
 * * las hojas de estilo y los `div` con flex), ancho de 600 px y sin imágenes externas, así se
 * * ve igual en Gmail, Outlook y el móvil sin que haya que "permitir imágenes".
 * * Colores: Gris Asfalto #1E293B (cabecera y texto), Ámbar Vial #F59E0B (botón y acentos),
 * * Azul Técnico oscurecido #0369A1 (enlaces), Gris Claro #F1F5F9 (fondo) y Gris Medio #475569.
 * * Todo texto del usuario se escapa; la versión de texto plano la arma el adaptador.
 */
final class PlantillaCorreo {

    private static final String ASFALTO = "#1E293B";
    private static final String AMBAR = "#F59E0B";
    private static final String AZUL = "#0369A1";
    private static final String FONDO = "#F1F5F9";
    private static final String MEDIO = "#475569";
    private static final String BORDE = "#E2E8F0";
    private static final String FUENTE = "'Segoe UI',Roboto,Helvetica,Arial,sans-serif";

    private PlantillaCorreo() {
    }

    /**
     * @param titulo      encabezado grande del correo
     * @param nombres     a quién se saluda ("Hola {nombres},")
     * @param parrafos    cuerpo, un párrafo por elemento (texto sin escapar)
     * @param etiquetaBoton texto del botón, o {@code null} si el correo no tiene acción
     * @param enlace      destino del botón, o {@code null} si no hay acción
     * @param nota        aviso final en un recuadro (por ejemplo "si no lo pediste..."), o {@code null}
     */
    static String armar(String titulo, String nombres, List<String> parrafos, String etiquetaBoton,
                        String enlace, String nota) {
        StringBuilder h = new StringBuilder(4096);
        h.append("<!DOCTYPE html><html lang=\"es\"><head><meta charset=\"UTF-8\">")
            .append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">")
            .append("<meta name=\"color-scheme\" content=\"light\"><title>").append(esc(titulo)).append("</title></head>")
            .append("<body style=\"margin:0;padding:0;background:").append(FONDO).append(";\">")
            // * Texto de vista previa de la bandeja de entrada
            .append("<div style=\"display:none;max-height:0;overflow:hidden;opacity:0;\">")
            .append(esc(titulo)).append("</div>")
            .append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" bgcolor=\"")
            .append(FONDO).append("\"><tr><td align=\"center\" style=\"padding:24px 12px;\">")
            .append("<table role=\"presentation\" width=\"600\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" ")
            .append("style=\"width:100%;max-width:600px;font-family:").append(FUENTE).append(";\">");

        cabecera(h);
        h.append("<tr><td bgcolor=\"#FFFFFF\" style=\"background:#FFFFFF;border:1px solid ").append(BORDE)
            .append(";border-top:0;border-radius:0 0 16px 16px;padding:32px 32px 28px;\">");

        h.append("<h1 style=\"margin:0 0 16px;font-size:22px;line-height:1.3;color:").append(ASFALTO)
            .append(";font-weight:800;\">").append(esc(titulo)).append("</h1>");
        h.append("<p style=\"margin:0 0 14px;font-size:15px;line-height:1.6;color:").append(ASFALTO).append(";\">Hola ")
            .append(esc(nombres)).append(",</p>");
        for (String parrafo : parrafos) {
            h.append("<p style=\"margin:0 0 14px;font-size:15px;line-height:1.6;color:").append(MEDIO).append(";\">")
                .append(esc(parrafo)).append("</p>");
        }

        if (enlace != null) {
            boton(h, etiquetaBoton, enlace);
        }
        if (nota != null) {
            h.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" ")
                .append("style=\"margin-top:20px;\"><tr><td bgcolor=\"").append(FONDO).append("\" style=\"background:")
                .append(FONDO).append(";border-left:4px solid ").append(AMBAR)
                .append(";border-radius:8px;padding:12px 16px;font-size:13px;line-height:1.5;color:").append(MEDIO)
                .append(";\">").append(esc(nota)).append("</td></tr></table>");
        }
        h.append("</td></tr>");

        h.append("<tr><td align=\"center\" style=\"padding:20px 16px 0;font-size:12px;line-height:1.5;color:")
            .append(MEDIO).append(";\">DriveHub · Plataforma para autoescuelas<br>")
            .append("Este correo se envió automáticamente: por favor no lo respondas.</td></tr>")
            .append("</table></td></tr></table></body></html>");
        return h.toString();
    }

    /** Franja de asfalto con la marca y una línea ámbar debajo. */
    private static void cabecera(StringBuilder h) {
        h.append("<tr><td bgcolor=\"").append(ASFALTO).append("\" style=\"background:").append(ASFALTO)
            .append(";border-radius:16px 16px 0 0;padding:20px 32px;\">")
            .append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr>")
            .append("<td width=\"36\" height=\"36\" align=\"center\" bgcolor=\"").append(AMBAR)
            .append("\" style=\"background:").append(AMBAR).append(";border-radius:10px;font-size:20px;")
            .append("font-weight:800;color:").append(ASFALTO).append(";font-family:").append(FUENTE)
            .append(";\">D</td>")
            .append("<td style=\"padding-left:12px;font-size:22px;font-weight:800;letter-spacing:-0.4px;color:#FFFFFF;")
            .append("font-family:").append(FUENTE).append(";\">Drive<span style=\"color:").append(AMBAR)
            .append(";\">Hub</span></td></tr></table></td></tr>")
            .append("<tr><td height=\"4\" bgcolor=\"").append(AMBAR).append("\" style=\"background:").append(AMBAR)
            .append(";font-size:0;line-height:0;\">&nbsp;</td></tr>");
    }

    /** Botón ámbar con texto de asfalto (con blanco no hay contraste) y el enlace en texto debajo. */
    private static void boton(StringBuilder h, String etiqueta, String enlace) {
        String destino = HtmlUtils.htmlEscape(enlace);
        h.append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"margin:22px 0 18px;\">")
            .append("<tr><td align=\"center\" bgcolor=\"").append(AMBAR).append("\" style=\"background:").append(AMBAR)
            .append(";border-radius:12px;\"><a href=\"").append(destino)
            .append("\" style=\"display:inline-block;padding:14px 28px;font-size:15px;font-weight:800;color:")
            .append(ASFALTO).append(";text-decoration:none;font-family:").append(FUENTE).append(";\">")
            .append(esc(etiqueta)).append("</a></td></tr></table>")
            .append("<p style=\"margin:0;font-size:12px;line-height:1.5;color:").append(MEDIO)
            .append(";\">Si el botón no funciona, copia y pega este enlace en tu navegador:<br>")
            .append("<a href=\"").append(destino).append("\" style=\"color:").append(AZUL)
            .append(";word-break:break-all;\">").append(destino).append("</a></p>");
    }

    private static String esc(String texto) {
        return HtmlUtils.htmlEscape(texto);
    }
}
