import { provideHttpClient, withInterceptors } from '@angular/common/http'
import type { Routes } from '@angular/router'

import { apiInterceptor } from './core/api/api.interceptor'

/**
 * Rutas de la gestión de escuelas del Super admin (CU05). Es lo que expone el remoto por Native
 * Federation como './routes-escuelas': el shell las monta bajo /escuelas, protegidas con el rol
 * SUPER_ADMIN. Aparte de './routes' a propósito: esas son las del Administrador de escuela
 * (bajo /escuela) y el Super admin no las carga.
 *
 * * HttpClient se provee acá (a nivel de ruta), igual que en app.routes.ts.
 */
export const routes: Routes = [
  {
    path: '',
    providers: [provideHttpClient(withInterceptors([apiInterceptor]))],
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./features/escuelas/screens/escuelas-lista').then((m) => m.EscuelasLista),
      },
      {
        path: 'nueva',
        loadComponent: () =>
          import('./features/escuelas/screens/escuela-crear').then((m) => m.EscuelaCrear),
      },
      {
        path: ':codigo/editar',
        loadComponent: () =>
          import('./features/escuelas/screens/escuela-editar').then((m) => m.EscuelaEditar),
      },
    ],
  },
]
