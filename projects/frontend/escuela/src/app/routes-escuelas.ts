import { provideHttpClient, withInterceptors } from '@angular/common/http'
import type { Routes } from '@angular/router'

import { apiInterceptor } from './core/api/api.interceptor'

/**
 * Rutas de la gestión de escuelas del Super admin (CU05). Es lo que expone el remoto por Native
 * Federation como './routes-escuelas': el shell las monta bajo /escuelas, protegidas con el rol
 * SUPER_ADMIN. Aparte de './routes' a propósito: esas son las del Administrador de escuela
 * (bajo /escuela) y el Super admin no las carga.
 *
 * * Es UNA sola ruta: <dh-gestion> maneja por dentro el listado, crear y editar.
 */
export const routes: Routes = [
  {
    path: '',
    providers: [provideHttpClient(withInterceptors([apiInterceptor]))],
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./features/escuelas/screens/escuelas-gestion').then((m) => m.EscuelasGestion),
      },
    ],
  },
]
