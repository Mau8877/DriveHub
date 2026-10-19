import { type ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core'
import { provideRouter, type Routes } from '@angular/router'

import { routes } from './app.routes'

// Config del remoto suelto. Lo que el remoto necesita también dentro del
// shell (ej. HttpClient) se provee en app.routes.ts, no acá.
//
// * La ruta /registro (pública) solo se suma acá: dentro del shell la monta
// * el propio shell, fuera de su layout, desde './routes-publicas'.
const rutasSueltas: Routes = [
  ...routes,
  { path: 'registro', loadChildren: () => import('./routes-publicas').then((m) => m.routes) },
]

export const appConfig: ApplicationConfig = {
  providers: [provideBrowserGlobalErrorListeners(), provideRouter(rutasSueltas)],
}
