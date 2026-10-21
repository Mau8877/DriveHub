import { provideHttpClient, withInterceptors } from '@angular/common/http'
import type { Routes } from '@angular/router'

import { apiInterceptor } from './core/api/api.interceptor'

/**
 * Rutas públicas del remoto Identidad (sin sesión). Es lo que expone por
 * Native Federation como './routes-publicas': el shell las monta bajo
 * /register, fuera de su layout con menú.
 *
 * * Las pantallas con sesión (CU05, CU06) siguen en app.routes.ts ('./routes').
 */
export const routes: Routes = [
  {
    path: '',
    providers: [provideHttpClient(withInterceptors([apiInterceptor]))],
    children: [
      {
        path: ':escuelaSlug',
        loadComponent: () => import('./features/registro/pages/registro').then((m) => m.Registro),
      },
    ],
  },
]
