import { provideHttpClient, withInterceptors } from '@angular/common/http'
import type { Routes } from '@angular/router'

import { apiInterceptor } from './core/api/api.interceptor'

/**
 * Páginas públicas de la cuenta (sin sesión): verificar el correo (CU04b). Es lo
 * que expone el remoto por Native Federation como './routes-cuenta': el shell las
 * monta bajo /cuenta, fuera de su layout con menú, y los enlaces de los correos
 * apuntan acá.
 *
 * * Registro es otra cosa y sigue en './routes-publicas'.
 */
export const routes: Routes = [
  {
    path: '',
    providers: [provideHttpClient(withInterceptors([apiInterceptor]))],
    children: [
      {
        path: 'verificar-correo',
        loadComponent: () =>
          import('./features/cuenta/pages/verificar-correo').then((m) => m.VerificarCorreo),
      },
    ],
  },
]
