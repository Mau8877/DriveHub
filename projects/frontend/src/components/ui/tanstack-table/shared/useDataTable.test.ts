import { describe, it, expect, vi } from 'vitest'
import { renderHook } from '@testing-library/react'
import type { ColumnDef } from '@tanstack/react-table'
import { useDataTable } from './useDataTable'
import type { DataTablePageMeta } from './types'

interface Row {
  id: string
  nombre: string
}

const columns: ColumnDef<Row, unknown>[] = [
  { accessorKey: 'nombre', header: 'Nombre' },
]

const data: Row[] = [{ id: '1', nombre: 'Ana' }]

const meta: DataTablePageMeta = { page: 2, pageSize: 20, totalItems: 45, totalPages: 3 }

describe('useDataTable', () => {
  it('refleja meta.totalPages como pageCount', () => {
    const { result } = renderHook(() =>
      useDataTable({
        data,
        columns,
        meta,
        page: 2,
        pageSize: 20,
        onPageChange: vi.fn(),
        onPageSizeChange: vi.fn(),
      }),
    )

    expect(result.current.pageCount).toBe(3)
    expect(result.current.canPreviousPage).toBe(true)
    expect(result.current.canNextPage).toBe(true)
  })

  it('sin meta, pageCount es -1 y canNextPage es false', () => {
    const { result } = renderHook(() =>
      useDataTable({
        data,
        columns,
        meta: undefined,
        page: 1,
        pageSize: 20,
        onPageChange: vi.fn(),
        onPageSizeChange: vi.fn(),
      }),
    )

    expect(result.current.pageCount).toBe(-1)
    expect(result.current.canNextPage).toBe(false)
  })

  it('al cambiar pageIndex dispara onPageChange en 1-based, no onPageSizeChange', () => {
    const onPageChange = vi.fn()
    const onPageSizeChange = vi.fn()

    const { result } = renderHook(() =>
      useDataTable({
        data,
        columns,
        meta,
        page: 2,
        pageSize: 20,
        onPageChange,
        onPageSizeChange,
      }),
    )

    result.current.table.setPageIndex(2)

    expect(onPageChange).toHaveBeenCalledWith(3)
    expect(onPageSizeChange).not.toHaveBeenCalled()
  })

  it('al cambiar pageSize dispara onPageSizeChange, no onPageChange', () => {
    const onPageChange = vi.fn()
    const onPageSizeChange = vi.fn()

    const { result } = renderHook(() =>
      useDataTable({
        data,
        columns,
        meta,
        page: 2,
        pageSize: 20,
        onPageChange,
        onPageSizeChange,
      }),
    )

    result.current.table.setPageSize(50)

    expect(onPageSizeChange).toHaveBeenCalledWith(50)
    expect(onPageChange).not.toHaveBeenCalled()
  })

  it('con enableSorting en false (default), no dispara onSortingChange', () => {
    const onSortingChange = vi.fn()

    const { result } = renderHook(() =>
      useDataTable({
        data,
        columns,
        meta,
        page: 1,
        pageSize: 20,
        onPageChange: vi.fn(),
        onPageSizeChange: vi.fn(),
        onSortingChange,
      }),
    )

    result.current.table.getColumn('nombre')?.toggleSorting()

    expect(onSortingChange).not.toHaveBeenCalled()
  })

  it('con enableSorting en true, togglear un header dispara onSortingChange', () => {
    const onSortingChange = vi.fn()

    const { result } = renderHook(() =>
      useDataTable({
        data,
        columns,
        meta,
        page: 1,
        pageSize: 20,
        onPageChange: vi.fn(),
        onPageSizeChange: vi.fn(),
        enableSorting: true,
        sorting: [],
        onSortingChange,
      }),
    )

    result.current.table.getColumn('nombre')?.toggleSorting()

    expect(onSortingChange).toHaveBeenCalledWith([{ id: 'nombre', desc: false }])
  })
})
