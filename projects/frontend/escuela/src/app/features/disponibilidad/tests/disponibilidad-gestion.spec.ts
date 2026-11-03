import { TestBed } from '@angular/core/testing'

import { ESQUEMA_DISPONIBILIDAD } from '../schemas/disponibilidad.esquema'
import { DisponibilidadGestion } from '../screens/disponibilidad-gestion'

describe('DisponibilidadGestion', () => {
  it('monta <dh-gestion> con el esquema de disponibilidad como propiedad', async () => {
    const fixture = TestBed.createComponent(DisponibilidadGestion)
    fixture.detectChanges()

    const elemento = (fixture.nativeElement as HTMLElement).querySelector('dh-gestion') as
      (HTMLElement & { esquema: unknown }) | null
    expect(elemento).not.toBeNull()
    expect(elemento!.esquema).toBe(ESQUEMA_DISPONIBILIDAD)
  })
})
