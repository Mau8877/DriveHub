// * React 19 reconoce los Custom Elements y les pasa las propiedades. Esta declaración le dice a
// * TypeScript que existen en JSX. Cada remoto React la trae con
// * `/// <reference types="@drivehub/ui/react" />` en su vite-env.d.ts, en vez de copiarla.
// * Los eventos `dh-cambio` y `dh-blur` se escuchan con `addEventListener` sobre una ref: así no
// * depende de cómo cada versión de React traduce los nombres de eventos con guion.
declare module 'react' {
  namespace JSX {
    interface IntrinsicElements {
      'dh-select': React.DetailedHTMLProps<React.HTMLAttributes<HTMLElement>, HTMLElement>
      'dh-fecha': React.DetailedHTMLProps<React.HTMLAttributes<HTMLElement>, HTMLElement>
    }
  }
}

export {}
