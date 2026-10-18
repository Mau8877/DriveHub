import { HttpErrorResponse, HttpInterceptorFn, HttpResponse } from '@angular/common/http'
import { inject } from '@angular/core'
import { catchError, map, throwError } from 'rxjs'

import { API_BASE_URL } from '../config'
import { SesionService } from '../sesion/sesion.service'
import { ApiError, type ApiEnvelope } from './api.types'

/**
 * Interceptor único de HttpClient para la API de DriveHub (equivalente al
 * apiClient de los remotos React, ver TANSTACK_QUERY_FRONTEND.md).
 *
 * * Agrega el Bearer del shell a las requests a la API.
 * * En éxito desempaqueta el sobre: el body queda como el `data` real
 * * (o { data, meta } en listados). En error rechaza con ApiError (código
 * * de negocio + mensaje) en vez de un HttpErrorResponse crudo.
 */
export const apiInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.startsWith(API_BASE_URL)) return next(req)

  const token = inject(SesionService).obtenerToken()
  const conToken = token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req

  return next(conToken).pipe(
    map((evento) => {
      if (!(evento instanceof HttpResponse)) return evento
      const sobre = evento.body as ApiEnvelope<unknown>
      return evento.clone({
        body: sobre.meta ? { data: sobre.data, meta: sobre.meta } : sobre.data,
      })
    }),
    catchError((error: HttpErrorResponse) => {
      const sobre = error.error as ApiEnvelope<unknown> | null
      return throwError(() => (sobre?.error ? new ApiError(sobre.error, sobre.message) : error))
    })
  )
}
