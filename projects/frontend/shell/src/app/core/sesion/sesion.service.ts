import { DestroyRef, Injectable, computed, inject, signal } from '@angular/core'
import { Router } from '@angular/router'
import { firstValueFrom } from 'rxjs'

import { ApiError } from '../api/api.types'
import { AuthApiService } from './auth-api.service'
import type { SesionRespuesta, UsuarioSesion } from './sesion.types'

/** Se renueva cuando pasó esta fracción de la vida del token de acceso. */
const FRACCION_DE_VIDA_PARA_RENOVAR = 0.8
/** Código con el que se explica una sesión que no se pudo renovar sin un motivo más específico. */
const SESION_TERMINADA = 'ERR_IDE_09'
/** El correo ya estaba verificado: la sesión que tiene el cliente quedó vieja. */
const CORREO_YA_VERIFICADO = 'ERR_IDE_10'

/**
 * Sesión del usuario: el ÚNICO lugar de la web donde vive el access token.
 *
 * * Solo en memoria (nunca localStorage ni cookie legible por JS): el
 * * refresh token viaja en una cookie HttpOnly que maneja el backend (ver
 * * .claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md). Los remotos piden el
 * * token y la renovación al shell vía @drivehub/mf-contract, no la guardan.
 * * La renovación se hace de a una: en la pestaña comparte la promesa en curso
 * * y entre pestañas se serializa con Web Locks, porque la rotación invalida el
 * * refresh token usado y dos renovaciones simultáneas harían fallar a una.
 */
@Injectable({ providedIn: 'root' })
export class SesionService {
  private readonly api = inject(AuthApiService)
  private readonly router = inject(Router)

  private readonly accessToken = signal<string | null>(null)
  private readonly _usuario = signal<UsuarioSesion | null>(null)
  private readonly _restaurada = signal(false)

  readonly usuario = this._usuario.asReadonly()
  /** true cuando ya se intentó recuperar la sesión al arrancar (con o sin éxito). */
  readonly restaurada = this._restaurada.asReadonly()
  readonly autenticado = computed(() => this.accessToken() !== null)
  readonly roles = computed(() => this._usuario()?.roles ?? [])

  private temporizador: ReturnType<typeof setTimeout> | null = null
  private renovacionEnCurso: Promise<string | null> | null = null

  constructor() {
    // * Si la persona verifica el correo en otra pestaña o ventana, al volver se renueva la
    // * sesión para que el botón "Verificar correo" desaparezca sin esperar a la renovación.
    if (typeof document !== 'undefined') {
      const alVolver = () => {
        if (
          document.visibilityState === 'visible' &&
          this.autenticado() &&
          this._usuario()?.correoVerificado === false
        ) {
          void this.renovar()
        }
      }
      document.addEventListener('visibilitychange', alVolver)
      inject(DestroyRef).onDestroy(() => document.removeEventListener('visibilitychange', alVolver))
    }
  }

  obtenerToken(): string | null {
    return this.accessToken()
  }

  async iniciarSesion(email: string, password: string): Promise<void> {
    const sesion = await firstValueFrom(this.api.login(email, password))
    this.establecer(sesion)
  }

  /** Al arrancar: intenta recuperar la sesión con la cookie, sin pedir la contraseña. */
  async restaurar(): Promise<void> {
    try {
      this.establecer(await firstValueFrom(this.api.renovar()))
    } catch {
      // * Sin cookie válida simplemente no hay sesión: el guard lleva a /login
    } finally {
      this._restaurada.set(true)
    }
  }

  /**
   * Renueva la sesión. Devuelve el token nuevo, o null si no se pudo; en ese
   * caso ya limpió la sesión y llevó a /login con el motivo.
   */
  renovar(): Promise<string | null> {
    if (!this.renovacionEnCurso) {
      this.renovacionEnCurso = this.ejecutarRenovacion().finally(() => {
        this.renovacionEnCurso = null
      })
    }
    return this.renovacionEnCurso
  }

  /**
   * Pide el correo de verificación (CU04b). Si el correo ya estaba verificado (`ERR_IDE_10`),
   * renueva la sesión para actualizarla y no es un error; cualquier otro fallo se propaga
   * para que la pantalla lo muestre.
   */
  async verificarCorreo(): Promise<void> {
    try {
      await firstValueFrom(this.api.solicitarVerificacionCorreo())
    } catch (error) {
      if (error instanceof ApiError && error.code === CORREO_YA_VERIFICADO) {
        await this.renovar()
        return
      }
      throw error
    }
  }

  /**
   * Cierra la sesión: avisa al backend (revoca el refresh token y borra la cookie),
   * olvida la sesión local y lleva a /login sin redirect ni motivo.
   *
   * * El borrado local no depende de la respuesta: si el backend falla (red, 429, 5xx)
   * * igual se sale, la sesión renovable vence sola en el servidor.
   */
  async cerrarSesion(): Promise<void> {
    try {
      await firstValueFrom(this.api.cerrar())
    } catch {
      // * El borrado local no depende del backend
    }
    this.limpiar()
    await this.router.navigate(['/login'])
  }

  /** Olvida la sesión local (token, usuario y temporizador). No llama al backend. */
  limpiar(): void {
    this.cancelarTemporizador()
    this.accessToken.set(null)
    this._usuario.set(null)
  }

  private async ejecutarRenovacion(): Promise<string | null> {
    try {
      const sesion = await this.conBloqueoEntrePestanias(() => firstValueFrom(this.api.renovar()))
      this.establecer(sesion)
      return sesion.accessToken
    } catch (error) {
      const motivo = error instanceof ApiError ? error.code : SESION_TERMINADA
      const redirect = this.router.url
      this.limpiar()
      void this.router.navigate(['/login'], {
        queryParams: { redirect: redirect.startsWith('/login') ? undefined : redirect, motivo },
      })
      return null
    }
  }

  private conBloqueoEntrePestanias<T>(tarea: () => Promise<T>): Promise<T> {
    const bloqueos = typeof navigator !== 'undefined' ? navigator.locks : undefined
    return bloqueos ? bloqueos.request('drivehub-renovar', tarea) : tarea()
  }

  private establecer(sesion: SesionRespuesta): void {
    this.accessToken.set(sesion.accessToken)
    this._usuario.set(sesion.usuario)
    this.programarRenovacion(sesion.expiraEnSegundos)
  }

  private programarRenovacion(expiraEnSegundos: number): void {
    this.cancelarTemporizador()
    this.temporizador = setTimeout(
      () => void this.renovar(),
      expiraEnSegundos * 1000 * FRACCION_DE_VIDA_PARA_RENOVAR
    )
  }

  private cancelarTemporizador(): void {
    if (this.temporizador !== null) {
      clearTimeout(this.temporizador)
      this.temporizador = null
    }
  }
}
