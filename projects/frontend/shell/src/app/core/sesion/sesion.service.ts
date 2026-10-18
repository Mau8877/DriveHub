import { Injectable, computed, signal } from '@angular/core'

/**
 * Sesión del usuario: el ÚNICO lugar de la web donde vive el access token.
 *
 * * Solo en memoria (nunca localStorage ni cookie legible por JS): el
 * * refresh token viaja en una cookie HttpOnly que maneja el backend (ver
 * * .claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md). Los remotos piden el
 * * token al shell vía el contrato (@drivehub/mf-contract), no lo guardan.
 * TODO: CU02 (login), CU03 (logout) y la renovación automática del token
 * TODO: completan este servicio. Hoy solo expone el estado.
 */
@Injectable({ providedIn: 'root' })
export class SesionService {
  private readonly accessToken = signal<string | null>(null)

  readonly autenticado = computed(() => this.accessToken() !== null)

  obtenerToken(): string | null {
    return this.accessToken()
  }
}
