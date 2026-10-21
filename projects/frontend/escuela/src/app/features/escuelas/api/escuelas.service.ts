import { HttpClient, HttpParams } from '@angular/common/http'
import { Injectable, inject } from '@angular/core'
import type { Observable } from 'rxjs'

import { apiBaseUrl } from '../../../core/api/api.config'
import type {
  CrearEscuelaRequest,
  EscuelaCreada,
  EscuelaEstado,
  EscuelaListada,
  FiltroEscuelas,
  PaginaDeEscuelas,
} from '../types/escuela.types'

/**
 * Llamadas a la API de gestión de escuelas del Super admin (CU05), ya desempaquetadas por el
 * apiInterceptor. Los errores de negocio llegan como ApiError.
 */
@Injectable({ providedIn: 'root' })
export class EscuelasService {
  private readonly http = inject(HttpClient)

  private get base(): string {
    return `${apiBaseUrl()}/escuelas`
  }

  /** `filter[estado]` viaja con los corchetes codificados (HttpParams lo hace solo). */
  listar(filtro: Partial<FiltroEscuelas> = {}): Observable<PaginaDeEscuelas> {
    let params = new HttpParams()
    if (filtro.page) params = params.set('page', filtro.page)
    if (filtro.pageSize) params = params.set('pageSize', filtro.pageSize)
    const busqueda = filtro.search?.trim()
    if (busqueda) params = params.set('search', busqueda)
    if (filtro.estado) params = params.set('filter[estado]', filtro.estado)
    return this.http.get<PaginaDeEscuelas>(this.base, { params })
  }

  /** La escuela en cualquier estado (también si está inactiva). */
  obtener(codigo: string): Observable<EscuelaEstado> {
    return this.http.get<EscuelaEstado>(`${this.base}/por-codigo/${encodeURIComponent(codigo)}`)
  }

  crear(datos: CrearEscuelaRequest): Observable<EscuelaCreada> {
    return this.http.post<EscuelaCreada>(this.base, datos)
  }

  editar(codigo: string, nombre: string): Observable<EscuelaListada> {
    return this.http.patch<EscuelaListada>(`${this.base}/${encodeURIComponent(codigo)}`, { nombre })
  }

  desactivar(codigo: string): Observable<null> {
    return this.http.delete<null>(`${this.base}/${encodeURIComponent(codigo)}`)
  }

  reactivar(codigo: string): Observable<EscuelaListada> {
    return this.http.post<EscuelaListada>(
      `${this.base}/${encodeURIComponent(codigo)}/reactivar`,
      null
    )
  }
}
