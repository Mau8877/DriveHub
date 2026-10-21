import {
  HttpContextToken,
  HttpErrorResponse,
  type HttpEvent,
  type HttpHandlerFn,
  type HttpInterceptorFn,
  type HttpRequest,
  HttpResponse,
} from '@angular/common/http'
import { obtenerContextoShell } from '@drivehub/mf-contract'
import { type Observable, catchError, from, map, switchMap, throwError } from 'rxjs'

import { apiBaseUrl, tokenActual } from './api.config'
import { ApiError, type ApiEnvelope } from './api.types'

/** Marca una petición que ya se repitió tras renovar la sesión: nunca se repite dos veces. */
const REINTENTADA = new HttpContextToken<boolean>(() => false)

/**
 * Interceptor único de HttpClient para la API de DriveHub.
 *
 * * Agrega el Bearer que entrega el shell (el remoto nunca hace login).
 * * En éxito desempaqueta el sobre: el body queda como el `data` real
 * * (o { data, meta } en listados). En error rechaza con ApiError (código
 * * de negocio + mensaje) en vez de un HttpErrorResponse crudo.
 * * Ante un 401 (sesión vencida) le pide al shell renovar la sesión UNA vez y
 * * repite la petición; si no se puede (o el remoto corre suelto, sin shell),
 * * el error llega al llamador. Iniciar sesión o ir a /login es cosa del shell.
 */
export const apiInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.startsWith(apiBaseUrl())) return next(req)
  return enviar(req, next)
}

function enviar(req: HttpRequest<unknown>, next: HttpHandlerFn): Observable<HttpEvent<unknown>> {
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
      if (!debeRenovar(req, error)) return throwError(() => aErrorDeApi(error))
      return from(renovarSesionDelShell()).pipe(
        switchMap((nuevoToken) =>
          nuevoToken
            ? enviar(req.clone({ context: req.context.set(REINTENTADA, true) }), next)
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

function renovarSesionDelShell(): Promise<string | null> {
  return obtenerContextoShell()?.renovarSesion() ?? Promise.resolve(null)
}

function aErrorDeApi(error: HttpErrorResponse): unknown {
  const sobre = error.error as ApiEnvelope<unknown> | null
  return sobre?.error ? new ApiError(sobre.error, sobre.message) : error
}
