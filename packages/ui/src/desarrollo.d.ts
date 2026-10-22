import type { DhShell } from './index.js'

/**
 * Para correr un micro-frontend SUELTO: lo envuelve en un <dh-shell> con el mismo aspecto que
 * tiene dentro del shell. Devuelve el armazón y el elemento donde montar la app.
 */
export function montarShellSuelto(
  elemento: HTMLElement,
  propiedades?: Partial<Omit<DhShell, keyof HTMLElement>>,
): { shell: DhShell; contenido: HTMLElement }
