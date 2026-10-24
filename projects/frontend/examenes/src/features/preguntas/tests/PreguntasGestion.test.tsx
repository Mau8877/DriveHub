import { describe, expect, it } from 'vitest'
import { render } from '@testing-library/react'

import { ESQUEMA_PREGUNTAS } from '../schemas/preguntas.esquema'
import { PreguntasGestion } from '../screens/PreguntasGestion'

describe('PreguntasGestion', () => {
  it('monta <dh-gestion> con el esquema de preguntas como propiedad', () => {
    const { container } = render(<PreguntasGestion />)

    const elemento = container.querySelector('dh-gestion') as
      (HTMLElement & { esquema: unknown }) | null
    expect(elemento).not.toBeNull()
    expect(customElements.get('dh-gestion')).toBeDefined()
    expect(elemento!.esquema).toBe(ESQUEMA_PREGUNTAS)
  })
})
