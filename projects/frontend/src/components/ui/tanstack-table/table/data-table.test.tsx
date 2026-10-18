import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { renderHook } from '@testing-library/react'
import { useReactTable, getCoreRowModel } from '@tanstack/react-table'
import type { ColumnDef } from '@tanstack/react-table'
import { DataTable } from './data-table'

interface Row {
  id: string
  nombre: string
  email: string
}

const columns: ColumnDef<Row, unknown>[] = [
  { accessorKey: 'nombre', header: 'Nombre' },
  { accessorKey: 'email', header: 'Correo' },
]

function useTestTable(data: Row[]) {
  return renderHook(function useWrapper() {
    return useReactTable({
      data,
      columns,
      getCoreRowModel: getCoreRowModel(),
    })
  }).result.current
}

function useTestTableWithPagination(data: Row[], pageIndex: number, pageSize: number) {
  return renderHook(function useWrapper() {
    return useReactTable({
      data,
      columns,
      state: { pagination: { pageIndex, pageSize } },
      manualPagination: true,
      onPaginationChange: () => {},
      getCoreRowModel: getCoreRowModel(),
    })
  }).result.current
}

describe('DataTable', () => {
  it('en loading, renderiza filas skeleton en vez de datos', () => {
    const table = useTestTable([{ id: '1', nombre: 'Ana', email: 'ana@test.com' }])

    render(<DataTable table={table} isLoading isError={false} skeletonRowCount={3} />)

    expect(screen.queryByText('Ana')).not.toBeInTheDocument()
    expect(document.querySelectorAll('[data-slot="skeleton"]')).toHaveLength(3 * columns.length)
  })

  it('en error, muestra el mensaje y no la tabla de datos', () => {
    const table = useTestTable([{ id: '1', nombre: 'Ana', email: 'ana@test.com' }])

    render(<DataTable table={table} isLoading={false} isError errorMessage="Falló la carga" />)

    expect(screen.getByText('Falló la carga')).toBeInTheDocument()
    expect(screen.queryByText('Ana')).not.toBeInTheDocument()
  })

  it('sin datos (no loading/error), muestra el mensaje de vacío', () => {
    const table = useTestTable([])

    render(<DataTable table={table} isLoading={false} isError={false} />)

    expect(screen.getByText('Sin resultados')).toBeInTheDocument()
  })

  it('caso normal, renderiza filas y celdas de los datos', () => {
    const table = useTestTable([
      { id: '1', nombre: 'Ana', email: 'ana@test.com' },
      { id: '2', nombre: 'Beto', email: 'beto@test.com' },
    ])

    render(<DataTable table={table} isLoading={false} isError={false} />)

    expect(screen.getByText('Ana')).toBeInTheDocument()
    expect(screen.getByText('beto@test.com')).toBeInTheDocument()
  })

  it('con showRowNumber, antepone la columna "#" con el número 1-based de la página actual', () => {
    const table = useTestTableWithPagination(
      [
        { id: '1', nombre: 'Ana', email: 'ana@test.com' },
        { id: '2', nombre: 'Beto', email: 'beto@test.com' },
      ],
      0,
      2,
    )

    render(<DataTable table={table} isLoading={false} isError={false} showRowNumber />)

    expect(screen.getByText('#')).toBeInTheDocument()
    expect(screen.getByText('1')).toBeInTheDocument()
    expect(screen.getByText('2')).toBeInTheDocument()
  })

  it('con showRowNumber y página 2, el número respeta el offset de la página', () => {
    const table = useTestTableWithPagination(
      [{ id: '3', nombre: 'Carla', email: 'carla@test.com' }],
      1,
      2,
    )

    render(<DataTable table={table} isLoading={false} isError={false} showRowNumber />)

    expect(screen.getByText('3')).toBeInTheDocument()
  })

  it('con renderActions, agrega una celda por fila con el contenido devuelto', () => {
    const table = useTestTable([{ id: '1', nombre: 'Ana', email: 'ana@test.com' }])

    render(
      <DataTable
        table={table}
        isLoading={false}
        isError={false}
        renderActions={(row) => <button type="button">Editar {row.original.nombre}</button>}
      />,
    )

    expect(screen.getByRole('button', { name: 'Editar Ana' })).toBeInTheDocument()
  })

  it('alterna un fondo sutil en las filas pares (índice 1, 3, ...)', () => {
    const table = useTestTable([
      { id: '1', nombre: 'Ana', email: 'ana@test.com' },
      { id: '2', nombre: 'Beto', email: 'beto@test.com' },
      { id: '3', nombre: 'Caro', email: 'caro@test.com' },
    ])

    render(<DataTable table={table} isLoading={false} isError={false} />)

    const rows = document.querySelectorAll('[data-slot="table-body"] [data-slot="table-row"]')
    expect(rows[0]).not.toHaveClass('bg-muted/40')
    expect(rows[1]).toHaveClass('bg-muted/40')
    expect(rows[2]).not.toHaveClass('bg-muted/40')
  })

  it('con meta.grow en una columna, esa columna recibe un ancho explícito (w-56) y el resto no', () => {
    const growColumns: ColumnDef<Row, unknown>[] = [
      { accessorKey: 'nombre', header: 'Nombre', meta: { grow: true } },
      { accessorKey: 'email', header: 'Correo' },
    ]
    const table = renderHook(function useWrapper() {
      return useReactTable({
        data: [{ id: '1', nombre: 'Ana', email: 'ana@test.com' }],
        columns: growColumns,
        getCoreRowModel: getCoreRowModel(),
      })
    }).result.current

    render(<DataTable table={table} isLoading={false} isError={false} />)

    const growCell = screen.getByText('Ana').closest('[data-slot="table-cell"]')
    expect(growCell).toHaveClass('w-56')
    expect(screen.getByText('ana@test.com').closest('[data-slot="table-cell"]')).not.toHaveClass('w-56')
  })

  it('con meta.width en una columna, usa esa clase en vez de la de meta.grow', () => {
    const widthColumns: ColumnDef<Row, unknown>[] = [
      { accessorKey: 'nombre', header: 'Nombre', meta: { grow: true, width: 'w-24' } },
      { accessorKey: 'email', header: 'Correo' },
    ]
    const table = renderHook(function useWrapper() {
      return useReactTable({
        data: [{ id: '1', nombre: 'Ana', email: 'ana@test.com' }],
        columns: widthColumns,
        getCoreRowModel: getCoreRowModel(),
      })
    }).result.current

    render(<DataTable table={table} isLoading={false} isError={false} />)

    const widthCell = screen.getByText('Ana').closest('[data-slot="table-cell"]')
    expect(widthCell).toHaveClass('w-24')
    expect(widthCell).not.toHaveClass('w-56')
  })
})
