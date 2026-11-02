import { Logger } from '@nestjs/common'
import { type JWTVerifyGetKey, jwtVerify } from 'jose'

/** La persona autenticada, armada desde los claims del JWT. */
export interface UsuarioAutenticado {
  // = sub del token = codigo_usuario de Identidad (usuarioCodigo en las tablas)
  codigo: string
  roles: string[]
  escuelaCodigo?: string
}

/**
 * Valida el access token (RS256) con las claves públicas de Identidad (JWKS).
 *
 * * Devuelve null ante cualquier problema (firma, vencido, otro emisor, JWKS
 * * caído): el motivo no se le cuenta al cliente. Mismo criterio que el
 * * VerificadorJwt de Contenido y de Asistente IA.
 * * `jose` cachea las claves y vuelve a pedirlas si ve un kid desconocido.
 */
export class VerificadorJwt {
  private readonly logger = new Logger(VerificadorJwt.name)

  constructor(
    private readonly obtenerClave: JWTVerifyGetKey,
    private readonly emisor: string
  ) {}

  async verificar(token: string): Promise<UsuarioAutenticado | null> {
    try {
      const { payload } = await jwtVerify(token, this.obtenerClave, {
        issuer: this.emisor,
        algorithms: ['RS256'],
        requiredClaims: ['exp', 'iss', 'sub'],
      })
      if (!payload.sub) return null
      const roles = Array.isArray(payload.roles) ? payload.roles.map(String) : []
      const escuela = typeof payload.escuela === 'string' ? payload.escuela : undefined
      return { codigo: payload.sub, roles, escuelaCodigo: escuela }
    } catch (error) {
      this.logger.warn({
        message: 'Token rechazado',
        module: 'seguridad',
        tipo: error instanceof Error ? error.name : 'desconocido',
      })
      return null
    }
  }
}
