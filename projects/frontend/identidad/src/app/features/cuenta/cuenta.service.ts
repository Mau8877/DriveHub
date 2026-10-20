import { HttpClient } from '@angular/common/http'
import { Injectable, inject } from '@angular/core'
import type { Observable } from 'rxjs'

import { apiBaseUrl } from '../../core/api/api.config'

/**
 * Llamadas a la API de las páginas públicas de la cuenta (verificar el correo, CU04b),
 * ya desempaquetadas por el apiInterceptor. Los errores de negocio llegan como ApiError.
 */
@Injectable({ providedIn: 'root' })
export class CuentaService {
  private readonly http = inject(HttpClient)

  /** Confirma el enlace del correo. Es público: se abre fuera de la sesión. */
  confirmarCorreo(token: string): Observable<null> {
    return this.http.post<null>(`${apiBaseUrl()}/auth/verificar-correo/confirmar`, { token })
  }
}
