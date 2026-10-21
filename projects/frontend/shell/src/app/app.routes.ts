import { loadRemoteModule } from '@angular-architects/native-federation'
import type { Routes } from '@angular/router'

import { RemoteHost } from './core/federacion/remote-host'
import { prefijo } from './core/federacion/prefijo.matcher'
import { ShellLayout } from './layout/shell-layout'
import { loginGuard, sesionGuard } from './core/sesion/sesion.guard'
import { ROLES } from './core/sesion/roles'
import { rolGuard } from './core/sesion/rol.guard'
import { Inicio } from './features/inicio/screens/inicio'
import { Login } from './features/login/screens/login'
import { NoEncontrado } from './features/no-encontrado/screens/no-encontrado'

/**
 * Rutas de primer nivel. El shell es dueño solo de este nivel: cada remoto
 * maneja sus propias subrutas.
 *
 * * Remotos Angular (Native Federation): loadChildren con las rutas que
 * * expone cada remoto ('./routes'), URL en public/federation.manifest.json.
 * * Remotos React (Module Federation): RemoteHost los monta con mount/unmount,
 * * URL en public/remotos-react.manifest.json.
 * * El layout con todos los remotos exige sesión (sesionGuard); /login solo se
 * * muestra sin sesión (loginGuard); /register es público.
 */
export const routes: Routes = [
  { path: 'login', component: Login, canActivate: [loginGuard] },

  // Público y fuera del layout (sin menú ni sesión): registro del estudiante
  // en el enlace de su escuela, /register/:escuelaSlug (CU01, remoto identidad).
  {
    path: 'register',
    loadChildren: () => loadRemoteModule('identidad', './routes-publicas').then((m) => m.routes),
  },

  // Público y fuera del layout: páginas de la cuenta a las que llevan los enlaces de los
  // correos, /account/verify-email (CU04b, remoto identidad). Sin sesionGuard ni loginGuard:
  // se abren con o sin sesión, también desde el navegador de un teléfono.
  {
    path: 'account',
    loadChildren: () => loadRemoteModule('identidad', './routes-cuenta').then((m) => m.routes),
  },
  {
    path: '',
    component: ShellLayout,
    canActivate: [sesionGuard],
    children: [
      { path: '', component: Inicio },

      // Gestión de escuelas del Super admin (CU05). Es de primer nivel porque el shell es dueño de
      // ese nivel; las pantallas son del remoto `escuela`, que las expone aparte de './routes'
      // (las del Administrador, bajo /escuela). El rolGuard protege la ruta, no solo el menú.
      {
        path: 'escuelas',
        canActivate: [rolGuard(ROLES.SUPER_ADMIN)],
        loadChildren: () => loadRemoteModule('escuela', './routes-escuelas').then((m) => m.routes),
      },

      // Angular: Identidad (CU01, CU06), Escuela (CU05, CU07 a CU11), Pagos (CU25)
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
