import { useEffect } from 'react'
import { useLocation } from '@tanstack/react-router'
import { XIcon } from 'lucide-react'

import { cn } from '@/lib/utils'
import { useUiStore } from '@/store'

import type { NavItem } from './types'

interface SidebarNavProps {
  items: NavItem[]
  activeHref?: string
  collapsed?: boolean
  onNavigate?: () => void
}

// * Se usa <a> en vez del <Link> tipado de TanStack Router porque los
// * navItems de cada rol (src/layouts/) hoy apuntan a rutas que todavía no
// * existen (ej. /usuarios, /pedidos) -- son placeholders de la estructura del
// * menú, no rutas reales todavía. Cuando esas rutas se implementen, este
// * componente pasa a usar <Link to={item.href}> para navegación tipada.
function SidebarNav({
  items,
  activeHref,
  collapsed,
  onNavigate,
}: SidebarNavProps) {
  const location = useLocation()
  const current = activeHref ?? location.pathname

  return (
    <nav className="flex flex-col gap-1 px-3 py-4">
      {items.map((item) => {
        const Icon = item.icon
        const isActive = current === item.href
        return (
          <a
            key={item.href}
            href={item.href}
            onClick={onNavigate}
            className={cn(
              'relative flex items-center gap-3 rounded-lg px-3 py-2 text-sm text-sidebar-foreground/80 transition-colors hover:bg-sidebar-primary/5 hover:text-sidebar-foreground',
              isActive &&
                'bg-sidebar-primary/10 font-semibold text-sidebar-primary hover:bg-sidebar-primary/10 hover:text-sidebar-primary'
            )}
          >
            {isActive && (
              <span className="absolute inset-y-1 left-0 w-1 rounded-full bg-accent" />
            )}
            <Icon className="size-[18px] shrink-0" />
            <span className={cn(collapsed && 'md:hidden')}>
              {item.label}
            </span>
          </a>
        )
      })}
    </nav>
  )
}

interface AppSidebarProps {
  items: NavItem[]
  activeHref?: string
}

// * El sidebar tiene dos estados independientes en useUiStore:
// * `sidebarCollapsed` (desktop: ancho completo vs. solo íconos) y
// * `mobileSidebarOpen` (mobile: el drawer abierto o cerrado). Son
// * conceptos distintos -- confundirlos haría que el sidebar apareciera
// * abierto por default en mobile solo porque en desktop no está colapsado.
//
// * El drawer mobile es propio (no el Sheet de shadcn) para tener control
// * total de la marca visual (sin la sombra/radios/botón de cierre por
// * default de ese primitive) y que se vea igual al aside de desktop.
export function AppSidebar({ items, activeHref }: AppSidebarProps) {
  const sidebarCollapsed = useUiStore((s) => s.sidebarCollapsed)
  const mobileSidebarOpen = useUiStore((s) => s.mobileSidebarOpen)
  const setMobileSidebarOpen = useUiStore((s) => s.setMobileSidebarOpen)

  useEffect(() => {
    if (!mobileSidebarOpen) return
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setMobileSidebarOpen(false)
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [mobileSidebarOpen, setMobileSidebarOpen])

  return (
    <>
      <aside
        className={cn(
          'hidden shrink-0 flex-col overflow-hidden rounded-2xl border border-border bg-sidebar shadow-sm transition-[width] duration-200 md:flex',
          sidebarCollapsed ? 'md:w-16' : 'md:w-60'
        )}
      >
        <div className="flex-1 overflow-y-auto">
          <SidebarNav
            items={items}
            activeHref={activeHref}
            collapsed={sidebarCollapsed}
          />
        </div>
        <p
          className={cn(
            'truncate border-t border-sidebar-border px-4 py-3 text-xs text-muted-foreground',
            sidebarCollapsed && 'md:hidden'
          )}
        >
          v0.1.0 · App
        </p>
      </aside>

      <div
        className={cn(
          'fixed inset-0 z-50 bg-black/30 transition-opacity duration-200 md:hidden',
          mobileSidebarOpen
            ? 'pointer-events-auto opacity-100'
            : 'pointer-events-none opacity-0'
        )}
        onClick={() => setMobileSidebarOpen(false)}
        aria-hidden={!mobileSidebarOpen}
      />
      <div
        role="dialog"
        aria-modal="true"
        aria-label="Menú de navegación"
        className={cn(
          'fixed inset-y-0 left-0 z-50 flex w-64 flex-col bg-sidebar transition-transform duration-200 md:hidden',
          mobileSidebarOpen ? 'translate-x-0' : '-translate-x-full'
        )}
      >
        <div className="flex h-14 items-center justify-between border-b border-sidebar-border px-4">
          <span className="font-heading text-sm font-medium text-sidebar-foreground">
            Menú
          </span>
          <button
            type="button"
            onClick={() => setMobileSidebarOpen(false)}
            className="flex size-7 items-center justify-center rounded-full text-muted-foreground hover:bg-muted"
          >
            <XIcon className="size-4" />
            <span className="sr-only">Cerrar menú</span>
          </button>
        </div>
        <div className="flex-1 overflow-y-auto">
          <SidebarNav
            items={items}
            activeHref={activeHref}
            onNavigate={() => setMobileSidebarOpen(false)}
          />
        </div>
        <p className="truncate border-t border-sidebar-border px-4 py-3 text-xs text-muted-foreground">
          v0.1.0 · App
        </p>
      </div>
    </>
  )
}
