import { Injectable } from '@nestjs/common'
import { ConfigService } from '@nestjs/config'
import { createTransport, type Transporter } from 'nodemailer'

import type { CorreoSaliente, ProveedorCorreo } from './proveedor-correo.js'

/**
 * Correo por SMTP. En desarrollo va a Mailpit; con un SMTP real solo cambian
 * las variables (mismas que Identidad).
 */
@Injectable()
export class NodemailerProveedorCorreo implements ProveedorCorreo {
  private transporter?: Transporter

  constructor(private readonly config: ConfigService) {}

  async enviar(correo: CorreoSaliente): Promise<void> {
    await this.obtenerTransporter().sendMail({
      from: this.config.get<string>('SMTP_FROM', 'no-reply@drivehub.local'),
      to: correo.para,
      subject: correo.asunto,
      text: correo.texto,
      html: correo.html,
    })
  }

  private obtenerTransporter(): Transporter {
    if (!this.transporter) {
      const usuario = this.config.get<string>('SMTP_USER')
      this.transporter = createTransport({
        host: this.config.get<string>('SMTP_HOST', 'localhost'),
        port: Number(this.config.get<string>('SMTP_PORT', '1025')),
        auth: usuario ? { user: usuario, pass: this.config.get<string>('SMTP_PASSWORD') } : undefined,
      })
    }
    return this.transporter
  }
}
