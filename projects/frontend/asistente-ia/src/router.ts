import { createRouter } from '@tanstack/react-router'

import { routeTree } from './routeTree.gen'

/**
 * Router del remoto. Se crea en cada montaje porque basepath lo decide el
 * shell (ej. '/examenes'): las subrutas de src/routes/ cuelgan de ahí.
 */
export function crearRouter(basepath: string) {
  return createRouter({ routeTree, basepath })
}

declare module '@tanstack/react-router' {
  interface Register {
    router: ReturnType<typeof crearRouter>
  }
}
