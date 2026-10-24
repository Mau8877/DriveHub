import { provideHttpClient, withInterceptors } from '@angular/common/http'
import type { Routes } from '@angular/router'

import { apiInterceptor } from './core/api/api.interceptor'
import { Inicio } from './inicio'

/**
 * Rutas del remoto Escuela. Es lo que expone por Native Federation
 * ('./routes'): el shell las monta bajo /escuela con loadChildren.
 *
 * * HttpClient se provee acá (a nivel de ruta) y no en app.config.ts: así el
 * * remoto usa su propio interceptor tanto suelto como dentro del shell.
 * * Cada feature nueva suma sus rutas como hijas de esta ruta raíz.
 */
export const routes: Routes = [
  {
    path: '',
    providers: [provideHttpClient(withInterceptors([apiInterceptor]))],
    children: [
      { path: '', component: Inicio },
      // * Gestionar autos (CU07): una sola ruta, <dh-gestion> maneja lista, crear y editar por dentro
      {
        path: 'autos',
        loadComponent: () =>
          import('./features/autos/screens/autos-gestion').then((m) => m.AutosGestion),
      },
    ],
  },
]
