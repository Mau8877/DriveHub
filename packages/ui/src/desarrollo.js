import './index.js'

/**
 * Para correr un micro-frontend SUELTO (sin el shell): lo envuelve en un <dh-shell> con el mismo
 * menú, cabecera y aspecto que tiene dentro del shell. Dentro del shell NO se usa.
 *
 *   // main.tsx de un remoto React
 *   const { contenido } = montarShellSuelto(document.getElementById('root'), {
 *     items: [{ ruta: '/', etiqueta: 'Exámenes', icono: 'clipboard' }],
 *     migas: [{ etiqueta: 'Exámenes' }],
 *   })
 *   // ...y se monta la app en `contenido`
 *
 * @param {HTMLElement} elemento  contenedor donde queda el armazón (ej. #root)
 * @param {Record<string, unknown>} propiedades  propiedades de <dh-shell> (items, usuario, ...)
 * @returns {{ shell: HTMLElement, contenido: HTMLElement }}
 */
export function montarShellSuelto(elemento, propiedades = {}) {
  const shell = document.createElement('dh-shell')
  Object.assign(shell, propiedades)
  const contenido = document.createElement('div')
  contenido.id = 'contenido-remoto'
  shell.appendChild(contenido)
  elemento.replaceChildren(shell)
  return { shell, contenido }
}
