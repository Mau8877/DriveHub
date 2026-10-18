import type { ReactNode } from 'react'

import { AppHeader } from './AppHeader'
import { AppSidebar } from './AppSidebar'
import type { LayoutUser, NavItem, NotificationsController } from './types'

interface AppShellProps {
  appName: string
  title: string
  navItems: NavItem[]
  activeHref?: string
  user: LayoutUser
  /** Ver AppHeaderProps. */
  notifications?: NotificationsController
  children: ReactNode
}

// * Esqueleto compartido por todos los layouts de rol (Admin, Usuario, y los
// * que se agreguen a futuro) -- no conoce roles, solo recibe navItems/user
// * por props. Cada layout de rol (src/layouts/) es el único que decide QUÉ
// * le pasa acá, nunca se bifurca por rol dentro de este componente.
export function AppShell({
  appName,
  title,
  navItems,
  activeHref,
  user,
  notifications,
  children,
}: AppShellProps) {
  return (
    <div className="flex h-screen flex-col overflow-hidden bg-background">
      <AppHeader appName={appName} user={user} notifications={notifications} />

      {/* * p-4 acá es lo que crea el espacio real entre el sidebar y el
          header -- el sidebar es una tarjeta flotante (bordes redondeados
          propios), no una columna pegada de borde a borde. */}
      <div className="flex min-h-0 flex-1 gap-4 overflow-hidden p-4">
        <AppSidebar items={navItems} activeHref={activeHref} />
        <main className="min-w-0 flex-1 overflow-y-auto rounded-2xl border border-border bg-card p-6 shadow-sm">
          <h1 className="font-heading text-2xl font-semibold text-foreground">
            {title}
          </h1>
          <div className="mt-4">{children}</div>
        </main>
      </div>
    </div>
  )
}
