import type { UsuarioSesion } from './sesion.types'

/**
 * Evento del navegador con el que un remoto avisa al shell que la persona cambió su nombre o su
 * foto (CU06). El remoto usa el mismo nombre (constante propia, los remotos no importan del shell).
 */
export const EVENTO_PERFIL_ACTUALIZADO = 'perfil-actualizado'

export type PerfilActualizado = Pick<UsuarioSesion, 'nombres' | 'apellidos' | 'fotoUrl'>
