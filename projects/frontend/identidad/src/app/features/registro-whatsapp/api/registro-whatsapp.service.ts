import { HttpClient } from '@angular/common/http'
import { Injectable, inject } from '@angular/core'
import type { Observable } from 'rxjs'

import { apiBaseUrl } from '../../../core/api/api.config'
import type { RegistroWhatsappCompletado } from '../types/registro-whatsapp.types'

/**
 * Llamada de la página pública donde se fija la contraseña de un registro hecho por WhatsApp (CU48),
 * ya desempaquetada por el apiInterceptor. Los errores de negocio llegan como ApiError.
 */
@Injectable({ providedIn: 'root' })
export class RegistroWhatsappService {
  private readonly http = inject(HttpClient)

  /** Crea la cuenta con el enlace del chat y la contraseña elegida. Es público: no hay sesión todavía. */
  completar(token: string, password: string): Observable<RegistroWhatsappCompletado> {
    return this.http.post<RegistroWhatsappCompletado>(
      `${apiBaseUrl()}/registros-whatsapp/completar`,
      {
        token,
        password,
      }
    )
  }
}
