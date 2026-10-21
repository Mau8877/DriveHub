/**
 * Nombres de los roles del sistema, tal como vienen en el claim `roles` del JWT.
 *
 * * Son datos del backend (tabla `rol` de Identidad): acá solo viven como constantes para no
 * * escribir textos sueltos en el menú ni en los guards.
 */
export const ROLES = {
  SUPER_ADMIN: 'SUPER_ADMIN',
  ADMIN_ESCUELA: 'ADMIN_ESCUELA',
  INSTRUCTOR: 'INSTRUCTOR',
  ESTUDIANTE: 'ESTUDIANTE',
} as const
