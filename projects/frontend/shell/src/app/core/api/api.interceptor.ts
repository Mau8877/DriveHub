import {
  HttpContextToken,
  HttpErrorResponse,
  type HttpEvent,
  type HttpHandlerFn,
  type HttpInterceptorFn,
  type HttpRequest,
  HttpResponse,
} from '@angular/common/http'
import { inject } from '@angular/core'
import { type Observable, catchError, from, map, switchMap, throwError } from 'rxjs'

import { API_BASE_URL } from '../config'
import { SesionService } from '../sesion/sesion.service'
import { ApiError, type ApiEnvelope } from './api.types'

/** Marca una petición que ya se repitió tras renovar la sesión: nunca se repite dos veces. */
const REINTENTADA = new HttpContextToken<boolean>(() => false)

/**
 * Interceptor único de HttpClient para la API de DriveHub (equivalente al
 * apiClient de los remotos React, ver TANSTACK_QUERY_FRONTEND.md).
 *
 * * Agrega el Bearer del shell a las requests a la API.
 * * En éxito desempaqueta el sobre: el body queda como el `data` real
 * * (o { data, meta } en listados). En error rechaza con ApiError (código
 * * de negocio + mensaje) en vez de un HttpErrorResponse crudo.
 * * Ante un 401 (sesión vencida) renueva la sesión UNA vez y repite la
 * * petición; si no se puede renovar, el error original llega al llamador
 * * (la renovación ya llevó a la persona a /login).
 */
export const apiInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.startsWith(API_BASE_URL)) return next(req)
  return enviar(req, next, inject(SesionService))
}

function enviar(
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
  sesion: SesionService
): Observable<HttpEvent<unknown>> {
  const token = sesion.obtenerToken()
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
      if (!debeRenovar(req, error)) return throwError(() => aErrorDeApi(error))
      return from(sesion.renovar()).pipe(
        switchMap((nuevoToken) =>
          nuevoToken
            ? enviar(req.clone({ context: req.context.set(REINTENTADA, true) }), next, sesion)
            : throwError(() => aErrorDeApi(error))
        )
      )
    })
  )
}

/** 401 de una petición de negocio (no del login/renovación) que todavía no se repitió. */
function debeRenovar(req: HttpRequest<unknown>, error: HttpErrorResponse): boolean {
  return error.status === 401 && !req.context.get(REINTENTADA) && !req.url.includes('/auth/')
}

function aErrorDeApi(error: HttpErrorResponse): unknown {
  const sobre = error.error as ApiEnvelope<unknown> | null
  return sobre?.error ? new ApiError(sobre.error, sobre.message) : error
}
