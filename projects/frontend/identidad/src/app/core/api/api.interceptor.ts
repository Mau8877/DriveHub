import { HttpErrorResponse, type HttpInterceptorFn, HttpResponse } from '@angular/common/http'
import { catchError, map, throwError } from 'rxjs'

import { apiBaseUrl, tokenActual } from './api.config'
import { ApiError, type ApiEnvelope } from './api.types'

/**
 * Interceptor único de HttpClient para la API de DriveHub.
 *
 * * Agrega el Bearer que entrega el shell (el remoto nunca hace login).
 * * En éxito desempaqueta el sobre: el body queda como el `data` real
 * * (o { data, meta } en listados). En error rechaza con ApiError (código
 * * de negocio + mensaje) en vez de un HttpErrorResponse crudo.
 */
export const apiInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.startsWith(apiBaseUrl())) return next(req)

  const token = tokenActual()
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
