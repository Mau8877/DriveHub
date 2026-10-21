import { inject } from '@angular/core'
import { type CanActivateFn, Router } from '@angular/router'

import { SesionService } from './sesion.service'

/** Protege el panel: sin sesión lleva a /login recordando la dirección pedida. */
export const sesionGuard: CanActivateFn = (_ruta, estado) => {
  if (inject(SesionService).autenticado()) return true
  return inject(Router).createUrlTree(['/login'], { queryParams: { redirect: estado.url } })
}

/** Quien ya tiene sesión no necesita ver el login: va al inicio. */
export const loginGuard: CanActivateFn = () => {
  if (!inject(SesionService).autenticado()) return true
  return inject(Router).createUrlTree(['/'])
}
