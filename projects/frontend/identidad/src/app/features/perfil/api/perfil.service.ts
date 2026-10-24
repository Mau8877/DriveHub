import { HttpClient } from '@angular/common/http'
import { Injectable, inject } from '@angular/core'
import type { Observable } from 'rxjs'

import { apiBaseUrl } from '../../../core/api/api.config'
import type {
  CambiarContrasenaRequest,
  CambiarCorreoRequest,
  EditarPerfilRequest,
  Perfil,
} from '../types/perfil.types'

/**
 * Llamadas a la API del perfil de la cuenta propia (CU06), ya desempaquetadas por el
 * apiInterceptor. Los errores de negocio llegan como ApiError. La persona sale de la sesión:
 * ninguna ruta lleva su código.
 */
@Injectable({ providedIn: 'root' })
export class PerfilService {
  private readonly http = inject(HttpClient)

  private get ruta(): string {
    return `${apiBaseUrl()}/usuarios/me`
  }

  obtener(): Observable<Perfil> {
    return this.http.get<Perfil>(this.ruta)
  }

  editar(cambios: EditarPerfilRequest): Observable<Perfil> {
    return this.http.patch<Perfil>(this.ruta, cambios)
  }

  /** Sube la foto como multipart/form-data (parte `archivo`); el navegador agrega el boundary. */
  subirFoto(archivo: File): Observable<Perfil> {
    const cuerpo = new FormData()
    cuerpo.append('archivo', archivo)
    return this.http.put<Perfil>(`${this.ruta}/foto`, cuerpo)
  }

  /** Idempotente: sin foto responde igual. */
  quitarFoto(): Observable<Perfil> {
    return this.http.delete<Perfil>(`${this.ruta}/foto`)
  }

  cambiarContrasena(solicitud: CambiarContrasenaRequest): Observable<null> {
    return this.http.post<null>(`${this.ruta}/cambiar-contrasena`, solicitud)
  }

  /** Envía el enlace al correo NUEVO; el correo de la cuenta no cambia hasta abrirlo. */
  solicitarCambioCorreo(solicitud: CambiarCorreoRequest): Observable<null> {
    return this.http.post<null>(`${this.ruta}/cambiar-correo`, solicitud)
  }
}
