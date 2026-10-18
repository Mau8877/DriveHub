import { AppShell } from '@/components/layout'

import { usuarioNavItems } from './usuarioNavItems'
import type { UsuarioLayoutProps } from './usuarioTypes'

export function UsuarioLayout({
  title,
  user,
  notifications,
  activeHref,
  children,
}: UsuarioLayoutProps) {
  return (
    <AppShell
      appName="App"
      title={title}
      navItems={usuarioNavItems}
      activeHref={activeHref}
      user={user}
      notifications={notifications}
    >
      {children}
    </AppShell>
  )
}
