import { TestBed } from '@angular/core/testing'

import { ESQUEMA_ESTUDIANTES } from '../schemas/estudiantes.esquema'
import { EstudiantesGestion } from '../screens/estudiantes-gestion'

describe('EstudiantesGestion', () => {
  it('monta <dh-gestion> con el esquema de estudiantes como propiedad', async () => {
    const fixture = TestBed.createComponent(EstudiantesGestion)
    fixture.detectChanges()

    const elemento = (fixture.nativeElement as HTMLElement).querySelector('dh-gestion') as
      (HTMLElement & { esquema: unknown }) | null
    expect(elemento).not.toBeNull()
    expect(customElements.get('dh-gestion')).toBeDefined()
    expect(elemento!.esquema).toBe(ESQUEMA_ESTUDIANTES)
  })
})
