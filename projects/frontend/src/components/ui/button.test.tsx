import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Button } from './button'

describe('Button', () => {
  it('renderiza el texto que recibe', () => {
    render(<Button>Guardar</Button>)
    expect(screen.getByRole('button', { name: 'Guardar' })).toBeInTheDocument()
  })

  it('dispara onClick al hacer click', async () => {
    const onClick = vi.fn()
    render(<Button onClick={onClick}>Guardar</Button>)

    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }))

    expect(onClick).toHaveBeenCalledTimes(1)
  })

  it('no dispara onClick si está disabled', async () => {
    const onClick = vi.fn()
    render(
      <Button onClick={onClick} disabled>
        Guardar
      </Button>,
    )

    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }))

    expect(onClick).not.toHaveBeenCalled()
  })
})
