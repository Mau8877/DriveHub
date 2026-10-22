import { provideHttpClient, withInterceptors } from '@angular/common/http'
import type { Routes } from '@angular/router'

import { apiInterceptor } from './core/api/api.interceptor'

/**
 * Páginas públicas de la cuenta (sin sesión): verificar el correo (CU04b), confirmar el cambio
 * de correo (CU06) y recuperar la contraseña (CU04). Es lo que expone el remoto por Native Federation como './routes-cuenta':
 * el shell las monta bajo /account, fuera de su layout con menú, y los enlaces de los correos
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
        path: 'verify-email',
        loadComponent: () =>
          import('./features/cuenta/screens/verificar-correo').then((m) => m.VerificarCorreo),
      },
      {
        path: 'change-email',
        loadComponent: () =>
          import('./features/cuenta/screens/cambiar-correo').then(
            (m) => m.CambiarCorreoConfirmacion
          ),
      },
      {
        path: 'forgot-password',
        loadComponent: () => import('./features/cuenta/screens/recuperar').then((m) => m.Recuperar),
      },
      {
        path: 'reset-password',
        loadComponent: () =>
          import('./features/cuenta/screens/restablecer').then((m) => m.Restablecer),
      },
    ],
  },
]
