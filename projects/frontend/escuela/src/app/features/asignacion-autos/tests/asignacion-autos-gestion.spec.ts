import { TestBed } from '@angular/core/testing'

import { ESQUEMA_ASIGNACION_AUTOS } from '../schemas/asignacion-autos.esquema'
import { AsignacionAutosGestion } from '../screens/asignacion-autos-gestion'

describe('AsignacionAutosGestion', () => {
  it('monta <dh-gestion> con el esquema de asignación de autos como propiedad', async () => {
    const fixture = TestBed.createComponent(AsignacionAutosGestion)
    fixture.detectChanges()

    const elemento = (fixture.nativeElement as HTMLElement).querySelector('dh-gestion') as
      (HTMLElement & { esquema: unknown }) | null
    expect(elemento).not.toBeNull()
    expect(elemento!.esquema).toBe(ESQUEMA_ASIGNACION_AUTOS)
  })
})
