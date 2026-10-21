import { HttpClient } from '@angular/common/http'
import { Injectable, inject } from '@angular/core'
import type { Observable } from 'rxjs'

import { API_BASE_URL } from '../config'
import type { LoginRequest, SesionRespuesta } from './sesion.types'

/**
 * Llamadas HTTP de la sesión web. Las respuestas ya vienen desempaquetadas
 * por el apiInterceptor y los errores de negocio como ApiError.
 *
 * * El refresh token viaja en la cookie `refreshToken` (HttpOnly, mismo
 * * origen): el navegador la manda sola, este service nunca la lee ni la escribe.
 */
@Injectable({ providedIn: 'root' })
export class AuthApiService {
  private readonly http = inject(HttpClient)

  login(email: string, password: string): Observable<SesionRespuesta> {
    const cuerpo: LoginRequest = { email, password, dispositivo: 'WEB' }
    return this.http.post<SesionRespuesta>(`${API_BASE_URL}/auth/login`, cuerpo)
  }

  renovar(): Observable<SesionRespuesta> {
    return this.http.post<SesionRespuesta>(`${API_BASE_URL}/auth/refresh`, { dispositivo: 'WEB' })
  }

  /** Pide el correo con el enlace para verificar la dirección de la cuenta (CU04b). */
  solicitarVerificacionCorreo(): Observable<null> {
    return this.http.post<null>(`${API_BASE_URL}/auth/verificar-correo`, null)
  }

  cerrar(): Observable<null> {
    return this.http.post<null>(`${API_BASE_URL}/auth/logout`, { dispositivo: 'WEB' })
  }
}
