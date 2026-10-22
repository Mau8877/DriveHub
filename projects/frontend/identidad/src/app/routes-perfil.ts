import { provideHttpClient, withInterceptors } from '@angular/common/http'
import type { Routes } from '@angular/router'

import { apiInterceptor } from './core/api/api.interceptor'

/**
 * Perfil de la cuenta propia (CU06). Es lo que expone el remoto por Native Federation como
 * './routes-perfil': el shell las monta bajo /perfil, dentro de su layout y con sesión, para los
 * cuatro roles. Va aparte de './routes' porque la ruta /perfil es de primer nivel del shell.
 */
export const routes: Routes = [
  {
    path: '',
    providers: [provideHttpClient(withInterceptors([apiInterceptor]))],
    children: [
      {
        path: '',
        loadComponent: () => import('./features/perfil/screens/perfil').then((m) => m.Perfil),
      },
    ],
  },
]
