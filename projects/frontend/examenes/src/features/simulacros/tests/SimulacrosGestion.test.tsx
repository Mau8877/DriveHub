import { describe, expect, it } from 'vitest'
import { render } from '@testing-library/react'

import { ESQUEMA_SIMULACROS } from '../schemas/simulacros.esquema'
import { SimulacrosGestion } from '../screens/SimulacrosGestion'

describe('SimulacrosGestion', () => {
  it('monta <dh-gestion> con el esquema de simulacros como propiedad', () => {
    const { container } = render(<SimulacrosGestion />)

    const elemento = container.querySelector('dh-gestion') as
      (HTMLElement & { esquema: unknown }) | null
    expect(elemento).not.toBeNull()
    expect(customElements.get('dh-gestion')).toBeDefined()
    expect(elemento!.esquema).toBe(ESQUEMA_SIMULACROS)
  })
})
