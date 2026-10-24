import { describe, expect, it } from 'vitest'
import { render } from '@testing-library/react'

import { ESQUEMA_MANUALES } from '../schemas/manuales.esquema'
import { ManualesGestion } from '../screens/ManualesGestion'

describe('ManualesGestion', () => {
  it('monta <dh-gestion> con el esquema de manuales como propiedad', () => {
    const { container } = render(<ManualesGestion />)

    const elemento = container.querySelector('dh-gestion') as
      (HTMLElement & { esquema: unknown }) | null
    expect(elemento).not.toBeNull()
    expect(customElements.get('dh-gestion')).toBeDefined()
    expect(elemento!.esquema).toBe(ESQUEMA_MANUALES)
  })
})
