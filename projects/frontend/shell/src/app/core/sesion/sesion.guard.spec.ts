import { TestBed } from '@angular/core/testing'
import {
  type ActivatedRouteSnapshot,
  type RouterStateSnapshot,
  Router,
  UrlTree,
  provideRouter,
} from '@angular/router'

import { loginGuard, sesionGuard } from './sesion.guard'
import { SesionService } from './sesion.service'

describe('guards de sesión', () => {
  let autenticado: boolean

  beforeEach(() => {
    autenticado = false
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: SesionService, useValue: { autenticado: () => autenticado } },
      ],
    })
  })

  const ejecutar = (guard: typeof sesionGuard, url = '/escuela/vehiculos') =>
    TestBed.runInInjectionContext(() =>
      guard({} as ActivatedRouteSnapshot, { url } as RouterStateSnapshot)
    )

  it('sesionGuard deja pasar a quien tiene sesión', () => {
    autenticado = true

    expect(ejecutar(sesionGuard)).toBe(true)
  })

  it('sesionGuard sin sesión lleva a /login recordando la URL pedida', () => {
    const resultado = ejecutar(sesionGuard) as UrlTree

    expect(TestBed.inject(Router).serializeUrl(resultado)).toBe(
      '/login?redirect=%2Fescuela%2Fvehiculos'
    )
  })

  it('loginGuard sin sesión muestra el login', () => {
    expect(ejecutar(loginGuard)).toBe(true)
  })

  it('loginGuard con sesión lleva al inicio', () => {
    autenticado = true

    const resultado = ejecutar(loginGuard) as UrlTree

    expect(TestBed.inject(Router).serializeUrl(resultado)).toBe('/')
  })
})
