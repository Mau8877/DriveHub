import { TestBed } from '@angular/core/testing'
import {
  type ActivatedRouteSnapshot,
  type RouterStateSnapshot,
  Router,
  UrlTree,
  provideRouter,
} from '@angular/router'

import { rolGuard } from './rol.guard'
import { SesionService } from './sesion.service'

describe('rolGuard', () => {
  let roles: string[]

  beforeEach(() => {
    roles = []
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: SesionService, useValue: { roles: () => roles } }],
    })
  })

  const ejecutar = (...permitidos: string[]) =>
    TestBed.runInInjectionContext(() =>
      rolGuard(...permitidos)({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot)
    )

  it('deja pasar a quien tiene el rol pedido', () => {
    roles = ['SUPER_ADMIN']

    expect(ejecutar('SUPER_ADMIN')).toBe(true)
  })

  it('deja pasar si tiene alguno de varios roles pedidos', () => {
    roles = ['ADMIN_ESCUELA']

    expect(ejecutar('SUPER_ADMIN', 'ADMIN_ESCUELA')).toBe(true)
  })

  it('con otro rol lleva al inicio', () => {
    roles = ['ADMIN_ESCUELA']

    const resultado = ejecutar('SUPER_ADMIN') as UrlTree

    expect(TestBed.inject(Router).serializeUrl(resultado)).toBe('/')
  })

  it('sin roles lleva al inicio', () => {
    const resultado = ejecutar('SUPER_ADMIN') as UrlTree

    expect(TestBed.inject(Router).serializeUrl(resultado)).toBe('/')
  })
})
