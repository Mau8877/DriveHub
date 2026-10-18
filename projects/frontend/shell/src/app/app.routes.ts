import { loadRemoteModule } from '@angular-architects/native-federation'
import type { Routes } from '@angular/router'

import { RemoteHost } from './core/federacion/remote-host'
import { prefijo } from './core/federacion/prefijo.matcher'
import { ShellLayout } from './layout/shell-layout'
import { Inicio } from './pages/inicio'
import { Login } from './pages/login'
import { NoEncontrado } from './pages/no-encontrado'

/**
 * Rutas de primer nivel. El shell es dueño solo de este nivel: cada remoto
 * maneja sus propias subrutas.
 *
 * * Remotos Angular (Native Federation): loadChildren con las rutas que
 * * expone cada remoto ('./routes'), URL en public/federation.manifest.json.
 * * Remotos React (Module Federation): RemoteHost los monta con mount/unmount,
 * * URL en public/remotos-react.manifest.json.
 * TODO: guard de sesión (redirigir a /login sin token) cuando exista CU02.
 */
export const routes: Routes = [
  { path: 'login', component: Login },
  {
    path: '',
    component: ShellLayout,
    children: [
      { path: '', component: Inicio },

      // Angular: Identidad (CU01, CU05, CU06), Escuela (CU07 a CU11), Pagos (CU25)
      {
        path: 'identidad',
        loadChildren: () => loadRemoteModule('identidad', './routes').then((m) => m.routes),
      },
      {
        path: 'escuela',
        loadChildren: () => loadRemoteModule('escuela', './routes').then((m) => m.routes),
      },
      {
        path: 'pagos',
        loadChildren: () => loadRemoteModule('pagos', './routes').then((m) => m.routes),
      },

      // React: Exámenes (CU19, CU20, CU22, CU23), Contenido (CU32), Asistente IA (CU41, CU43)
      {
        matcher: prefijo('examenes'),
        component: RemoteHost,
        data: { remoto: 'examenes', basePath: '/examenes' },
      },
      {
        matcher: prefijo('contenido'),
        component: RemoteHost,
        data: { remoto: 'contenido', basePath: '/contenido' },
      },
      {
        matcher: prefijo('asistente-ia'),
        component: RemoteHost,
        data: { remoto: 'asistente_ia', basePath: '/asistente-ia' },
      },

      { path: '**', component: NoEncontrado },
    ],
  },
]
