import type { ExecutionContext } from '@nestjs/common'
import { Reflector } from '@nestjs/core'
import { describe, expect, it } from 'vitest'

import { ForbiddenException } from '../exceptions/business.exception.js'
import { Roles } from './roles.decorator.js'
import { RolesGuard } from './roles.guard.js'

class ControllerDePrueba {
  @Roles('ESTUDIANTE', 'INSTRUCTOR')
  conRoles() {}

  sinRoles() {}
}

function contexto(metodo: 'conRoles' | 'sinRoles', roles?: string[]): ExecutionContext {
  const request = { usuario: roles ? { codigo: 'u1', roles } : undefined }
  return {
    getHandler: () => ControllerDePrueba.prototype[metodo],
    getClass: () => ControllerDePrueba,
    switchToHttp: () => ({ getRequest: () => request }),
  } as unknown as ExecutionContext
}

describe('RolesGuard', () => {
  const guard = new RolesGuard(new Reflector())

  it('deja pasar a quien tiene alguno de los roles permitidos', () => {
    expect(guard.canActivate(contexto('conRoles', ['INSTRUCTOR']))).toBe(true)
    expect(guard.canActivate(contexto('conRoles', ['OTRO', 'ESTUDIANTE']))).toBe(true)
  })

  it('rechaza con ERR_SYS_05 a quien tiene otro rol', () => {
    const intento = () => guard.canActivate(contexto('conRoles', ['ADMIN_ESCUELA']))
    expect(intento).toThrow(ForbiddenException)
    expect(intento).toThrowError(expect.objectContaining({ errorCode: 'ERR_SYS_05' }))
  })

  it('rechaza a quien no tiene roles o no tiene sesión cargada', () => {
    expect(() => guard.canActivate(contexto('conRoles', []))).toThrow(ForbiddenException)
    expect(() => guard.canActivate(contexto('conRoles'))).toThrow(ForbiddenException)
  })

  it('no exige nada si el endpoint no declara @Roles', () => {
    expect(guard.canActivate(contexto('sinRoles', []))).toBe(true)
  })
})
