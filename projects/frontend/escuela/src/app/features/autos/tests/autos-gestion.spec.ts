import { TestBed } from '@angular/core/testing'

import { ESQUEMA_AUTOS } from '../schemas/autos.esquema'
import { AutosGestion } from '../screens/autos-gestion'

describe('AutosGestion', () => {
  it('monta <dh-gestion> con el esquema de autos como propiedad', async () => {
    const fixture = TestBed.createComponent(AutosGestion)
    fixture.detectChanges()

    const elemento = (fixture.nativeElement as HTMLElement).querySelector('dh-gestion') as
      (HTMLElement & { esquema: unknown }) | null
    expect(elemento).not.toBeNull()
    expect(elemento!.esquema).toBe(ESQUEMA_AUTOS)
  })
})
