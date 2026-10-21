import { inject } from '@angular/core'
import { type CanActivateFn, Router } from '@angular/router'

import { SesionService } from './sesion.service'

/**
 * Deja pasar solo a quien tenga alguno de los roles pedidos; el resto vuelve al inicio.
 *
 * * Va junto al `sesionGuard` del layout, que ya exige sesión: acá no se vuelve a comprobar.
 * * No reemplaza la autorización del backend: protege la pantalla, no los datos.
 */
export function rolGuard(...roles: string[]): CanActivateFn {
  return () => {
    const tieneRol = inject(SesionService)
      .roles()
      .some((rol) => roles.includes(rol))
    return tieneRol ? true : inject(Router).createUrlTree(['/'])
  }
}
