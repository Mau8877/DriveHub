export const ELLIPSIS = 'ellipsis' as const

function range(start: number, end: number): number[] {
  return Array.from({ length: end - start + 1 }, (_, i) => start + i)
}

/**
 * Arma la lista de números de página a mostrar en `DataTablePagination`,
 * con "…" donde el rango se corta -- ej. `[1, 2, 3, 'ellipsis', 20]`.
 * Siempre incluye la primera y la última página, más `siblingCount`
 * páginas a cada lado de la actual.
 */
export function getPaginationRange(
  page: number,
  totalPages: number,
  siblingCount: number = 1,
): (number | typeof ELLIPSIS)[] {
  if (totalPages <= 0) return [1]

  const totalPageNumbers = siblingCount * 2 + 5 // primera + última + actual + 2 posibles ellipsis
  if (totalPages <= totalPageNumbers) {
    return range(1, totalPages)
  }

  const leftSiblingIndex = Math.max(page - siblingCount, 1)
  const rightSiblingIndex = Math.min(page + siblingCount, totalPages)

  const showLeftEllipsis = leftSiblingIndex > 2
  const showRightEllipsis = rightSiblingIndex < totalPages - 1

  if (!showLeftEllipsis && showRightEllipsis) {
    const leftItemCount = 3 + siblingCount * 2
    return [...range(1, leftItemCount), ELLIPSIS, totalPages]
  }

  if (showLeftEllipsis && !showRightEllipsis) {
    const rightItemCount = 3 + siblingCount * 2
    return [1, ELLIPSIS, ...range(totalPages - rightItemCount + 1, totalPages)]
  }

  return [1, ELLIPSIS, ...range(leftSiblingIndex, rightSiblingIndex), ELLIPSIS, totalPages]
}
