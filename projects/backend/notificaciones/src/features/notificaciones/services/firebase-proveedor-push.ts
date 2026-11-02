import { readFileSync } from 'node:fs'

import { Injectable } from '@nestjs/common'
import { ConfigService } from '@nestjs/config'
import { cert, getApps, initializeApp } from 'firebase-admin/app'
import { getMessaging } from 'firebase-admin/messaging'

import type { ProveedorPush, RespuestaPush } from './proveedor-push.js'

/** Lo mínimo de Firebase Messaging que usamos (así los tests lo reemplazan). */
export interface MensajeriaFirebase {
  sendEachForMulticast(mensaje: {
    tokens: string[]
    notification: { title: string; body: string }
    data: Record<string, string>
  }): Promise<{ responses: { success: boolean; error?: { code: string } }[] }>
}

const CODIGOS_TOKEN_INVALIDO = new Set([
  'messaging/registration-token-not-registered',
  'messaging/invalid-registration-token',
])

/**
 * Push con Firebase Cloud Messaging.
 *
 * * FIREBASE_CREDENCIALES es la ruta a la cuenta de servicio (JSON) o el JSON
 * * mismo. Sin ella el envío falla con un error claro y la notificación
 * * queda FALLIDA.
 * ! Los tokens de los dispositivos nunca se loguean.
 */
@Injectable()
export class FirebaseProveedorPush implements ProveedorPush {
  private mensajeria?: MensajeriaFirebase

  constructor(
    private readonly config: ConfigService,
    private readonly crearMensajeria?: () => MensajeriaFirebase
  ) {}

  async enviar(
    tokens: string[],
    titulo: string,
    mensaje: string,
    datos: Record<string, unknown>
  ): Promise<RespuestaPush> {
    const respuesta = await this.obtenerMensajeria().sendEachForMulticast({
      tokens,
      notification: { title: titulo, body: mensaje },
      data: datosComoTexto(datos),
    })

    let aceptados = 0
    const tokensInvalidos: string[] = []
    respuesta.responses.forEach((r, i) => {
      if (r.success) aceptados++
      else if (r.error && CODIGOS_TOKEN_INVALIDO.has(r.error.code)) tokensInvalidos.push(tokens[i])
    })
    return { aceptados, tokensInvalidos }
  }

  private obtenerMensajeria(): MensajeriaFirebase {
    if (!this.mensajeria) {
      this.mensajeria = this.crearMensajeria ? this.crearMensajeria() : this.iniciarFirebase()
    }
    return this.mensajeria
  }

  private iniciarFirebase(): MensajeriaFirebase {
    const credenciales = this.config.get<string>('FIREBASE_CREDENCIALES')?.trim()
    if (!credenciales) {
      throw new Error('Firebase no está configurado: falta FIREBASE_CREDENCIALES')
    }
    const json = credenciales.startsWith('{') ? credenciales : readFileSync(credenciales, 'utf8')
    const app = getApps()[0] ?? initializeApp({ credential: cert(JSON.parse(json)) })
    return getMessaging(app)
  }
}

/** FCM solo admite texto en `data`: lo que no lo es se serializa. */
export function datosComoTexto(datos: Record<string, unknown>): Record<string, string> {
  return Object.fromEntries(
    Object.entries(datos).map(([clave, valor]) => [
      clave,
      typeof valor === 'string' ? valor : JSON.stringify(valor),
    ])
  )
}
