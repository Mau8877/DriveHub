import type { ExecutionContext } from '@nestjs/common'
import type { ConfigService } from '@nestjs/config'
import { describe, expect, it } from 'vitest'

import { UnauthorizedException } from '../exceptions/business.exception.js'
import { CredencialServicioGuard } from './credencial-servicio.guard.js'

function contexto(cabecera?: string): ExecutionContext {
  const request = { path: '/internal/v1/notificaciones', header: () => cabecera }
  return { switchToHttp: () => ({ getRequest: () => request }) } as unknown as ExecutionContext
}

function guard(credencial?: string): CredencialServicioGuard {
  const config = { get: () => credencial } as unknown as ConfigService
  return new CredencialServicioGuard(config)
}

describe('CredencialServicioGuard', () => {
  it('deja pasar con la credencial correcta', () => {
    expect(guard('secreto').canActivate(contexto('secreto'))).toBe(true)
  })

  it('rechaza una credencial distinta con ERR_NOT_01', () => {
    expect(() => guard('secreto').canActivate(contexto('otro-valor'))).toThrow(UnauthorizedException)
    try {
      guard('secreto').canActivate(contexto('otro-valor'))
    } catch (error) {
      expect((error as UnauthorizedException).errorCode).toBe('ERR_NOT_01')
    }
  })

  it('rechaza si falta el header', () => {
    expect(() => guard('secreto').canActivate(contexto(undefined))).toThrow(UnauthorizedException)
  })

  it('rechaza todo si no hay credencial configurada, incluso con header vacío', () => {
    expect(() => guard(undefined).canActivate(contexto(''))).toThrow(UnauthorizedException)
    expect(() => guard('').canActivate(contexto(''))).toThrow(UnauthorizedException)
  })
})
