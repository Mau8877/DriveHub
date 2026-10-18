import { AppShell } from '@/components/layout'

import { adminNavItems } from './adminNavItems'
import type { AdminLayoutProps } from './adminTypes'

export function AdminLayout({
  title,
  user,
  notifications,
  activeHref,
  children,
}: AdminLayoutProps) {
  return (
    <AppShell
      appName="App Admin"
      title={title}
      navItems={adminNavItems}
      activeHref={activeHref}
      user={user}
      notifications={notifications}
    >
      {children}
    </AppShell>
  )
}
