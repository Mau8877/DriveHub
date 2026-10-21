import { HttpClient } from '@angular/common/http'
import { Injectable, inject } from '@angular/core'
import type { Observable } from 'rxjs'

import { apiBaseUrl } from '../../../core/api/api.config'
import type {
  EscuelaPublica,
  RegistroEstudianteRequest,
  UsuarioRegistrado,
} from '../types/registro.types'

/**
 * Llamadas a la API del registro del estudiante (CU01), ya desempaquetadas
 * por el apiInterceptor. Los errores de negocio llegan como ApiError.
 */
@Injectable({ providedIn: 'root' })
export class RegistroService {
  private readonly http = inject(HttpClient)

  obtenerEscuela(slug: string): Observable<EscuelaPublica> {
    return this.http.get<EscuelaPublica>(`${apiBaseUrl()}/escuelas/${encodeURIComponent(slug)}`)
  }

  registrar(datos: RegistroEstudianteRequest): Observable<UsuarioRegistrado> {
    return this.http.post<UsuarioRegistrado>(`${apiBaseUrl()}/usuarios`, datos)
  }
}
