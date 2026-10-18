import { type ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core'
import { provideRouter } from '@angular/router'

import { routes } from './app.routes'

// Config del remoto suelto. Lo que el remoto necesita también dentro del
// shell (ej. HttpClient) se provee en app.routes.ts, no acá.
export const appConfig: ApplicationConfig = {
  providers: [provideBrowserGlobalErrorListeners(), provideRouter(routes)],
}
