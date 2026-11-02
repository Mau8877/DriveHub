import { SetMetadata } from '@nestjs/common'

export const CLAVE_ROLES = 'roles'

/** Roles que pueden usar el endpoint; lo hace cumplir RolesGuard (después de JwtAuthGuard). */
export const Roles = (...roles: string[]) => SetMetadata(CLAVE_ROLES, roles)
