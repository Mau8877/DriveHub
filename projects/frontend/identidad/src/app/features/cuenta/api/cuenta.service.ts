import { HttpClient } from '@angular/common/http'
import { Injectable, inject } from '@angular/core'
import type { Observable } from 'rxjs'

import { apiBaseUrl } from '../../../core/api/api.config'

/**
 * Llamadas a la API de las páginas públicas de la cuenta (verificar el correo, CU04b,
 * y recuperar la contraseña, CU04), ya desempaquetadas por el apiInterceptor. Los
 * errores de negocio llegan como ApiError.
 */
@Injectable({ providedIn: 'root' })
export class CuentaService {
  private readonly http = inject(HttpClient)

  /** Confirma el enlace del correo. Es público: se abre fuera de la sesión. */
  confirmarCorreo(token: string): Observable<null> {
    return this.http.post<null>(`${apiBaseUrl()}/auth/verificar-correo/confirmar`, { token })
  }

  /** Confirma el cambio de correo con el enlace que llegó al correo nuevo (CU06). Público. */
  confirmarCambioCorreo(token: string): Observable<null> {
    return this.http.post<null>(`${apiBaseUrl()}/auth/cambiar-correo/confirmar`, { token })
  }

  /** Pide el enlace de recuperación. Responde siempre lo mismo, exista o no la cuenta. */
  solicitarRecuperacion(email: string): Observable<null> {
    return this.http.post<null>(`${apiBaseUrl()}/auth/recuperar`, { email })
  }

  /** Fija la contraseña nueva con el token del enlace del correo. */
  restablecerContrasena(token: string, password: string): Observable<null> {
    return this.http.post<null>(`${apiBaseUrl()}/auth/restablecer`, { token, password })
  }
}
