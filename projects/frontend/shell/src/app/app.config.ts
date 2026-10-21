import { provideHttpClient, withInterceptors } from '@angular/common/http'
import {
  type ApplicationConfig,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core'
import { Router, provideRouter, withComponentInputBinding } from '@angular/router'
import { registrarContextoShell } from '@drivehub/mf-contract'

import { routes } from './app.routes'
import { apiInterceptor } from './core/api/api.interceptor'
import { API_BASE_URL } from './core/config'
import { SesionService } from './core/sesion/sesion.service'

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    // withComponentInputBinding: el `data` de la ruta llega como input (RemoteHost).
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withInterceptors([apiInterceptor])),
    // * El contexto que comparten todos los remotos se registra una sola vez,
    // * antes de que se cargue cualquiera de ellos (ver @drivehub/mf-contract).
    provideAppInitializer(() => {
      const sesion = inject(SesionService)
      const router = inject(Router)
      registrarContextoShell({
        apiBaseUrl: API_BASE_URL,
        obtenerToken: () => sesion.obtenerToken(),
        navegar: (ruta) => void router.navigateByUrl(ruta),
        // * Los remotos piden la renovación al shell: es el único dueño de la sesión
        renovarSesion: () => sesion.renovar(),
      })
    }),
    // * Antes de enrutar: intenta recuperar la sesión con la cookie, sin pedir la contraseña
    provideAppInitializer(() => inject(SesionService).restaurar()),
  ],
}
