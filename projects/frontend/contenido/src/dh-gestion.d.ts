import type { EsquemaGestion } from '@drivehub/gestion'

// * React 19 pasa objetos como propiedad a un Custom Element: <dh-gestion esquema={...} />.
// * Esta declaración le dice a TypeScript que el elemento existe y qué propiedad lleva.
declare module 'react' {
  namespace JSX {
    interface IntrinsicElements {
      'dh-gestion': React.HTMLAttributes<HTMLElement> & { esquema?: EsquemaGestion | null }
    }
  }
}
