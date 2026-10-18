import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { renderHook } from '@testing-library/react'
import { useReactTable, getCoreRowModel } from '@tanstack/react-table'
import type { ColumnDef } from '@tanstack/react-table'
import { DataCards } from './data-cards'
import '../shared/types'

interface Row {
  id: string
  nombre: string
  email: string
  rol: string
}

const rows: Row[] = [{ id: '1', nombre: 'Ana', email: 'ana@test.com', rol: 'ADMIN' }]

function useTestTable(columns: ColumnDef<Row, unknown>[], data: Row[] = rows) {
  return renderHook(function useWrapper() {
    return useReactTable({
      data,
      columns,
      getCoreRowModel: getCoreRowModel(),
    })
  }).result.current
}

describe('DataCards', () => {
  it('usa la columna marcada con meta.cardTitle como CardTitle, sin duplicarla en el body', () => {
    const columns: ColumnDef<Row, unknown>[] = [
      { accessorKey: 'nombre', header: 'Nombre', meta: { cardTitle: true } },
      { accessorKey: 'email', header: 'Correo' },
    ]
    const table = useTestTable(columns)

    render(<DataCards table={table} isLoading={false} isError={false} />)

    const title = screen.getByText('Ana')
    expect(title.closest('[data-slot="card-title"]')).not.toBeNull()
    expect(screen.getAllByText('Ana')).toHaveLength(1)
  })

  it('respeta hideInCard: esa columna no aparece en la card', () => {
    const columns: ColumnDef<Row, unknown>[] = [
      { accessorKey: 'nombre', header: 'Nombre', meta: { cardTitle: true } },
      { accessorKey: 'email', header: 'Correo' },
      { accessorKey: 'rol', header: 'Rol', meta: { hideInCard: true } },
    ]
    const table = useTestTable(columns)

    render(<DataCards table={table} isLoading={false} isError={false} />)

    expect(screen.queryByText('ADMIN')).not.toBeInTheDocument()
  })

  it('respeta cardLabel como override del label mostrado', () => {
    const columns: ColumnDef<Row, unknown>[] = [
      { accessorKey: 'nombre', header: 'Nombre', meta: { cardTitle: true } },
      { accessorKey: 'email', header: 'Correo', meta: { cardLabel: 'Correo electrónico' } },
    ]
    const table = useTestTable(columns)

    render(<DataCards table={table} isLoading={false} isError={false} />)

    expect(screen.getByText('Correo electrónico')).toBeInTheDocument()
  })

  it('sin ninguna columna cardTitle, usa la primera columna visible como fallback', () => {
    const columns: ColumnDef<Row, unknown>[] = [
      { accessorKey: 'nombre', header: 'Nombre' },
      { accessorKey: 'email', header: 'Correo' },
    ]
    const table = useTestTable(columns)

    render(<DataCards table={table} isLoading={false} isError={false} />)

    expect(screen.getByText('Ana').closest('[data-slot="card-title"]')).not.toBeNull()
  })

  it('con renderCard, ignora la derivación automática', () => {
    const columns: ColumnDef<Row, unknown>[] = [
      { accessorKey: 'nombre', header: 'Nombre', meta: { cardTitle: true } },
    ]
    const table = useTestTable(columns)

    render(
      <DataCards
        table={table}
        isLoading={false}
        isError={false}
        renderCard={(row) => <div>Custom: {row.original.nombre}</div>}
      />,
    )

    expect(screen.getByText('Custom: Ana')).toBeInTheDocument()
  })

  it('en loading, renderiza skeletons en vez de cards', () => {
    const columns: ColumnDef<Row, unknown>[] = [
      { accessorKey: 'nombre', header: 'Nombre', meta: { cardTitle: true } },
    ]
    const table = useTestTable(columns)

    render(<DataCards table={table} isLoading isError={false} skeletonCount={2} />)

    expect(screen.queryByText('Ana')).not.toBeInTheDocument()
    expect(document.querySelectorAll('[data-slot="skeleton"]').length).toBeGreaterThan(0)
  })

  it('en error, muestra el mensaje y no las cards', () => {
    const columns: ColumnDef<Row, unknown>[] = [
      { accessorKey: 'nombre', header: 'Nombre', meta: { cardTitle: true } },
    ]
    const table = useTestTable(columns)

    render(<DataCards table={table} isLoading={false} isError errorMessage="Falló la carga" />)

    expect(screen.getByText('Falló la carga')).toBeInTheDocument()
    expect(screen.queryByText('Ana')).not.toBeInTheDocument()
  })

  it('sin filas, muestra el mensaje de vacío', () => {
    const columns: ColumnDef<Row, unknown>[] = [
      { accessorKey: 'nombre', header: 'Nombre', meta: { cardTitle: true } },
    ]
    const table = useTestTable(columns, [])

    render(<DataCards table={table} isLoading={false} isError={false} />)

    expect(screen.getByText('Sin resultados')).toBeInTheDocument()
  })

  it('con showRowNumber, muestra el número 1-based como badge en la card', () => {
    const columns: ColumnDef<Row, unknown>[] = [
      { accessorKey: 'nombre', header: 'Nombre', meta: { cardTitle: true } },
    ]
    const table = useTestTable(columns)

    render(<DataCards table={table} isLoading={false} isError={false} showRowNumber />)

    expect(screen.getByText('#1')).toBeInTheDocument()
  })

  it('con renderActions, agrega el contenido devuelto en el pie de la card', () => {
    const columns: ColumnDef<Row, unknown>[] = [
      { accessorKey: 'nombre', header: 'Nombre', meta: { cardTitle: true } },
    ]
    const table = useTestTable(columns)

    render(
      <DataCards
        table={table}
        isLoading={false}
        isError={false}
        renderActions={(row) => <button type="button">Editar {row.original.nombre}</button>}
      />,
    )

    expect(screen.getByRole('button', { name: 'Editar Ana' })).toBeInTheDocument()
  })

  it('con renderCard, ignora showRowNumber y renderActions', () => {
    const columns: ColumnDef<Row, unknown>[] = [
      { accessorKey: 'nombre', header: 'Nombre', meta: { cardTitle: true } },
    ]
    const table = useTestTable(columns)

    render(
      <DataCards
        table={table}
        isLoading={false}
        isError={false}
        showRowNumber
        renderActions={() => <button type="button">Editar</button>}
        renderCard={(row) => <div>Custom: {row.original.nombre}</div>}
      />,
    )

    expect(screen.queryByText('#1')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Editar' })).not.toBeInTheDocument()
  })

  it('con meta.cardImage, renderiza la imagen grande como primer hijo de la card y no duplica el avatar en el título', () => {
    const columns: ColumnDef<Row, unknown>[] = [
      {
        accessorKey: 'nombre',
        header: 'Nombre',
        meta: { cardTitle: true, cardImage: (row) => `https://img.test/${row.id}.png` },
        cell: (ctx) => (
          <div>
            <img src="avatar.png" alt="" />
            <span>{ctx.getValue<string>()}</span>
          </div>
        ),
      },
      { accessorKey: 'email', header: 'Correo' },
    ]
    const table = useTestTable(columns)

    render(<DataCards table={table} isLoading={false} isError={false} />)

    const card = screen.getByText('ana@test.com').closest('[data-slot="card"]')
    const bigImage = card?.querySelector('img')
    expect(bigImage).toHaveAttribute('src', 'https://img.test/1.png')
    expect(card?.querySelectorAll('img')).toHaveLength(1)
    expect(screen.getAllByText('Ana')).toHaveLength(1)
  })

  it('respeta cardImageAlt como override del alt de la imagen grande', () => {
    const columns: ColumnDef<Row, unknown>[] = [
      {
        accessorKey: 'nombre',
        header: 'Nombre',
        meta: {
          cardTitle: true,
          cardImage: (row) => `https://img.test/${row.id}.png`,
          cardImageAlt: (row) => `Foto de ${row.nombre}`,
        },
      },
    ]
    const table = useTestTable(columns)

    render(<DataCards table={table} isLoading={false} isError={false} />)

    expect(screen.getByAltText('Foto de Ana')).toBeInTheDocument()
  })
})
