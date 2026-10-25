import { TestBed } from '@angular/core/testing'

import { ESQUEMA_PLANES } from '../schemas/planes.esquema'
import { PlanesGestion } from '../screens/planes-gestion'

describe('PlanesGestion', () => {
  it('monta <dh-gestion> con el esquema de planes como propiedad', async () => {
    const fixture = TestBed.createComponent(PlanesGestion)
    fixture.detectChanges()

    const elemento = (fixture.nativeElement as HTMLElement).querySelector('dh-gestion') as
      (HTMLElement & { esquema: unknown }) | null
    expect(elemento).not.toBeNull()
    expect(customElements.get('dh-gestion')).toBeDefined()
    expect(elemento!.esquema).toBe(ESQUEMA_PLANES)
  })
})
