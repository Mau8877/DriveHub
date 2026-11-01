import { describe, expect, it } from 'vitest'
import { render } from '@testing-library/react'

import { ESQUEMA_CONDUCCION } from '../schemas/conduccion.esquema'
import { ConduccionGestion } from '../screens/ConduccionGestion'

describe('ConduccionGestion', () => {
  it('monta <dh-gestion> con el esquema de examen de conducción como propiedad', () => {
    const { container } = render(<ConduccionGestion />)

    const elemento = container.querySelector('dh-gestion') as
      (HTMLElement & { esquema: unknown }) | null
    expect(elemento).not.toBeNull()
    expect(elemento!.esquema).toBe(ESQUEMA_CONDUCCION)
  })
})
