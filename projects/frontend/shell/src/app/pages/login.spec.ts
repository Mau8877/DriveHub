import { TestBed } from '@angular/core/testing'
import { ActivatedRoute } from '@angular/router'

import { routes } from '../app.routes'
import { Login } from './login'

async function crearLogin(query: Record<string, string>) {
  await TestBed.configureTestingModule({
    imports: [Login],
    providers: [
      {
        provide: ActivatedRoute,
        useValue: { snapshot: { queryParamMap: new Map(Object.entries(query)) } },
      },
    ],
  }).compileComponents()
  const fixture = TestBed.createComponent(Login)
  await fixture.whenStable()
  return fixture.nativeElement as HTMLElement
}

describe('Login', () => {
  it('con ?registrado=1 muestra el aviso de cuenta creada', async () => {
    const html = await crearLogin({ registrado: '1' })

    expect(html.textContent).toContain('Tu cuenta fue creada')
  })

  it('sin el parámetro no muestra el aviso', async () => {
    const html = await crearLogin({})

    expect(html.textContent).not.toContain('Tu cuenta fue creada')
  })
})

describe('rutas del shell', () => {
  it('/registro es de primer nivel, fuera del layout', () => {
    const registro = routes.find((r) => r.path === 'registro')
    const layout = routes.find((r) => r.path === '')

    expect(registro?.loadChildren).toBeDefined()
    expect(layout?.children?.some((r) => r.path === 'registro')).toBe(false)
  })
})
