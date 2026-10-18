import type { ReactNode } from 'react'
import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { TooltipProvider } from '@/components/ui/tooltip'
import { RowActionButton } from './row-action-button'

function renderWithProvider(ui: ReactNode) {
  return render(<TooltipProvider>{ui}</TooltipProvider>)
}

describe('RowActionButton', () => {
  it('usa label como aria-label del botón', () => {
    renderWithProvider(<RowActionButton icon={<span>i</span>} label="Editar" />)
    expect(screen.getByRole('button', { name: 'Editar' })).toBeInTheDocument()
  })

  it('dispara onClick al hacer click', async () => {
    const onClick = vi.fn()
    const user = userEvent.setup()
    renderWithProvider(<RowActionButton icon={<span>i</span>} label="Editar" onClick={onClick} />)

    await user.click(screen.getByRole('button', { name: 'Editar' }))

    expect(onClick).toHaveBeenCalledTimes(1)
  })

  it('muestra el label como tooltip al hacer hover', async () => {
    const user = userEvent.setup()
    renderWithProvider(<RowActionButton icon={<span>i</span>} label="Eliminar" />)

    await user.hover(screen.getByRole('button', { name: 'Eliminar' }))

    expect(await screen.findByText('Eliminar')).toBeInTheDocument()
  })

  it('con variant destructive, aplica el color de destructive', () => {
    renderWithProvider(<RowActionButton icon={<span>i</span>} label="Eliminar" variant="destructive" />)
    expect(screen.getByRole('button', { name: 'Eliminar' })).toHaveClass('text-destructive')
  })
})
