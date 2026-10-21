import { type ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core'
import { provideRouter } from '@angular/router'

import { routes } from './app.routes'
import { routes as routesEscuelas } from './routes-escuelas'

// Config del remoto suelto. Lo que el remoto necesita también dentro del
// shell (ej. HttpClient) se provee en app.routes.ts, no acá.
export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    // * /escuelas solo se suma acá (el remoto corriendo suelto): dentro del shell la monta él,
    // * con el rolGuard, desde './routes-escuelas'.
    provideRouter([{ path: 'escuelas', children: routesEscuelas }, ...routes]),
  ],
}
