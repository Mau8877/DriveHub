import { TestBed } from '@angular/core/testing'

import { ESQUEMA_ESCUELAS } from '../schemas/escuelas.esquema'
import { EscuelasGestion } from '../screens/escuelas-gestion'

describe('EscuelasGestion', () => {
  it('monta <dh-gestion> con el esquema de escuelas como propiedad', async () => {
    const fixture = TestBed.createComponent(EscuelasGestion)
    fixture.detectChanges()

    const elemento = (fixture.nativeElement as HTMLElement).querySelector('dh-gestion') as
      (HTMLElement & { esquema: unknown }) | null
    expect(elemento).not.toBeNull()
    expect(customElements.get('dh-gestion')).toBeDefined()
    expect(elemento!.esquema).toBe(ESQUEMA_ESCUELAS)
  })
})
