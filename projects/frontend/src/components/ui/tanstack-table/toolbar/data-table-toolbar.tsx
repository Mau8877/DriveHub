import { SearchIcon, XIcon } from 'lucide-react'
import type { ReactNode } from 'react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { cn } from '@/lib/utils'

const FILTER_ALL_VALUE = '__all__'

export interface DataTableToolbarFilter {
  /** Nombre del campo -- típicamente el mismo que va en `filter[campo]` del backend (ver .claude/rules/backend/ENDPOINTS_BACKEND.md). Solo se usa como `key` de React, no viaja a ningún lado por sí mismo. */
  id: string
  label: string
  options: { label: string; value: string }[]
  /** `undefined` = "sin filtrar" (opción "Todos"). */
  value: string | undefined
  onValueChange: (value: string | undefined) => void
  placeholder?: string
}

export interface DataTableToolbarProps {
  /** Omitir `searchValue`/`onSearchChange` oculta el buscador -- no todo listado necesita uno. */
  searchValue?: string
  onSearchChange?: (value: string) => void
  searchPlaceholder?: string
  /** Un `Select` por filtro, en el orden dado. Array vacío u omitido = sin filtros. */
  filters?: DataTableToolbarFilter[]
  /** Slot libre para botones de la feature (crear, exportar, refrescar, eliminar seleccionados, etc.) -- el toolbar no sabe qué acciones existen, solo las alinea a la derecha. */
  actions?: ReactNode
  className?: string
}

/**
 * Shell de layout puro para la barra de arriba de un listado: buscador +
 * filtros por `Select` a la izquierda, acciones de la feature a la
 * derecha. No sabe nada de negocio -- ni qué campos son buscables/
 * filtrables, ni qué hacen los botones de `actions`; todo eso lo decide
 * quien lo usa, igual que `DataCards` no sabe qué es cada columna más
 * allá de su `meta`.
 *
 * Deliberadamente sin debounce interno en el buscador -- si una feature
 * quiere debounce, envuelve su propio `onSearchChange` con
 * `src/utils/debounce.ts` antes de pasarlo acá. El toolbar solo refleja
 * `searchValue` tal cual se lo dan.
 *
 * Incluye un botón "Limpiar filtros" que aparece solo cuando hay búsqueda
 * y/o algún filtro con valor -- limpia todo de una vez (llama
 * `onSearchChange('')` y `onValueChange(undefined)` de cada filtro), sin
 * que cada feature tenga que armar ese botón a mano.
 */
export function DataTableToolbar({
  searchValue,
  onSearchChange,
  searchPlaceholder = 'Buscar...',
  filters = [],
  actions,
  className,
}: DataTableToolbarProps) {
  const hasActiveFilters =
    Boolean(searchValue?.trim()) || filters.some((filter) => filter.value !== undefined)

  const clearFilters = () => {
    onSearchChange?.('')
    filters.forEach((filter) => filter.onValueChange(undefined))
  }

  return (
    <div className={cn('flex flex-wrap items-end justify-between gap-3', className)}>
      <div className="flex flex-wrap items-end gap-3">
        {onSearchChange && (
          <div className="relative">
            <SearchIcon className="pointer-events-none absolute top-1/2 left-2.5 size-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              type="search"
              value={searchValue ?? ''}
              onChange={(event) => onSearchChange(event.target.value)}
              placeholder={searchPlaceholder}
              className="w-56 pl-8"
            />
          </div>
        )}
        {filters.map((filter) => (
          <div key={filter.id} className="flex flex-col gap-1">
            <span className="text-xs font-medium text-muted-foreground">{filter.label}</span>
            <Select
              value={filter.value ?? FILTER_ALL_VALUE}
              onValueChange={(value) =>
                filter.onValueChange(value == null || value === FILTER_ALL_VALUE ? undefined : value)
              }
            >
              <SelectTrigger size="sm" aria-label={filter.label}>
                <SelectValue placeholder={filter.placeholder ?? filter.label}>
                  {(value: string | null) =>
                    !value || value === FILTER_ALL_VALUE
                      ? 'Todos'
                      : (filter.options.find((option) => option.value === value)?.label ?? value)
                  }
                </SelectValue>
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={FILTER_ALL_VALUE}>Todos</SelectItem>
                {filter.options.map((option) => (
                  <SelectItem key={option.value} value={option.value}>
                    {option.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        ))}
        {hasActiveFilters && (
          <Button variant="ghost" size="sm" onClick={clearFilters}>
            <XIcon data-icon="inline-start" />
            Limpiar filtros
          </Button>
        )}
      </div>

      {actions && <div className="flex flex-wrap items-center gap-2">{actions}</div>}
    </div>
  )
}
