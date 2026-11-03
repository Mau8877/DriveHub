import { Module } from '@nestjs/common'
import { ConfigService } from '@nestjs/config'
import { createRemoteJWKSet } from 'jose'

import { CredencialServicioGuard } from './credencial-servicio.guard.js'
import { JwtAuthGuard } from './jwt-auth.guard.js'
import { RolesGuard } from './roles.guard.js'
import { VerificadorJwt } from './verificador-jwt.js'

/**
 * Seguridad del servicio: JWT de las personas (JWKS de Identidad) y
 * credencial de servicio de las rutas internas.
 */
@Module({
  providers: [
    {
      provide: VerificadorJwt,
      inject: [ConfigService],
      useFactory: (config: ConfigService) => {
        const identidad = config.get<string>('IDENTIDAD_URL', 'http://localhost:8081')
        const jwks = createRemoteJWKSet(new URL(`${identidad}/.well-known/jwks.json`), {
          cooldownDuration: 30_000,
          timeoutDuration: 3_000,
        })
        return new VerificadorJwt(jwks, config.get<string>('JWT_EMISOR', 'drivehub-identidad'))
      },
    },
    JwtAuthGuard,
    RolesGuard,
    CredencialServicioGuard,
  ],
  exports: [VerificadorJwt, JwtAuthGuard, RolesGuard, CredencialServicioGuard],
})
export class SecurityModule {}
