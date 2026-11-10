import { ForbiddenException } from '../../../common/exceptions/business.exception.js'
import { ErrorCodes } from '../../../common/exceptions/error-codes.js'
import type { UsuarioAutenticado } from '../../../common/security/verificador-jwt.js'

/** Los roles que tienen bandeja: la propia (CU38) o la de su escuela (CU53). */
export const ROLES_CON_BANDEJA = ['ESTUDIANTE', 'INSTRUCTOR', 'ADMIN_ESCUELA'] as const

/**
 * De qué bandeja lee quien hace el pedido, siempre a partir del token.
 *
 * * El Administrador de escuela lee la de su escuela (claim `escuela`), que comparte con los demás Administradores.
 * * Estudiante e Instructor leen la suya (`sub`).
 * ! Un Administrador sin escuela en el token no tiene bandeja: 403, igual que un rol sin permiso.
 */
export function bandejaDe(usuario: UsuarioAutenticado): string {
  if (!usuario.roles.includes('ADMIN_ESCUELA')) return usuario.codigo
  if (!usuario.escuelaCodigo) {
    throw new ForbiddenException(ErrorCodes.SIN_PERMISO, 'No tienes permiso para ver estos avisos')
  }
  return usuario.escuelaCodigo
}
