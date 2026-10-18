import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { DataTableToolbar } from './data-table-toolbar'
import type { DataTableToolbarFilter } from './data-table-toolbar'

describe('DataTableToolbar', () => {
  it('sin onSearchChange, no renderiza el buscador', () => {
    render(<DataTableToolbar />)
    expect(screen.queryByPlaceholderText('Buscar...')).not.toBeInTheDocument()
  })

  it('con onSearchChange, renderiza el buscador y dispara el callback al escribir', async () => {
    const onSearchChange = vi.fn()
    const user = userEvent.setup()

    render(<DataTableToolbar searchValue="" onSearchChange={onSearchChange} />)

    await user.type(screen.getByPlaceholderText('Buscar...'), 'a')

    expect(onSearchChange).toHaveBeenCalledWith('a')
  })

  it('sin actions, no renderiza el contenedor de acciones', () => {
    const { container } = render(<DataTableToolbar />)
    expect(container.querySelectorAll('button')).toHaveLength(0)
  })

  it('con actions, renderiza el contenido pasado', () => {
    render(<DataTableToolbar actions={<button type="button">Nuevo</button>} />)
    expect(screen.getByRole('button', { name: 'Nuevo' })).toBeInTheDocument()
  })

  it('renderiza un Select por cada filtro, con su label como aria-label', () => {
    const filters: DataTableToolbarFilter[] = [
      {
        id: 'rol',
        label: 'Rol',
        value: undefined,
        onValueChange: vi.fn(),
        options: [
          { label: 'Administrador', value: 'ADMIN' },
          { label: 'Usuario', value: 'USUARIO' },
        ],
      },
    ]

    render(<DataTableToolbar filters={filters} />)

    expect(screen.getByRole('combobox', { name: 'Rol' })).toBeInTheDocument()
  })

  it('muestra el label del filtro como título visible encima del select', () => {
    const filters: DataTableToolbarFilter[] = [
      {
        id: 'rol',
        label: 'Rol',
        value: undefined,
        onValueChange: vi.fn(),
        options: [{ label: 'Administrador', value: 'ADMIN' }],
      },
    ]

    render(<DataTableToolbar filters={filters} />)

    expect(screen.getByText('Rol')).toBeInTheDocument()
  })

  it('al elegir una opción de un filtro, dispara onValueChange con ese value', async () => {
    const onValueChange = vi.fn()
    const user = userEvent.setup()
    const filters: DataTableToolbarFilter[] = [
      {
        id: 'rol',
        label: 'Rol',
        value: undefined,
        onValueChange,
        options: [{ label: 'Administrador', value: 'ADMIN' }],
      },
    ]

    render(<DataTableToolbar filters={filters} />)

    await user.click(screen.getByRole('combobox', { name: 'Rol' }))
    await user.click(await screen.findByText('Administrador'))

    expect(onValueChange).toHaveBeenCalledWith('ADMIN')
  })

  it('sin búsqueda ni filtros activos, no muestra "Limpiar filtros"', () => {
    render(<DataTableToolbar searchValue="" onSearchChange={vi.fn()} />)
    expect(screen.queryByRole('button', { name: 'Limpiar filtros' })).not.toBeInTheDocument()
  })

  it('con búsqueda activa, muestra "Limpiar filtros" y al hacer click la vacía', async () => {
    const onSearchChange = vi.fn()
    const user = userEvent.setup()

    render(<DataTableToolbar searchValue="tomate" onSearchChange={onSearchChange} />)

    await user.click(screen.getByRole('button', { name: 'Limpiar filtros' }))

    expect(onSearchChange).toHaveBeenCalledWith('')
  })

  it('con un filtro activo, muestra "Limpiar filtros" y al hacer click llama onValueChange(undefined) de cada filtro', async () => {
    const onValueChange = vi.fn()
    const user = userEvent.setup()
    const filters: DataTableToolbarFilter[] = [
      {
        id: 'rol',
        label: 'Rol',
        value: 'ADMIN',
        onValueChange,
        options: [{ label: 'Administrador', value: 'ADMIN' }],
      },
    ]

    render(<DataTableToolbar filters={filters} />)

    await user.click(screen.getByRole('button', { name: 'Limpiar filtros' }))

    expect(onValueChange).toHaveBeenCalledWith(undefined)
  })
})
