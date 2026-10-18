import type { SortingState } from '@tanstack/react-table'

/**
 * Traduce el `SortingState` de `useDataTable` al formato exacto del query
 * param `sort` que espera el backend (ver
 * .claude/rules/backend/ENDPOINTS_BACKEND.md#formato-de-sort):
 * `campo` ascendente, `-campo` descendente. Solo un campo -- si llegara
 * más de una entrada (no debería, `useDataTable` fuerza
 * `enableMultiSort: false`), se usa la primera y se ignora el resto.
 *
 * `undefined` cuando no hay orden aplicado -- la feature entonces omite el
 * query param en vez de mandar `sort=` vacío.
 */
export function toSortQueryParam(sorting: SortingState): string | undefined {
  const [first] = sorting
  if (!first) return undefined
  return first.desc ? `-${first.id}` : first.id
}
