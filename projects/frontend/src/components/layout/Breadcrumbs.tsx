import { Fragment } from 'react'
import { useMatches } from '@tanstack/react-router'
import { ChevronRightIcon } from 'lucide-react'

import { cn } from '@/lib/utils'

// Cada route file opta-in a aparecer acá declarando
// `staticData: { breadcrumb: 'Label' }` en las opciones de
// `createFileRoute` -- sin esto, ese nivel de la ruta no aporta ningún
// cruce (ej. los layouts pathless como `_authenticated` no tienen texto
// propio que mostrar, y no lo necesitan). Se declara acá, en el único
// componente que la usa, mismo criterio que la module augmentation de
// `ColumnMeta` en components/ui/tanstack-table/shared/types.ts.
declare module '@tanstack/react-router' {
  interface StaticDataRouteOption {
    breadcrumb?: string
  }
}

interface BreadcrumbsProps {
  className?: string
}

// * <a> en vez del <Link> tipado de TanStack Router -- mismo motivo que
// * AppSidebar.tsx: acá el destino sale de `match.pathname` (un string
// * genérico resuelto en runtime), no de un literal de ruta conocido en
// * tiempo de compilación que el tipado de `Link to=` pueda validar.
export function Breadcrumbs({ className }: BreadcrumbsProps) {
  const matches = useMatches()
  const crumbs = matches
    .filter((match) => !!match.staticData.breadcrumb)
    .map((match) => ({ pathname: match.pathname, label: match.staticData.breadcrumb as string }))

  if (crumbs.length === 0) return null

  return (
    <nav aria-label="Ruta actual" className={cn('flex min-w-0 items-center gap-1.5 text-sm', className)}>
      {crumbs.map((crumb, index) => {
        const isLast = index === crumbs.length - 1
        return (
          <Fragment key={crumb.pathname}>
            {index > 0 && (
              <ChevronRightIcon className="size-3.5 shrink-0 text-header-foreground/40" />
            )}
            {isLast ? (
              <span className="truncate font-medium text-header-foreground">{crumb.label}</span>
            ) : (
              <a
                href={crumb.pathname}
                className="shrink-0 text-header-foreground/70 transition-colors hover:text-header-foreground hover:underline"
              >
                {crumb.label}
              </a>
            )}
          </Fragment>
        )
      })}
    </nav>
  )
}
