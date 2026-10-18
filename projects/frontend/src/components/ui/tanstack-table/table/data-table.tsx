import { flexRender } from '@tanstack/react-table'
import type { Row, Table as TanstackTable } from '@tanstack/react-table'
import { ArrowDownIcon, ArrowUpIcon, ChevronsUpDownIcon } from 'lucide-react'
import type { ReactNode } from 'react'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { Skeleton } from '@/components/ui/skeleton'
import { cn } from '@/lib/utils'
import type { DataStateProps } from '../shared'

export interface DataTableProps<TData> extends DataStateProps {
  table: TanstackTable<TData>
  /** Filas placeholder mientras `isLoading` (default 5). */
  skeletonRowCount?: number
  emptyMessage?: string
  /** Antepone una columna "#" con el número de fila (1-based, respeta la página actual vía `table.getState().pagination`). */
  showRowNumber?: boolean
  /**
   * Agrega una columna final con lo que devuelva esta función por fila
   * (típicamente botones de editar/eliminar/ver). El paquete no sabe qué
   * acciones existen -- solo reserva y alinea la columna.
   */
  renderActions?: (row: Row<TData>) => ReactNode
  /** Header de la columna de acciones (default "Acciones"). Sin efecto si no se pasa `renderActions`. */
  actionsLabel?: string
  /** className del contenedor con borde -- el scroll horizontal lo maneja el `<Table>` de shadcn internamente, nunca el body. */
  className?: string
}

/**
 * `table-layout: fixed` (en vez del `auto` default) -- con `auto`, cuando
 * la tabla se fuerza a `width: 100%` (como hace `<Table>` de shadcn) y las
 * columnas no llenan ese ancho, el sobrante se lo queda una sola columna
 * casi entero (la única sin ninguna pista de ancho), dejando un hueco
 * vacío enorme después de su contenido en vez de verse proporcional. Con
 * `fixed` + un ancho explícito en la fila de headers (la única fila que
 * cuenta para fixed layout), el sobrante se reparte proporcionalmente
 * entre TODAS las columnas según el ancho que cada una declaró -- ninguna
 * absorbe el 100% del espacio libre ella sola.
 *
 * `meta.width` (una clase `w-*` puntual por columna) gana sobre `grow` --
 * así una feature puede fijar el ancho real de cada columna en vez de
 * depender de que el navegador reparta en partes iguales lo que sobra
 * entre las columnas sin pista de ancho.
 */
function columnWidthClassName(meta: { grow?: boolean; width?: string } | undefined): string {
  if (meta?.width) return meta.width
  return meta?.grow ? 'w-56' : ''
}

export function DataTable<TData>({
  table,
  isLoading,
  isError,
  errorMessage = 'Ocurrió un error al cargar los datos',
  skeletonRowCount = 5,
  emptyMessage = 'Sin resultados',
  showRowNumber = false,
  renderActions,
  actionsLabel = 'Acciones',
  className,
}: DataTableProps<TData>) {
  const columnCount = table.getAllLeafColumns().length + (showRowNumber ? 1 : 0) + (renderActions ? 1 : 0)
  const rows = table.getRowModel().rows
  const { pageIndex, pageSize } = table.getState().pagination

  return (
    <div className={cn('w-full overflow-hidden rounded-md border', className)}>
      <Table className="table-fixed">
        <TableHeader>
          {table.getHeaderGroups().map((headerGroup) => (
            <TableRow key={headerGroup.id} className="bg-accent hover:bg-accent">
              {showRowNumber && (
                <TableHead className="w-12 px-3 text-accent-foreground">#</TableHead>
              )}
              {headerGroup.headers.map((header) => {
                const canSort = header.column.getCanSort()
                const sortDirection = header.column.getIsSorted()
                const meta = header.column.columnDef.meta

                return (
                  <TableHead
                    key={header.id}
                    className={cn('px-3 text-accent-foreground', columnWidthClassName(meta))}
                    style={{ textAlign: meta?.align }}
                  >
                    {header.isPlaceholder ? null : canSort ? (
                      <button
                        type="button"
                        className="inline-flex items-center gap-1 hover:text-accent-foreground/70"
                        onClick={header.column.getToggleSortingHandler()}
                      >
                        {flexRender(header.column.columnDef.header, header.getContext())}
                        {sortDirection === 'asc' ? (
                          <ArrowUpIcon className="size-3.5" />
                        ) : sortDirection === 'desc' ? (
                          <ArrowDownIcon className="size-3.5" />
                        ) : (
                          <ChevronsUpDownIcon className="size-3.5 text-accent-foreground/50" />
                        )}
                      </button>
                    ) : (
                      flexRender(header.column.columnDef.header, header.getContext())
                    )}
                  </TableHead>
                )
              })}
              {renderActions && (
                <TableHead className="w-28 px-3 text-right text-accent-foreground">
                  {actionsLabel}
                </TableHead>
              )}
            </TableRow>
          ))}
        </TableHeader>
        <TableBody>
          {isLoading ? (
            Array.from({ length: skeletonRowCount }).map((_, rowIndex) => (
              <TableRow key={`skeleton-${rowIndex}`}>
                {Array.from({ length: columnCount }).map((_, cellIndex) => (
                  <TableCell key={cellIndex} className="px-3">
                    <Skeleton className="h-4 w-full" />
                  </TableCell>
                ))}
              </TableRow>
            ))
          ) : isError ? (
            <TableRow>
              <TableCell colSpan={columnCount} className="h-24 px-3 text-center text-destructive">
                {errorMessage}
              </TableCell>
            </TableRow>
          ) : rows.length === 0 ? (
            <TableRow>
              <TableCell colSpan={columnCount} className="h-24 px-3 text-center text-muted-foreground">
                {emptyMessage}
              </TableCell>
            </TableRow>
          ) : (
            rows.map((row, rowIndexInPage) => (
              <TableRow key={row.id} className={rowIndexInPage % 2 === 1 ? 'bg-muted/40' : undefined}>
                {showRowNumber && (
                  <TableCell className="w-12 px-3 text-muted-foreground">
                    {pageIndex * pageSize + rowIndexInPage + 1}
                  </TableCell>
                )}
                {row.getVisibleCells().map((cell) => {
                  const meta = cell.column.columnDef.meta
                  return (
                    <TableCell
                      key={cell.id}
                      className={cn('truncate px-3', columnWidthClassName(meta))}
                      style={{ textAlign: meta?.align }}
                    >
                      {flexRender(cell.column.columnDef.cell, cell.getContext())}
                    </TableCell>
                  )
                })}
                {renderActions && (
                  <TableCell className="w-28 px-3 text-right">
                    <div className="flex items-center justify-end gap-1">{renderActions(row)}</div>
                  </TableCell>
                )}
              </TableRow>
            ))
          )}
        </TableBody>
      </Table>
    </div>
  )
}
