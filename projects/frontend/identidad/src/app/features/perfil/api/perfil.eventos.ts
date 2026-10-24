import { emitirEvento } from '@drivehub/mf-contract'

import type { Perfil } from '../types/perfil.types'

/**
 * Mismo nombre que escucha el shell (core/sesion/eventos.ts): los remotos no importan del shell.
 * Avisa al avatar de la cabecera que cambió el nombre o la foto, sin tocar la sesión.
 */
export const EVENTO_PERFIL_ACTUALIZADO = 'perfil-actualizado'

export function avisarPerfilActualizado(perfil: Perfil): void {
  emitirEvento(EVENTO_PERFIL_ACTUALIZADO, {
    nombres: perfil.nombres,
    apellidos: perfil.apellidos,
    fotoUrl: perfil.fotoUrl,
  })
}
