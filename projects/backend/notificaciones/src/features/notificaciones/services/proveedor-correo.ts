export interface CorreoSaliente {
  para: string
  asunto: string
  texto: string
  html: string
}

/** Lo que el servicio necesita para mandar un correo; los tests ponen un doble. */
export interface ProveedorCorreo {
  enviar(correo: CorreoSaliente): Promise<void>
}

export const PROVEEDOR_CORREO = Symbol('PROVEEDOR_CORREO')

function escaparHtml(texto: string): string {
  return texto
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;')
}

/**
 * HTML del correo con la marca de DriveHub.
 *
 * ! El título y el mensaje los redacta otro servicio: se escapan siempre.
 */
export function construirHtmlCorreo(titulo: string, mensaje: string): string {
  const cuerpo = escaparHtml(mensaje).replaceAll('\n', '<br>')
  return `<!doctype html>
<html lang="es"><body style="margin:0;background:#F1F5F9;font-family:Arial,Helvetica,sans-serif;color:#1E293B">
<table role="presentation" width="100%" cellpadding="0" cellspacing="0"><tr><td align="center" style="padding:24px">
<table role="presentation" width="560" cellpadding="0" cellspacing="0" style="background:#FFFFFF;border-radius:8px;overflow:hidden">
<tr><td style="background:#1E293B;color:#FFFFFF;padding:16px 24px;font-size:18px;font-weight:bold">Drive<span style="color:#F59E0B">Hub</span></td></tr>
<tr><td style="padding:24px"><h1 style="margin:0 0 12px;font-size:20px">${escaparHtml(titulo)}</h1>
<p style="margin:0;font-size:15px;line-height:1.5;color:#475569">${cuerpo}</p></td></tr>
<tr><td style="padding:12px 24px;background:#F1F5F9;font-size:12px;color:#475569">Recibes este aviso porque tienes una cuenta en DriveHub. Puedes elegir qué avisos recibir desde la app.</td></tr>
</table></td></tr></table></body></html>`
}
