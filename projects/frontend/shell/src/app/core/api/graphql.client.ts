import { HttpClient, HttpContext, HttpErrorResponse } from '@angular/common/http'
import { Injectable, inject } from '@angular/core'
import { firstValueFrom } from 'rxjs'

import { API_BASE_URL } from '../config'
import { SIN_SOBRE } from './api.interceptor'
import { ApiError } from './api.types'

interface RespuestaGraphql<T> {
  data?: T | null
  errors?: { message: string; extensions?: { code?: string } }[]
}

/**
 * Cliente de GraphQL del shell (CU45): `POST /api/v1/graphql` con `{ query, variables }`.
 *
 * * Pasa por el `apiInterceptor` (Bearer y renovación de sesión ante un 401), marcado `SIN_SOBRE`: GraphQL no
 * * usa el sobre estándar. Responde 200 aun cuando hay errores, así que se leen de `errors`.
 * * Un error llega como `ApiError` con su código `ERR_...` (`extensions.code`), igual que en REST.
 * * Solo lo usa el shell; los remotos no consultan Notificaciones.
 */
@Injectable({ providedIn: 'root' })
export class GraphqlClient {
  private readonly http = inject(HttpClient)

  /** Ejecuta una consulta o mutación y devuelve el contenido de `data`. */
  async ejecutar<T>(documento: string, variables?: Record<string, unknown>): Promise<T> {
    let respuesta: RespuestaGraphql<T>
    try {
      respuesta = await firstValueFrom(
        this.http.post<RespuestaGraphql<T>>(
          `${API_BASE_URL}/graphql`,
          { query: documento, variables },
          { context: new HttpContext().set(SIN_SOBRE, true) }
        )
      )
    } catch (error) {
      // * Una consulta inválida responde 400 con el mismo cuerpo `{ errors }`
      if (error instanceof HttpErrorResponse && error.error?.errors?.length) {
        throw aApiError(error.error.errors[0])
      }
      throw error
    }
    if (respuesta.errors?.length) throw aApiError(respuesta.errors[0])
    if (respuesta.data == null) throw new ApiError('ERR_SYS_00', 'Respuesta vacía de GraphQL')
    return respuesta.data
  }
}

function aApiError(error: { message: string; extensions?: { code?: string } }): ApiError {
  return new ApiError(error.extensions?.code ?? 'ERR_SYS_00', error.message)
}
