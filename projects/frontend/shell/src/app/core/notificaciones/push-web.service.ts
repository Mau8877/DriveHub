import { HttpClient } from '@angular/common/http'
import { Injectable, inject } from '@angular/core'
import { firstValueFrom } from 'rxjs'

import { API_BASE_URL } from '../config'
import { type ConfigFirebaseWeb, FirebaseWeb } from './firebase-web'

/** Dónde se guarda el dispositivo registrado, para quitarlo al cerrar sesión. */
const CLAVE_DISPOSITIVO = 'dh.push.dispositivo'
const URL_CONFIG = '/firebase.config.json'

/**
 * Push web (CU36): registra el navegador de la persona en Notificaciones para que reciba los
 * avisos aunque no tenga el panel abierto, y lo quita al cerrar sesión.
 *
 * * Solo el shell lo usa: es el dueño de la sesión. Los remotos no registran nada.
 * * Si falta la configuración de Firebase, el navegador no lo soporta o la persona niega el
 * * permiso, no pasa nada: el resto del panel funciona igual y no se vuelve a insistir.
 * * Ninguno de los dos métodos lanza: el push nunca bloquea la sesión ni la navegación.
 * ! El token de Firebase nunca se loguea.
 */
@Injectable({ providedIn: 'root' })
export class PushWebService {
  private readonly http = inject(HttpClient)
  private readonly firebase = inject(FirebaseWeb)
  private enCurso: Promise<void> | null = null
  private registrado = false

  /** Pide el permiso (si aún no se decidió), obtiene el token y registra el navegador. */
  activar(): Promise<void> {
    if (this.registrado) return Promise.resolve()
    this.enCurso ??= this.registrar().finally(() => {
      this.enCurso = null
    })
    return this.enCurso
  }

  /** Quita el dispositivo registrado, si lo hay. */
  async desactivar(): Promise<void> {
    const codigo = this.leerCodigo()
    this.registrado = false
    if (!codigo) return
    try {
      await firstValueFrom(this.http.delete(`${API_BASE_URL}/dispositivos-push/${codigo}`))
    } catch {
      // * Si falla (red, sesión vencida), el token queda inactivo cuando Firebase lo rechace
    } finally {
      this.guardarCodigo(null)
    }
  }

  private async registrar(): Promise<void> {
    try {
      if (!this.soportado() || Notification.permission === 'denied') return
      const config = await this.leerConfig()
      if (!config) return
      if (Notification.permission === 'default') {
        if ((await Notification.requestPermission()) !== 'granted') return
      }
      const token = await this.firebase.obtenerToken(config)
      if (!token) return
      const dispositivo = await firstValueFrom(
        this.http.post<{ codigoDispositivoPush: string }>(`${API_BASE_URL}/dispositivos-push`, {
          tokenFcm: token,
          plataforma: 'WEB',
        })
      )
      this.guardarCodigo(dispositivo.codigoDispositivoPush)
      this.registrado = true
    } catch {
      console.warn('No se pudo activar el push web')
    }
  }

  private soportado(): boolean {
    return typeof Notification !== 'undefined' && 'serviceWorker' in navigator
  }

  /** La configuración, o null si falta alguno de sus valores (función desactivada). */
  private async leerConfig(): Promise<ConfigFirebaseWeb | null> {
    try {
      const respuesta = await fetch(URL_CONFIG)
      if (!respuesta.ok) return null
      const config = (await respuesta.json()) as Partial<ConfigFirebaseWeb>
      const completa = [
        config.apiKey,
        config.authDomain,
        config.projectId,
        config.messagingSenderId,
        config.appId,
        config.vapidKey,
      ].every((valor) => typeof valor === 'string' && valor.trim() !== '')
      return completa ? (config as ConfigFirebaseWeb) : null
    } catch {
      return null
    }
  }

  private leerCodigo(): string | null {
    try {
      return localStorage.getItem(CLAVE_DISPOSITIVO)
    } catch {
      return null
    }
  }

  private guardarCodigo(codigo: string | null): void {
    try {
      if (codigo) localStorage.setItem(CLAVE_DISPOSITIVO, codigo)
      else localStorage.removeItem(CLAVE_DISPOSITIVO)
    } catch {
      // * Sin almacenamiento no se puede quitar luego; no impide el registro
    }
  }
}
