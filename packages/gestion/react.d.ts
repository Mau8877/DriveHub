import type { EsquemaGestion } from './src/index.js'

// * React 19 pasa objetos como propiedad a un Custom Element: <dh-gestion esquema={...} />.
// * Esta declaración le dice a TypeScript que el elemento existe y qué propiedad lleva. Cada remoto
// * React la trae con `/// <reference types="@drivehub/gestion/react" />` en su vite-env.d.ts, en vez
// * de copiarla.
declare module 'react' {
  namespace JSX {
    interface IntrinsicElements {
      'dh-gestion': React.HTMLAttributes<HTMLElement> & { esquema?: EsquemaGestion | null }
    }
  }
}
