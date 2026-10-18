import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { DataTablePagination } from './data-table-pagination'
import type { DataTablePageMeta } from './types'

const meta: DataTablePageMeta = { page: 2, pageSize: 20, totalItems: 45, totalPages: 3 }

describe('DataTablePagination', () => {
  it('deshabilita "anterior" en la primera página y "siguiente" en la última', () => {
    render(
      <DataTablePagination
        meta={{ ...meta, page: 1 }}
        page={1}
        onPageChange={vi.fn()}
        onPageSizeChange={vi.fn()}
      />,
    )
    expect(screen.getByRole('button', { name: 'Página anterior' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Página siguiente' })).toBeEnabled()
  })

  it('llama a onPageChange con el valor correcto al hacer click en siguiente/anterior', async () => {
    const onPageChange = vi.fn()
    const user = userEvent.setup()

    render(
      <DataTablePagination meta={meta} page={2} onPageChange={onPageChange} onPageSizeChange={vi.fn()} />,
    )

    await user.click(screen.getByRole('button', { name: 'Página siguiente' }))
    expect(onPageChange).toHaveBeenCalledWith(3)

    await user.click(screen.getByRole('button', { name: 'Página anterior' }))
    expect(onPageChange).toHaveBeenCalledWith(1)
  })

  it('renderiza un botón por número de página y marca la actual con aria-current', () => {
    render(
      <DataTablePagination meta={meta} page={2} onPageChange={vi.fn()} onPageSizeChange={vi.fn()} />,
    )

    expect(screen.getByRole('button', { name: 'Página 1' })).toBeInTheDocument()
    const current = screen.getByRole('button', { name: 'Página 2' })
    expect(current).toHaveAttribute('aria-current', 'page')
    expect(screen.getByRole('button', { name: 'Página 3' })).toBeInTheDocument()
  })

  it('al hacer click en un número de página, dispara onPageChange con ese número', async () => {
    const onPageChange = vi.fn()
    const user = userEvent.setup()

    render(
      <DataTablePagination meta={meta} page={2} onPageChange={onPageChange} onPageSizeChange={vi.fn()} />,
    )

    await user.click(screen.getByRole('button', { name: 'Página 3' }))

    expect(onPageChange).toHaveBeenCalledWith(3)
  })

  it('sin meta, no rompe: botones anterior/siguiente deshabilitados, muestra la página 1', () => {
    render(
      <DataTablePagination meta={undefined} page={1} onPageChange={vi.fn()} onPageSizeChange={vi.fn()} />,
    )

    expect(screen.getByRole('button', { name: 'Página anterior' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Página siguiente' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Página 1' })).toBeInTheDocument()
  })

  it('con totalItems en 0 (totalPages: 0), muestra un rango de una sola página en vez de romper', () => {
    render(
      <DataTablePagination
        meta={{ page: 1, pageSize: 20, totalItems: 0, totalPages: 0 }}
        page={1}
        onPageChange={vi.fn()}
        onPageSizeChange={vi.fn()}
      />,
    )

    expect(screen.getByRole('button', { name: 'Página 1' })).toBeInTheDocument()
  })

  it('con un rango largo de páginas, muestra "…" en vez de todos los números', () => {
    render(
      <DataTablePagination
        meta={{ page: 1, pageSize: 20, totalItems: 400, totalPages: 20 }}
        page={1}
        onPageChange={vi.fn()}
        onPageSizeChange={vi.fn()}
      />,
    )

    expect(screen.getByText('…')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Página 10' })).not.toBeInTheDocument()
  })

  it('deshabilita ambos botones mientras isLoading', () => {
    render(
      <DataTablePagination
        meta={meta}
        page={2}
        onPageChange={vi.fn()}
        onPageSizeChange={vi.fn()}
        isLoading
      />,
    )

    expect(screen.getByRole('button', { name: 'Página anterior' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Página siguiente' })).toBeDisabled()
  })
})
