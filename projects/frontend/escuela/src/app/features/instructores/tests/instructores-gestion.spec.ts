import { TestBed } from '@angular/core/testing'

import { ESQUEMA_INSTRUCTORES } from '../schemas/instructores.esquema'
import { InstructoresGestion } from '../screens/instructores-gestion'

describe('InstructoresGestion', () => {
  it('monta <dh-gestion> con el esquema de instructores como propiedad', async () => {
    const fixture = TestBed.createComponent(InstructoresGestion)
    fixture.detectChanges()

    const elemento = (fixture.nativeElement as HTMLElement).querySelector('dh-gestion') as
      (HTMLElement & { esquema: unknown }) | null
    expect(elemento).not.toBeNull()
    expect(customElements.get('dh-gestion')).toBeDefined()
    expect(elemento!.esquema).toBe(ESQUEMA_INSTRUCTORES)
  })
})
