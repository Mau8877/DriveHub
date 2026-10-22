import { css, html } from 'lit'

/** Estilos que comparten todos los componentes: reset, foco visible y movimiento reducido. */
export const base = css`
  :host {
    font-family: var(--font-sans);
    color: var(--foreground);
  }
  *,
  *::before,
  *::after {
    box-sizing: border-box;
  }
  button {
    font: inherit;
    color: inherit;
    cursor: pointer;
    border: 0;
    background: none;
    padding: 0;
  }
  a {
    color: inherit;
    text-decoration: none;
  }
  :focus-visible {
    outline: 2px solid var(--ring);
    outline-offset: 2px;
    border-radius: 0.4rem;
  }
  .ico {
    flex: none;
  }
  @media (prefers-reduced-motion: reduce) {
    *,
    *::before,
    *::after {
      animation-duration: 0.01ms !important;
      transition-duration: 0.01ms !important;
    }
  }
`

/** Marca de DriveHub: una "D" con la línea central de una ruta. Solo variables de los tokens. */
export function logo(tam = 32) {
  return html`<svg class="logo" width=${tam} height=${tam} viewBox="0 0 32 32" aria-hidden="true">
    <rect width="32" height="32" rx="9" fill="var(--ambar-vial)" />
    <path d="M10 8h6.5a8 8 0 0 1 0 16H10V8Z" fill="none" stroke="var(--gris-asfalto)" stroke-width="2.6" stroke-linejoin="round" />
    <path d="M13.5 12.5v7" stroke="var(--gris-asfalto)" stroke-width="2.2" stroke-linecap="round" stroke-dasharray="1.6 3" />
  </svg>`
}

/** Nombre de marca por defecto: "Drive" + "Hub" resaltado. */
export const MARCA_POR_DEFECTO = { prefijo: 'Drive', resaltado: 'Hub', ruta: '/' }

/** Logo + nombre de la marca. `marca` es `{ prefijo, resaltado, ruta }` (todo opcional). */
export function marcaVisual(marca, tam = 32) {
  const m = { ...MARCA_POR_DEFECTO, ...(marca ?? {}) }
  return html`${logo(tam)}<span class="marca-nombre">${m.prefijo}<b>${m.resaltado}</b></span>`
}
