import { CUSTOM_ELEMENTS_SCHEMA, Component } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms'
import '@drivehub/ui'

import { DhValor } from './dh-valor.directive'

@Component({
  imports: [ReactiveFormsModule, DhValor],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `
    <form [formGroup]="form">
      <dh-select id="sexo" formControlName="sexo"></dh-select>
      <dh-fecha id="nacimiento" formControlName="nacimiento"></dh-fecha>
    </form>
  `,
})
class Anfitrion {
  readonly form = new FormGroup({
    sexo: new FormControl('FEMENINO', { nonNullable: true }),
    nacimiento: new FormControl('2000-01-01', { nonNullable: true }),
  })
}

type Elemento = HTMLElement & { valor: string; deshabilitado: boolean }

describe('DhValor', () => {
  function crear() {
    const fixture = TestBed.createComponent(Anfitrion)
    fixture.detectChanges()
    const html = fixture.nativeElement as HTMLElement
    return {
      fixture,
      form: fixture.componentInstance.form,
      sexo: html.querySelector('#sexo') as Elemento,
      nacimiento: html.querySelector('#nacimiento') as Elemento,
    }
  }

  const cambiar = (el: HTMLElement, valor: string) =>
    el.dispatchEvent(
      new CustomEvent('dh-cambio', { detail: { valor }, bubbles: true, composed: true })
    )

  it('el valor inicial del control llega al componente (lista y fecha)', () => {
    const { sexo, nacimiento } = crear()

    expect(sexo.valor).toBe('FEMENINO')
    expect(nacimiento.valor).toBe('2000-01-01')
  })

  it('un cambio del componente actualiza el control', () => {
    const { sexo, nacimiento, form } = crear()

    cambiar(sexo, 'MASCULINO')
    cambiar(nacimiento, '1999-12-31')

    expect(form.getRawValue()).toEqual({ sexo: 'MASCULINO', nacimiento: '1999-12-31' })
  })

  it('un cambio del programa (setValue o patchValue) llega al componente', () => {
    const { sexo, nacimiento, form } = crear()

    form.setValue({ sexo: 'MASCULINO', nacimiento: '1980-02-03' })

    expect(sexo.valor).toBe('MASCULINO')
    expect(nacimiento.valor).toBe('1980-02-03')
  })

  it('reset con un valor lo escribe en el componente, y sin valor vuelve al inicial', () => {
    const { sexo, form } = crear()
    cambiar(sexo, 'MASCULINO')

    form.controls.sexo.reset('')
    expect(sexo.valor).toBe('')

    form.controls.sexo.reset()
    expect(sexo.valor).toBe('FEMENINO')
  })

  it('un valor nulo se escribe como texto vacío', () => {
    const { sexo, form } = crear()

    form.controls.sexo.setValue(null as unknown as string)

    expect(sexo.valor).toBe('')
  })

  it('salir del componente (dh-blur) marca el control como tocado', () => {
    const { sexo, nacimiento, form } = crear()
    expect(form.controls.sexo.touched).toBe(false)

    sexo.dispatchEvent(new CustomEvent('dh-blur', { bubbles: true, composed: true }))
    expect(form.controls.sexo.touched).toBe(true)
    expect(form.controls.nacimiento.touched).toBe(false)

    nacimiento.dispatchEvent(new CustomEvent('dh-blur', { bubbles: true, composed: true }))
    expect(form.controls.nacimiento.touched).toBe(true)
  })

  it('deshabilitar el control deshabilita el componente, y habilitarlo lo vuelve a habilitar', () => {
    const { sexo, form } = crear()

    form.controls.sexo.disable()
    expect(sexo.deshabilitado).toBe(true)

    form.controls.sexo.enable()
    expect(sexo.deshabilitado).toBe(false)
  })

  it('un cambio hecho desde el componente deja el control como modificado (dirty)', () => {
    const { sexo, form } = crear()

    cambiar(sexo, 'MASCULINO')

    expect(form.controls.sexo.dirty).toBe(true)
    expect(form.controls.sexo.value).toBe('MASCULINO')
  })
})
